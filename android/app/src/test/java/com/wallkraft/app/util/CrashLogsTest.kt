package com.wallkraft.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CrashLogsTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("crash_log_test").toFile()
    }

    private fun crash(name: String, ageMs: Long, size: Int = 10): File {
        val file = File(dir, name)
        file.writeBytes(ByteArray(size))
        file.setLastModified(System.currentTimeMillis() - ageMs)
        return file
    }

    @Test
    fun emptyDir_returnsNull() {
        assertNull(CrashLogs.latestIn(dir))
    }

    @Test
    fun ignoresNonCrashFiles() {
        File(dir, "notes.txt").writeText("hello")
        assertNull(CrashLogs.latestIn(dir))
    }

    @Test
    fun ignoresEmptyCrashFiles() {
        File(dir, "crash-1.log").createNewFile()
        assertNull(CrashLogs.latestIn(dir))
    }

    @Test
    fun picksNewestCrashLog() {
        crash("crash-1.log", ageMs = 2000)
        val newest = crash("crash-2.log", ageMs = 1000)
        crash("crash-3.log", ageMs = 3000)
        assertEquals(newest, CrashLogs.latestIn(dir))
    }

    @Test
    fun missingDir_returnsNull() {
        assertNull(CrashLogs.latestIn(File(dir, "nope")))
    }
}
