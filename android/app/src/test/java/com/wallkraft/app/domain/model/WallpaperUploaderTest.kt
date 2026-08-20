package com.wallkraft.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperUploaderTest {

    @Test
    fun `uploaderName comes from the uploader object`() {
        val w = Wallpaper(id = "1", uploader = Uploader(username = "Pc7"))
        assertEquals("Pc7", w.uploaderName)
    }

    @Test
    fun `uploaderName is blank when there is no uploader`() {
        assertEquals("", Wallpaper(id = "1").uploaderName)
    }

    @Test
    fun `uploaderName is blank when the username is empty (deleted account)`() {
        val w = Wallpaper(id = "1", uploader = Uploader(username = ""))
        assertEquals("", w.uploaderName)
    }

    @Test
    fun `avatarUrl prefers the 128px size`() {
        val w = Wallpaper(
            id = "1",
            uploader = Uploader(avatar = UploaderAvatar(px128 = "a128", px32 = "a32")),
        )
        assertEquals("a128", w.uploaderAvatarUrl)
    }

    @Test
    fun `avatarUrl falls back through the remaining sizes`() {
        val w = Wallpaper(id = "1", uploader = Uploader(avatar = UploaderAvatar(px32 = "a32")))
        assertEquals("a32", w.uploaderAvatarUrl)
    }

    @Test
    fun `avatarUrl is blank when no avatar is present`() {
        val w = Wallpaper(id = "1", uploader = Uploader())
        assertEquals("", w.uploaderAvatarUrl)
    }
}
