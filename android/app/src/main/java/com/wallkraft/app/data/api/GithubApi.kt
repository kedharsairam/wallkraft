package com.wallkraft.app.data.api

import com.wallkraft.app.BuildConfig
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.AppUpdateInfo
import com.wallkraft.app.domain.model.compareVersions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

@Serializable
private data class GithubAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("size") val size: Long = 0L,
)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("assets") val assets: List<GithubAsset> = emptyList(),
)

/**
 * GitHub Releases update check. Manual only — caller decides when to call.
 * Public repo, no token. Rate limit 60/hr/IP unauth — fail silent, caller shows nothing.
 */
class GithubApi @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    suspend fun latestRelease(currentVersion: String = BuildConfig.VERSION_NAME): Result<AppUpdateInfo?> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/kedharsairam/wallkraft/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WallKraft/${BuildConfig.VERSION_NAME}")
                    .get()
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.code == 403 || resp.code == 429) {
                        return@withContext Result.Failure(AppError.NetworkError.RateLimited)
                    }
                    if (resp.code == 404) {
                        return@withContext Result.Failure(AppError.DataError.NotFound)
                    }
                    if (resp.code !in 200..299) {
                        return@withContext Result.Failure(AppError.NetworkError.ServerError(resp.code, "GitHub $resp"))
                    }
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) {
                        return@withContext Result.Failure(AppError.DataError.Parse())
                    }
                    val release = try {
                        json.decodeFromString(GithubRelease.serializer(), body)
                    } catch (_: SerializationException) {
                        return@withContext Result.Failure(AppError.DataError.Parse())
                    }
                    val tag = release.tagName.trim()
                    if (tag.isEmpty()) {
                        return@withContext Result.Failure(AppError.DataError.Parse())
                    }
                    if (compareVersions(tag, currentVersion) <= 0) {
                        return@withContext Result.Success(null)
                    }
                    val apk = release.assets.firstOrNull {
                        it.name.lowercase().endsWith(".apk") &&
                            it.browserDownloadUrl.startsWith("https://github.com/")
                    } ?: return@withContext Result.Success(null)
                    Result.Success(
                        AppUpdateInfo(
                            tag = tag,
                            version = tag.removePrefix("v").removePrefix("V"),
                            apkUrl = apk.browserDownloadUrl,
                            sizeBytes = apk.size,
                            notes = release.body,
                        ),
                    )
                }
            } catch (_: SocketTimeoutException) {
                Result.Failure(AppError.NetworkError.Timeout)
            } catch (_: IOException) {
                Result.Failure(AppError.NetworkError.NoConnection)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Failure(AppError.Unknown(message = e.message))
            }
        }
}
