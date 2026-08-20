package com.wallkraft.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CountFormatTest {

    @Test
    fun `zero formats as zero`() {
        assertEquals("0", formatCount(0))
    }

    @Test
    fun `under a thousand stays a plain number`() {
        assertEquals("41", formatCount(41))
        assertEquals("999", formatCount(999))
    }

    @Test
    fun `exactly a thousand trims the decimal`() {
        assertEquals("1k", formatCount(1000))
    }

    @Test
    fun `thousands are compacted with one decimal`() {
        assertEquals("5.1k", formatCount(5085))
        assertEquals("12.3k", formatCount(12345))
    }

    @Test
    fun `millions are compacted with one decimal`() {
        assertEquals("1.2m", formatCount(1234567))
    }

    @Test
    fun `negative counts stay a plain number`() {
        assertEquals("-5", formatCount(-5))
    }
}
