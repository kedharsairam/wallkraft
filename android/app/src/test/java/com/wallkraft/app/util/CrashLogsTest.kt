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

    @Test
    fun pruneOld_deletesOnlyExpiredLogs() {
        val dayMs = 24 * 60 * 60 * 1000L
        crash("crash-old.log", ageMs = 15 * dayMs)
        val fresh = crash("crash-fresh.log", ageMs = 1 * dayMs)
        File(dir, "notes.txt").writeText("keep me")
        assertEquals(1, CrashLogs.pruneOld(dir, maxAgeDays = 14))
        assertEquals(fresh, CrashLogs.latestIn(dir))
    }

    @Test
    fun pruneOld_keepsBoundaryAndMissingDir() {
        assertEquals(0, CrashLogs.pruneOld(File(dir, "nope")))
        assertEquals(0, CrashLogs.pruneOld(dir))
    }

    @Test
    fun deleteAll_removesCrashLogsOnly() {
        crash("crash-1.log", ageMs = 1000)
        crash("crash-2.log", ageMs = 2000)
        val notes = File(dir, "notes.txt").apply { writeText("keep me") }
        assertEquals(2, CrashLogs.deleteAll(dir))
        assertNull(CrashLogs.latestIn(dir))
        assertEquals(true, notes.exists())
    }

    @Test
    fun deleteAll_emptyOrMissingDir_returnsZero() {
        assertEquals(0, CrashLogs.deleteAll(dir))
        assertEquals(0, CrashLogs.deleteAll(File(dir, "nope")))
    }
}
