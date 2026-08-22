package com.wallkraft.app.data.api

import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.model.toCategoryParam
import com.wallkraft.app.domain.model.toPurityParam
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

@Serializable
private data class WallpaperEnvelope(
    @SerialName("data") val data: Wallpaper,
)

/**
 * Wallhaven API client with rate-limit tracking.
 *
 * Reads the API key from [SettingsRepository] on every request so key changes
 * (via Settings) take effect immediately without a restart. Throws
 * [WallpaperError.RateLimited] when the limit is reached.
 */
class WallhavenApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val settings: SettingsRepository,
) {
    private val baseUrl = "https://wallhaven.cc/api/v1"

    /** Max automatic retries for transient failures (network / 5xx). */
    private companion object {
        const val MAX_RETRIES = KraftConstants.RetryMax
    }

    private fun checkRateLimit() {
        if (RateLimitState.limited.value) throw WallpaperError.RateLimited
    }

    suspend fun search(filters: WallhavenFilters, page: Int): WallpaperResponse {
        checkRateLimit()
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .apply {
                addQueryParameter("categories", filters.categories.toCategoryParam())
                addQueryParameter("purity", filters.purity.toPurityParam())
                addQueryParameter("sorting", filters.sorting.value)
                addQueryParameter("page", page.toString())
                if (filters.query.isNotBlank()) addQueryParameter("q", filters.query)
                // Orientation maps to the `ratios` param; Both omits it.
                if (filters.orientation != Orientation.Both) {
                    addQueryParameter("ratios", filters.orientation.value)
                }
            }
            .build()
        return execute(url.toString())
    }

    suspend fun wallpaper(id: String): Wallpaper {
        checkRateLimit()
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("w")
            .addPathSegment(id)
            .build()
        return execute<WallpaperEnvelope>(url.toString()).data
    }

    fun observeRateLimited(): Flow<Boolean> = RateLimitState.limited

    private suspend inline fun <reified T> execute(url: String): T =
        withContext(Dispatchers.IO) {
            val apiKey = settings.current().apiKey
            var attempt = 0
            while (true) {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .apply { if (apiKey.isNotBlank()) header("X-API-Key", apiKey) }
                    .get()
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        parseRateLimit(response.header("X-RateLimit-Remaining"))
                        when {
                            response.isSuccessful -> {
                                val body = response.body?.string()
                                    ?: throw WallpaperError.Api("Empty response")
                                return@withContext json.decodeFromString<T>(body)
                            }
                            response.code == 429 -> throw WallpaperError.RateLimited
                            // Transient server error — retry with backoff.
                            response.code in 500..599 && attempt < MAX_RETRIES -> {
                                delay(backoffMillis(attempt))
                                attempt++
                            }
                            else -> throw WallpaperError.Api(
                                "API error: ${response.code}", code = response.code,
                            )
                        }
                    }
                } catch (e: WallpaperError) {
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    // Transient network failure — retry with backoff.
                    if (attempt < MAX_RETRIES) {
                        delay(backoffMillis(attempt))
                        attempt++
                    } else {
                        throw WallpaperError.Api("Network error: ${e.message}")
                    }
                } catch (e: SerializationException) {
                    throw WallpaperError.Api("Invalid API response")
                } catch (e: Exception) {
                    throw WallpaperError.Api("Request failed: ${e.message}")
                }
            }
            // The loop only exits via return/throw — this satisfies the type
            // checker but is never reached.
            @Suppress("KotlinUnreachableCode")
            error("unreachable")
        }

    private fun backoffMillis(attempt: Int): Long = KraftConstants.RetryBackoffBaseMs * (1 shl attempt)

    private fun parseRateLimit(remaining: String?) {
        remaining?.toIntOrNull()?.let { RateLimitState.update(it) }
    }
}
