package com.wallkraft.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Immutable
@Serializable
data class Thumbs(
    @SerialName("large") val large: String? = null,
    @SerialName("original") val original: String? = null,
    @SerialName("small") val small: String? = null,
)

@Immutable
@Serializable
data class Tag(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
)

/**
 * The uploader's avatar image at the sizes Wallhaven serves. Only the detail
 * endpoint returns the uploader; search listings omit it entirely.
 */
@Immutable
@Serializable
data class UploaderAvatar(
    @SerialName("200px") val px200: String = "",
    @SerialName("128px") val px128: String = "",
    @SerialName("32px") val px32: String = "",
    @SerialName("20px") val px20: String = "",
)

/**
 * The wallpaper's uploader. `null` (or an empty [username]) means the account
 * no longer exists — Wallhaven keeps the wallpaper but the uploader is gone.
 */
@Immutable
@Serializable
data class Uploader(
    @SerialName("username") val username: String = "",
    @SerialName("avatar") val avatar: UploaderAvatar? = null,
)

@Immutable
@Serializable
data class Wallpaper(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String = "",
    @SerialName("path") val path: String = "",
    @SerialName("thumbs") val thumbs: Thumbs = Thumbs(),
    @SerialName("dimension_x") val dimensionX: Int = 1920,
    @SerialName("dimension_y") val dimensionY: Int = 1080,
    @SerialName("ratio") val ratio: String = "16:9",
    @SerialName("file_size") val fileSize: Long = 0,
    @SerialName("favorites") val favorites: Int = 0,
    @SerialName("views") val views: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("category") val category: String = "general",
    @SerialName("purity") val purity: String = "sfw",
    @SerialName("tags") val tags: List<Tag> = emptyList(),
    @SerialName("uploader") val uploader: Uploader? = null,
) {
    // Wallhaven pre-crops `small`/`large` to fixed 3:2 / 16:9 ratios, which
    // makes non-matching wallpapers look zoomed. `original` preserves the true
    // aspect ratio, so the grid always shows the full image.
    //
    // The grid MUST never fall back to [path] — that's the full-resolution
    // file (can be multi-megabyte) meant only for the detail screen.
    val thumbnail: String? get() = thumbs.original ?: thumbs.large ?: thumbs.small
    val resolution: String get() = "${dimensionX}x$dimensionY"

    /** True when the API reports this wallpaper as safe-for-work. */
    val isSFW: Boolean get() = purity == "sfw"

    /** Purity as an enum value, parsed from the raw API string. */
    val purityEnum: Purity get() = when (purity) {
        "sfw" -> Purity.SFW
        "sketchy" -> Purity.Sketchy
        else -> Purity.SFW
    }

    /** Category as an enum value, parsed from the raw API string. */
    val categoryEnum: Category get() = when (category) {
        "anime" -> Category.Anime
        "people" -> Category.People
        else -> Category.General
    }

    /**
     * The uploader's username, or "" when there is none — a deleted account,
     * a guest upload, or data that hasn't loaded the uploader yet (search
     * listings never carry it; only the detail endpoint does).
     */
    val uploaderName: String get() = uploader?.username?.takeIf { it.isNotBlank() } ?: ""

    /**
     * Best available avatar URL (128px preferred — crisp on a 32dp circle at
     * typical densities), or "" when the uploader has no avatar.
     */
    val uploaderAvatarUrl: String get() {
        val avatar = uploader?.avatar ?: return ""
        return avatar.px128.ifBlank { avatar.px200.ifBlank { avatar.px32.ifBlank { avatar.px20 } } }
    }

    fun fileSizeFormatted(): String = when {
        fileSize < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", fileSize / 1024.0)
        fileSize < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0))
    }
}

@Immutable
@Serializable
data class WallpaperMeta(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 24,
    @SerialName("total") val total: Int = 0,
)

@Immutable
@Serializable
data class WallpaperResponse(
    @SerialName("data") val data: List<Wallpaper> = emptyList(),
    @SerialName("meta") val meta: WallpaperMeta = WallpaperMeta(),
)
