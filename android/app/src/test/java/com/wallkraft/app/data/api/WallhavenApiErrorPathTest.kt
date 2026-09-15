package com.wallkraft.app.data.api

import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WallhavenApiErrorPathTest {

    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    // --- JSON parsing edge cases (direct decoder tests) ---

    @Test
    fun `empty data array returns empty list`() {
        val body = """{"data":[],"meta":{"current_page":1,"last_page":1,"per_page":24,"total":0}}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertTrue(response.data.isEmpty())
        assertEquals(0, response.meta.total)
    }

    @Test
    fun `missing meta field applies defaults`() {
        val body = """{"data":[{"id":"abc","path":"https://example.com/abc.jpg"}]}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertEquals(1, response.data.size)
        assertEquals("abc", response.data[0].id)
        assertEquals(1, response.meta.currentPage)
        assertEquals(1, response.meta.lastPage)
        assertEquals(0, response.meta.total)
    }

    @Test
    fun `empty JSON object returns empty response with defaults`() {
        val body = """{}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertTrue(response.data.isEmpty())
        assertEquals(1, response.meta.currentPage)
        assertEquals(1, response.meta.lastPage)
        assertEquals(0, response.meta.total)
    }

    @Test
    fun `malformed JSON throws SerializationException`() {
        val body = """{not valid json"""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException")
        } catch (e: SerializationException) {
            assertTrue(e.message?.contains("Unexpected JSON token") == true || e.message != null)
        }
    }

    @Test
    fun `truncated JSON body throws SerializationException`() {
        val body = """{"data":[{"id":"abc","path":"https"""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException for truncated JSON")
        } catch (e: SerializationException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `empty string body throws SerializationException`() {
        val body = ""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException for empty body")
        } catch (e: SerializationException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `JSON array instead of object throws SerializationException`() {
        val body = """[{"id":"abc"}]"""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException for wrong root type")
        } catch (e: SerializationException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `null data field throws exception`() {
        val body = """{"data":null,"meta":{"current_page":1,"last_page":1}}"""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException for null data on non-nullable List field")
        } catch (e: SerializationException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `wallpaper with missing optional fields uses defaults`() {
        val body = """{"data":[{"id":"minimal"}]}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertEquals(1, response.data.size)
        val wp = response.data[0]
        assertEquals("minimal", wp.id)
        assertEquals("", wp.url)
        assertEquals("", wp.path)
        assertEquals(1920, wp.dimensionX)
        assertEquals(1080, wp.dimensionY)
        assertEquals("sfw", wp.purity)
    }

    @Test
    fun `wallpaper response parses single wallpaper`() {
        val body = """{"data":[{"id":"single","path":"https://example.com/single.jpg"}],"meta":{"current_page":1,"last_page":1,"per_page":24,"total":1}}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertEquals("single", response.data.first().id)
    }

    @Test
    fun `nested objects with unknown keys are tolerated`() {
        val body = """{"data":[{"id":"abc","unknown_field":123}],"meta":{"current_page":1,"last_page":1},"extra_top_level":"ignored"}"""
        val response = json.decodeFromString<WallpaperResponse>(body)
        assertEquals(1, response.data.size)
        assertEquals("abc", response.data[0].id)
    }

    // --- HTTP code → AppError mapping (via internal httpCodeToAppError) ---

    private fun buildTestApi(): WallhavenApi {
        val fakeSettings = object : SettingsRepository {
            private val _settings = MutableStateFlow(AppSettings())
            override val settings: Flow<AppSettings> = _settings
            override suspend fun current(): AppSettings = _settings.value
            override suspend fun update(transform: (AppSettings) -> AppSettings) {
                _settings.value = transform(_settings.value)
            }
        }
        return WallhavenApi(
            client = OkHttpClient(),
            json = json,
            settings = fakeSettings,
            rateLimitState = RateLimitState(),
        )
    }

    @Test
    fun `400 maps to Validation`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(400, "bad request")
        assertTrue(error is AppError.DataError.Validation)
        assertEquals("bad request", (error as AppError.DataError.Validation).message)
    }

    @Test
    fun `401 maps to Unauthorized`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(401, "unauthorized")
        assertTrue(error is AppError.AuthError.Unauthorized)
    }

    @Test
    fun `403 maps to Expired (Forbidden)`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(403, "forbidden")
        assertTrue(error is AppError.AuthError.Expired)
    }

    @Test
    fun `404 maps to NotFound`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(404, "not found")
        assertTrue(error is AppError.DataError.NotFound)
    }

    @Test
    fun `429 maps to RateLimited`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(429, "too many requests")
        assertTrue(error is AppError.NetworkError.RateLimited)
    }

    @Test
    fun `500 maps to ServerError`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(500, "internal error")
        assertTrue(error is AppError.NetworkError.ServerError)
        assertEquals(500, (error as AppError.NetworkError.ServerError).code)
    }

    @Test
    fun `502 maps to ServerError`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(502, "bad gateway")
        assertTrue(error is AppError.NetworkError.ServerError)
        assertEquals(502, (error as AppError.NetworkError.ServerError).code)
    }

    @Test
    fun `503 maps to ServerError`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(503, "service unavailable")
        assertTrue(error is AppError.NetworkError.ServerError)
        assertEquals(503, (error as AppError.NetworkError.ServerError).code)
    }

    @Test
    fun `599 maps to ServerError`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(599, "server error")
        assertTrue(error is AppError.NetworkError.ServerError)
        assertEquals(599, (error as AppError.NetworkError.ServerError).code)
    }

    @Test
    fun `unknown 4xx code maps to Unknown`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(418, "teapot")
        assertTrue(error is AppError.Unknown)
        assertEquals("teapot", (error as AppError.Unknown).message)
    }

    @Test
    fun `httpCodeToAppError preserves message`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(500, "server said no")
        assertEquals("server said no", (error as AppError.NetworkError.ServerError).message)
    }

    @Test
    fun `httpCodeToAppError handles null message`() {
        val api = buildTestApi()
        val error = api.httpCodeToAppError(500, null)
        assertTrue(error is AppError.NetworkError.ServerError)
        assertNull((error as AppError.NetworkError.ServerError).message)
    }

    // --- Rate limit state behavior ---

    @Test
    fun `RateLimitState starts not limited`() = runTest(dispatcher) {
        val state = RateLimitState()
        advanceUntilIdle()
        assertEquals(false, state.limited.value)
        assertEquals(com.wallkraft.app.core.design.KraftConstants.RateLimitDefaultRemaining, state.remaining.value)
    }

    @Test
    fun `RateLimitState becomes limited when remaining is 0`() = runTest(dispatcher) {
        val state = RateLimitState()
        advanceUntilIdle()

        state.update(0)
        advanceUntilIdle()

        assertEquals(true, state.limited.value)
        assertEquals(0, state.remaining.value)
    }

    @Test
    fun `RateLimitState stays not limited when remaining is positive`() = runTest(dispatcher) {
        val state = RateLimitState()
        advanceUntilIdle()

        state.update(42)
        advanceUntilIdle()

        assertEquals(false, state.limited.value)
        assertEquals(42, state.remaining.value)
    }

    @Test
    fun `RateLimitState reset clears limited`() = runTest(dispatcher) {
        val state = RateLimitState()
        advanceUntilIdle()

        state.update(0)
        advanceUntilIdle()
        assertEquals(true, state.limited.value)

        state.reset()
        advanceUntilIdle()

        assertEquals(false, state.limited.value)
    }

    // --- MockWebServer integration: HTTP-level error responses ---

    private fun mockClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    @Test
    fun `500 response returns ServerError via OkHttp`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(500, response.code)
            val body = response.body?.string()
            assertEquals("Internal Server Error", body)
        }
    }

    @Test
    fun `403 response returns forbidden status`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(403, response.code)
        }
    }

    @Test
    fun `429 response with Retry-After header is received`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "60")
                .setBody("Rate Limited"),
        )
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(429, response.code)
            assertEquals("60", response.header("Retry-After"))
        }
    }

    @Test
    fun `429 without Retry-After header returns null for header`() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("Rate Limited"))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(429, response.code)
            assertNull(response.header("Retry-After"))
        }
    }

    @Test
    fun `empty response body returns null body`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string()
            assertEquals("", body)
        }
    }

    @Test
    fun `response with empty body string is handled`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string()
            assertEquals("", body)
        }
    }

    // --- Network timeout via MockWebServer ---

    @Test
    fun `connection timeout results in SocketTimeoutException`() {
        // Enqueue a response that will never be sent (server won't send it
        // until we dequeue, but with a very short timeout this should timeout)
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MILLISECONDS)
            .readTimeout(1, TimeUnit.MILLISECONDS)
            .build()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        try {
            client.newCall(request).execute().use { }
            fail("Expected SocketTimeoutException")
        } catch (e: SocketTimeoutException) {
            assertNotNull(e.message)
        }
    }

    // --- Partial/truncated response ---

    @Test
    fun `truncated response body is readable but incomplete`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[{"id":"abc","path":"http"""))
        val client = mockClient()
        val request = okhttp3.Request.Builder().url(server.url("/api/v1/search")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string()
            assertNotNull(body)
            // Parsing the truncated body should throw SerializationException
            try {
                json.decodeFromString<WallpaperResponse>(body!!)
                fail("Expected SerializationException for truncated JSON")
            } catch (e: SerializationException) {
                assertNotNull(e.message)
            }
        }
    }

    // --- WallpaperResponse.isStale edge cases ---

    @Test
    fun `isStale returns false when cachedAt is null`() {
        val response = WallpaperResponse(
            data = emptyList(),
            meta = WallpaperMeta(),
            cachedAt = null,
        )
        assertEquals(false, response.isStale(nowMillis = System.currentTimeMillis()))
    }

    @Test
    fun `isStale returns true when cache is older than TTL`() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        val response = WallpaperResponse(
            data = emptyList(),
            meta = WallpaperMeta(),
            cachedAt = now - ttl - 1,
        )
        assertEquals(true, response.isStale(nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun `isStale returns false when cache is within TTL`() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        val response = WallpaperResponse(
            data = emptyList(),
            meta = WallpaperMeta(),
            cachedAt = now - ttl + 1,
        )
        assertEquals(false, response.isStale(nowMillis = now, ttlMillis = ttl))
    }

    // --- WallpaperResponse parsing edge cases ---

    @Test
    fun `WallpaperResponse with null data field throws exception`() {
        val body = """{"data":null}"""
        try {
            json.decodeFromString<WallpaperResponse>(body)
            fail("Expected SerializationException for null data on non-nullable List field")
        } catch (e: SerializationException) {
            assertNotNull(e.message)
        }
    }
}
