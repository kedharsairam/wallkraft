package com.wallkraft.app.data.api

import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.model.toCategoryParam
import com.wallkraft.app.domain.model.toPurityParam
import com.wallkraft.app.domain.repository.SettingsRepository
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
import java.net.SocketTimeoutException

@Serializable
private data class WallpaperEnvelope(
    @SerialName("data") val data: Wallpaper,
)

/**
 * Wallhaven API client with rate-limit tracking.
 *
 * Reads the API key from [SettingsRepository] on every request so key changes
 * (via Settings) take effect immediately without a restart. Returns
 * [Result.Failure] with [AppError.NetworkError.RateLimited] when the limit is reached.
 *
 * Hilt: [RateLimitState] is now injected so tests can provide a fake and the
 * singleton is owned by the graph.
 */
class WallhavenApi @javax.inject.Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val settings: SettingsRepository,
    private val rateLimitState: RateLimitState,
) {
    private val baseUrl = "https://wallhaven.cc/api/v1"

    /** Max automatic retries for transient failures (network / 5xx). */
    private companion object {
        const val MAX_RETRIES = KraftConstants.RetryMax
        const val TAG = "WallKraftPerf"
    }

    suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> {
        if (rateLimitState.limited.value) return Result.Failure(AppError.NetworkError.RateLimited)
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .apply {
                addQueryParameter("categories", filters.categories.toCategoryParam())
                addQueryParameter("purity", filters.purity.toPurityParam())
                addQueryParameter("sorting", filters.sorting.value)
                // Toplist requires a time range — the API defaults to 1M, but we
                // always send the user's explicit choice. Other sortings omit it.
                if (filters.sorting == Sorting.Toplist) {
                    addQueryParameter("topRange", filters.topRange.value)
                }
                addQueryParameter("page", page.toString())
                if (filters.query.isNotBlank()) addQueryParameter("q", filters.query)
                // Hex color filter (e.g. "0000ff"); blank omits it.
                if (filters.colors.isNotBlank()) addQueryParameter("colors", filters.colors)
                // Orientation maps to the `ratios` param; Both omits it.
                if (filters.orientation != Orientation.Both) {
                    addQueryParameter("ratios", filters.orientation.value)
                }
            }
            .build()
        return execute(url.toString())
    }

    suspend fun wallpaper(id: String): Result<Wallpaper> {
        if (rateLimitState.limited.value) return Result.Failure(AppError.NetworkError.RateLimited)
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("w")
            .addPathSegment(id)
            .build()
        return when (val result = execute<WallpaperEnvelope>(url.toString())) {
            is Result.Success -> Result.Success(result.data.data)
            is Result.Failure -> result
        }
    }

    fun observeRateLimited(): Flow<Boolean> = rateLimitState.limited

    /**
     * Validates the API key by making a lightweight search request.
     * Uses the X-API-Key header (same auth method as actual search requests).
     * Checks response code: 200 = valid, 401 = invalid.
     */
    suspend fun validateApiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return false
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/search?q=&page=1&limit=${KraftConstants.ValidateLimit}"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("X-API-Key", trimmed)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val rateLimit = response.header("X-RateLimit-Limit")?.toIntOrNull()
                    val rateRemaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()
                    // Consume body to release connection
                    response.body?.string()
                    if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.d("WallhavenApi", "validateApiKey: code=${response.code}, X-RateLimit-Limit=$rateLimit, X-RateLimit-Remaining=$rateRemaining")
                    // 200 = valid key, 401 = invalid key
                    response.code == 200
                }
            } catch (e: Exception) {
                if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.e("WallhavenApi", "validateApiKey error: ${e.message}", e)
                false
            }
        }
    }

    private suspend inline fun <reified T> execute(url: String): Result<T> =
        withContext(Dispatchers.IO) {
            val currentSettings = settings.current()
            val apiKey = currentSettings.apiKey
            // Only send the key when it's been validated. An invalid key
            // (wrong API key test) would otherwise make *every* request —
            // even SFW/Sketchy which don't need a key — fail with 401 and
            // show "Your API key was rejected" on Browse. SFW must work
            // without a valid key.
            val shouldSendKey = apiKey.isNotBlank() && currentSettings.apiKeyValid
            // Debug-only API timing. The key travels in a header, never the
            // URL, so logging the URL leaks nothing sensitive.
            val startMs = android.os.SystemClock.elapsedRealtime()
            var attempt = 0
            while (true) {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .apply { if (shouldSendKey) header("X-API-Key", apiKey) }
                    .get()
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        parseRateLimit(response.header("X-RateLimit-Remaining"))
                        when {
                            response.isSuccessful -> {
                                val body = response.body?.string()
                                    ?: return@withContext Result.Failure(
                                        AppError.DataError.Parse(message = "Empty response")
                                    )
                                val result = try {
                                    json.decodeFromString<T>(body)
                                } catch (e: SerializationException) {
                                    return@withContext Result.Failure(
                                        AppError.DataError.Parse(message = "Invalid API response", cause = e)
                                    )
                                }
                                if (com.wallkraft.app.BuildConfig.DEBUG) {
                                    val took = android.os.SystemClock.elapsedRealtime() - startMs
                                    android.util.Log.d(TAG, "api ${took}ms (attempts=${attempt + 1}) $url")
                                }
                                return@withContext Result.Success(result)
                            }
                            response.code == 429 -> return@withContext Result.Failure(AppError.NetworkError.RateLimited)
                            // Transient server error — retry with backoff.
                            response.code in 500..599 && attempt < MAX_RETRIES -> {
                                delay(backoffMillis(attempt))
                                attempt++
                            }
                            else -> return@withContext Result.Failure(httpCodeToAppError(response.code, "API error: ${response.code}"))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    // Transient network failure — retry with backoff.
                    if (attempt < MAX_RETRIES) {
                        delay(backoffMillis(attempt))
                        attempt++
                    } else {
                        val appError = if (e is SocketTimeoutException) {
                            AppError.NetworkError.Timeout
                        } else {
                            AppError.NetworkError.NoConnection
                        }
                        return@withContext Result.Failure(appError)
                    }
                } catch (e: SerializationException) {
                    return@withContext Result.Failure(AppError.DataError.Parse(message = "Invalid API response", cause = e))
                } catch (e: Exception) {
                    return@withContext Result.Failure(AppError.Unknown(throwable = e, message = e.message))
                }
            }
            // The loop only exits via return/throw — this satisfies the type
            // checker but is never reached.
            @Suppress("KotlinUnreachableCode")
            error("unreachable")
        }

    private fun httpCodeToAppError(code: Int, message: String?): AppError = when (code) {
        400 -> AppError.DataError.Validation(message)
        401 -> AppError.AuthError.Unauthorized
        403 -> AppError.AuthError.Expired
        404 -> AppError.DataError.NotFound
        429 -> AppError.NetworkError.RateLimited
        in 500..599 -> AppError.NetworkError.ServerError(code, message)
        else -> AppError.Unknown(message = message)
    }

    private fun backoffMillis(attempt: Int): Long = KraftConstants.RetryBackoffBaseMs * (1 shl attempt)

    private fun parseRateLimit(remaining: String?) {
        remaining?.toIntOrNull()?.let { rateLimitState.update(it) }
    }
}
