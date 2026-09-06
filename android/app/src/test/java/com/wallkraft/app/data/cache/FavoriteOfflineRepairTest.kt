package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FavoriteOfflineRepairTest {

    private class FakeStore(
        val localIds: MutableSet<String> = mutableSetOf(),
        val failIds: Set<String> = emptySet(),
        val repaired: MutableList<String> = mutableListOf(),
    ) : OfflineImageStore {
        override fun fileFor(id: String): File? =
            if (id in localIds) File(id) else null

        override suspend fun save(wallpaper: Wallpaper): Boolean {
            repaired += wallpaper.id
            if (wallpaper.id in failIds) return false
            localIds += wallpaper.id
            return true
        }
    }

    private fun wallpaper(id: String) = Wallpaper(id = id, path = "https://example.com/$id.jpg")

    @Test
    fun missing_returns_only_without_local_copy() {
        val repair = FavoriteOfflineRepair(FakeStore(localIds = mutableSetOf("b")))
        val out = repair.missing(listOf(wallpaper("a"), wallpaper("b"), wallpaper("c")))
        assertEquals(listOf("a", "c"), out.map { it.id })
    }

    @Test
    fun missing_empty_when_all_local() {
        val repair = FavoriteOfflineRepair(FakeStore(localIds = mutableSetOf("a")))
        assertTrue(repair.missing(listOf(wallpaper("a"))).isEmpty())
    }

    @Test
    fun repairAll_restores_all_and_reports_progress() = runTest {
        val repair = FavoriteOfflineRepair(FakeStore())
        val seen = mutableListOf<Pair<Int, Int>>()
        val result = repair.repairAll(
            listOf(wallpaper("a"), wallpaper("b")),
        ) { done, total -> seen += done to total }

        assertEquals(2, result.restored)
        assertTrue(result.failed.isEmpty())
        assertEquals(listOf(1 to 2, 2 to 2), seen)
    }

    @Test
    fun repairAll_collects_failures() = runTest {
        val repair = FavoriteOfflineRepair(FakeStore(failIds = setOf("b")))
        val result = repair.repairAll(listOf(wallpaper("a"), wallpaper("b")))

        assertEquals(1, result.restored)
        assertEquals(listOf("b"), result.failed)
    }

    @Test
    fun repairAll_empty_is_noop() = runTest {
        val repair = FavoriteOfflineRepair(FakeStore())
        val result = repair.repairAll(emptyList())
        assertEquals(0, result.restored)
        assertTrue(result.failed.isEmpty())
    }
}
