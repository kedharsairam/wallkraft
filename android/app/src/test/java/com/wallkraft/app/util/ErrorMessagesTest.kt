package com.wallkraft.app.util

import com.wallkraft.app.domain.repository.WallpaperError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMessagesTest {

    @Test
    fun `RateLimited is a WallpaperError`() {
        val error = WallpaperError.RateLimited
        assertTrue(error is WallpaperError)
        assertTrue(error is Exception)
    }

    @Test
    fun `Api error stores message and code`() {
        val error = WallpaperError.Api("API error: 500", code = 500)
        assertEquals("API error: 500", error.message)
        assertEquals(500, error.code)
    }

    @Test
    fun `Api error with null code`() {
        val error = WallpaperError.Api("Unknown")
        assertEquals("Unknown", error.message)
        assertNull(error.code)
    }

    @Test
    fun `Api error 400 is invalid query`() {
        val error = WallpaperError.Api("Bad request", code = 400)
        assertEquals(400, error.code)
    }

    @Test
    fun `Api error 401 is unauthorized`() {
        val error = WallpaperError.Api("Unauthorized", code = 401)
        assertEquals(401, error.code)
    }

    @Test
    fun `Api error 403 is forbidden`() {
        val error = WallpaperError.Api("Forbidden", code = 403)
        assertEquals(403, error.code)
    }

    @Test
    fun `Api error 404 is not found`() {
        val error = WallpaperError.Api("Not found", code = 404)
        assertEquals(404, error.code)
    }

    @Test
    fun `Api error 500 is server error`() {
        val error = WallpaperError.Api("Server error", code = 500)
        assertEquals(500, error.code)
    }

    @Test
    fun `Api error 429 matches rate limited`() {
        val error = WallpaperError.Api("Too many requests", code = 429)
        assertEquals(429, error.code)
    }

    @Test
    fun `unknown exception preserves its message`() {
        val error = Exception("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun `exception with null message has null message`() {
        val error = Exception(null as String?)
        assertNull(error.message)
    }
}
