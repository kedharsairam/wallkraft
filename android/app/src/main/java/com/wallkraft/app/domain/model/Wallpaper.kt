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
    @SerialName("category") val category: String = "general",
    @SerialName("purity") val purity: String = "sfw",
    @SerialName("tags") val tags: List<Tag> = emptyList(),
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
    val isSfw: Boolean get() = purity == "sfw"

    fun fileSizeFormatted(): String =
        if (fileSize < 1024 * 1024) {
            String.format(Locale.US, "%.1f KB", fileSize / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", fileSize / (1024.0 * 1024.0))
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
