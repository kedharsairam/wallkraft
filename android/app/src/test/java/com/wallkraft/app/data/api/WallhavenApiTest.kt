package com.wallkraft.app.data.api

import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.WallhavenFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WallhavenApiTest {
    private lateinit var rateLimitState: RateLimitState
    private val json = Json { ignoreUnknownKeys = true }

    private class FakeSettings : com.wallkraft.app.domain.repository.SettingsRepository {
        override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings())
        override suspend fun current(): AppSettings = AppSettings()
        override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    }

    @Before
    fun setUp() {
        rateLimitState = RateLimitState()
    }

    private fun apiFor(
        code: Int,
        headers: Map<String, String> = emptyMap(),
        body: String = "{}",
        counter: java.util.concurrent.atomic.AtomicInteger? = null,
    ): WallhavenApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                counter?.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return WallhavenApi(client, json, FakeSettings(), rateLimitState)
    }

    @Test
    fun `httpCodeToAppError mapping holds post-extraction`() {
        val api = apiFor(200)
        assertTrue(api.httpCodeToAppError(400, "bad") is AppError.DataError.Validation)
        assertTrue(api.httpCodeToAppError(401, null) is AppError.AuthError.Unauthorized)
        assertTrue(api.httpCodeToAppError(403, null) is AppError.AuthError.Expired)
        assertTrue(api.httpCodeToAppError(404, null) is AppError.DataError.NotFound)
        assertTrue(api.httpCodeToAppError(429, null) is AppError.NetworkError.RateLimited)
        val server = api.httpCodeToAppError(500, "boom")
        assertTrue(server is AppError.NetworkError.ServerError)
        assertEquals(500, (server as AppError.NetworkError.ServerError).code)
    }

    @Test
    fun `429 surfaces RateLimited with Retry-After`() = runTest {
        val api = apiFor(429, mapOf("Retry-After" to "45"))
        val result = api.search(WallhavenFilters(), page = 1)
        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.NetworkError.RateLimited)
        assertEquals(45L, (error as AppError.NetworkError.RateLimited).retryAfterSec)
    }

    @Test
    fun `execute is single-shot on 500`() = runTest {
        val counter = java.util.concurrent.atomic.AtomicInteger(0)
        val api = apiFor(500, counter = counter)
        val result = api.search(WallhavenFilters(), page = 1)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NetworkError.ServerError)
        assertEquals(1, counter.get())
    }

    @Test
    fun `401 maps to Unauthorized`() = runTest {
        val api = apiFor(401)
        val result = api.search(WallhavenFilters(), page = 1)
        assertTrue((result as Result.Failure).error is AppError.AuthError.Unauthorized)
    }
}
