package com.wallkraft.app.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun clientWith(delays: MutableList<Long>, maxAttempts: Int = 3): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                RetryInterceptor(maxAttempts = maxAttempts, sleeper = { delays.add(it) }),
            )
            .retryOnConnectionFailure(false)
            .build()

    private fun get(url: String = "/") = Request.Builder().url(server.url(url)).get().build()

    @Test
    fun `500 then 500 then 200 succeeds with 3 calls`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)

        client.newCall(get()).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(3, server.requestCount)
        assertEquals(2, delays.size)
    }

    @Test
    fun `persistent 500 returns failure after 4 calls`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)

        client.newCall(get()).execute().use { response ->
            assertEquals(500, response.code)
        }
        assertEquals(4, server.requestCount)
        assertEquals(3, delays.size)
    }

    @Test
    fun `429 is never retried`() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("rate limited"))
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)

        client.newCall(get()).execute().use { response ->
            assertEquals(429, response.code)
        }
        assertEquals(1, server.requestCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `POST is never retried`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)
        val post = Request.Builder().url(server.url("/")).post("".toRequestBody()).build()

        client.newCall(post).execute().use { response ->
            assertEquals(500, response.code)
        }
        assertEquals(1, server.requestCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `backoff delays stay within exponential caps`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)

        client.newCall(get()).execute().use { response ->
            assertEquals(500, response.code)
        }
        assertEquals(3, delays.size)
        val caps = listOf(1000L, 2000L, 4000L)
        delays.forEachIndexed { index, delayMs ->
            assertTrue("delay $delayMs exceeds cap ${caps[index]}", delayMs in 0 until caps[index])
        }
    }

    @Test
    fun `4xx is never retried`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val delays = mutableListOf<Long>()
        val client = clientWith(delays)

        client.newCall(get()).execute().use { response ->
            assertEquals(404, response.code)
        }
        assertEquals(1, server.requestCount)
        assertTrue(delays.isEmpty())
    }
}
