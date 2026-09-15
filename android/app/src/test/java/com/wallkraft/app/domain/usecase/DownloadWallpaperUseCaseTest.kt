package com.wallkraft.app.domain.usecase

import android.content.Context
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DownloadWallpaperUseCase delegates to the [Downloader] abstraction and
 * returns download IDs. Errors are surfaced as -1 per wallpaper.
 */
class DownloadWallpaperUseCaseTest {

    private class FakeDownloader : DownloadWallpaperUseCase.Downloader {
        val downloaded = mutableListOf<Wallpaper>()
        var downloadResult: Long = 123L

        override fun download(context: Context, wallpaper: Wallpaper): Long {
            downloaded += wallpaper
            return downloadResult
        }
    }

    @Test
    fun `download returns download id from downloader`() = runTest {
        val downloader = FakeDownloader()
        downloader.downloadResult = 42L
        val useCase = DownloadWallpaperUseCase(
            context = android.app.Application(),
            downloader = downloader,
        )
        val wallpaper = Wallpaper(id = "wp-1", path = "https://example.com/wp1.jpg")

        val result = useCase.download(wallpaper)

        assertEquals(42L, result)
        assertEquals(1, downloader.downloaded.size)
        assertEquals("wp-1", downloader.downloaded[0].id)
    }

    @Test
    fun `download returns -1 on failure`() = runTest {
        val downloader = FakeDownloader()
        downloader.downloadResult = -1L
        val useCase = DownloadWallpaperUseCase(
            context = android.app.Application(),
            downloader = downloader,
        )
        val wallpaper = Wallpaper(id = "wp-fail", path = "https://example.com/fail.jpg")

        val result = useCase.download(wallpaper)

        assertEquals(-1L, result)
    }

    @Test
    fun `downloadAll enqueues each wallpaper`() = runTest {
        val downloader = FakeDownloader()
        val useCase = DownloadWallpaperUseCase(
            context = android.app.Application(),
            downloader = downloader,
        )
        val wallpapers = listOf(
            Wallpaper(id = "wp-1", path = "https://example.com/1.jpg"),
            Wallpaper(id = "wp-2", path = "https://example.com/2.jpg"),
            Wallpaper(id = "wp-3", path = "https://example.com/3.jpg"),
        )

        val results = useCase.downloadAll(wallpapers)

        assertEquals(3, results.size)
        assertEquals(listOf(123L, 123L, 123L), results)
        assertEquals(3, downloader.downloaded.size)
        assertEquals("wp-1", downloader.downloaded[0].id)
        assertEquals("wp-2", downloader.downloaded[1].id)
        assertEquals("wp-3", downloader.downloaded[2].id)
    }

    @Test
    fun `downloadAll with empty list returns empty`() = runTest {
        val downloader = FakeDownloader()
        val useCase = DownloadWallpaperUseCase(
            context = android.app.Application(),
            downloader = downloader,
        )

        val results = useCase.downloadAll(emptyList())

        assertTrue(results.isEmpty())
    }

    @Test
    fun `downloadAll with mixed success and failure`() = runTest {
        val useCase = DownloadWallpaperUseCase(
            context = android.app.Application(),
            downloader = object : DownloadWallpaperUseCase.Downloader {
                override fun download(context: Context, wallpaper: Wallpaper): Long =
                    if (wallpaper.id == "wp-fail") -1L else 1L
            },
        )
        val wallpapers = listOf(
            Wallpaper(id = "wp-1", path = "https://example.com/1.jpg"),
            Wallpaper(id = "wp-fail", path = "https://example.com/fail.jpg"),
            Wallpaper(id = "wp-3", path = "https://example.com/3.jpg"),
        )

        val results = useCase.downloadAll(wallpapers)

        assertEquals(listOf(1L, -1L, 1L), results)
    }
}
