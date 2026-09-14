package com.wallkraft.app.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GDPR erasure must run in dependency order: workers go quiet first, the
 * database is emptied before the preferences that describe it, files and
 * image caches follow, and the scheduler is reset to OFF last.
 *
 * A fake [WipeBackend] records every call — pure JVM, no Room/DataStore/
 * WorkManager needed. (Instrumented coverage of `AndroidWipeBackend` against
 * a real device is intentionally skipped: no emulator in this environment.)
 */
class WipeAllDataUseCaseTest {

    private class FakeBackend(
        val calls: MutableList<String> = mutableListOf(),
    ) : WipeBackend {
        override suspend fun cancelRotationWork() {
            calls += "cancelRotationWork"
        }
        override suspend fun clearDatabase() {
            calls += "clearDatabase"
        }
        override suspend fun clearSettings() {
            calls += "clearSettings"
        }
        override suspend fun clearRotationSettings() {
            calls += "clearRotationSettings"
        }
        override suspend fun clearRotationCrops() {
            calls += "clearRotationCrops"
        }
        override suspend fun clearSearchHistory() {
            calls += "clearSearchHistory"
        }
        override suspend fun clearSecurePrefs() {
            calls += "clearSecurePrefs"
        }
        override suspend fun wipeFiles() {
            calls += "wipeFiles"
        }
        override suspend fun evictImageCaches() {
            calls += "evictImageCaches"
        }
        override suspend fun resetRotation() {
            calls += "resetRotation"
        }
    }

    @Test
    fun `wipeAll clears every area exactly once in order`() = runTest {
        val backend = FakeBackend()
        WipeAllDataUseCase(backend).wipeAll()

        assertEquals(
            listOf(
                "cancelRotationWork",
                "clearDatabase",
                "clearSettings",
                "clearRotationSettings",
                "clearRotationCrops",
                "clearSearchHistory",
                "clearSecurePrefs",
                "wipeFiles",
                "evictImageCaches",
                "resetRotation",
            ),
            backend.calls,
        )
    }

    @Test
    fun `wipeAll resets rotation last so scheduler lands clean`() = runTest {
        val backend = FakeBackend()
        WipeAllDataUseCase(backend).wipeAll()

        assertEquals("resetRotation", backend.calls.last())
        assertEquals("cancelRotationWork", backend.calls.first())
    }
}
