package com.wallkraft.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wallkraft.app.domain.model.Tag
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.serialization.json.Json

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val url: String,
    val path: String,
    val thumbnail: String,
    val thumbnailLarge: String?,
    val dimensionX: Int,
    val dimensionY: Int,
    val ratio: String,
    val fileSize: Long,
    val favoritesCount: Int,
    val category: String,
    val tagsJson: String,
    val addedAt: Long,
) {
    fun toWallpaper(json: Json): Wallpaper {
        val tags = runCatching { json.decodeFromString<List<Tag>>(tagsJson) }.getOrDefault(emptyList())
        return Wallpaper(
            id = id,
            url = url,
            path = path,
            thumbs = Thumbs(
                large = thumbnailLarge,
                // Empty string is the no-thumb sentinel (keeps the column
                // non-null so Room schema stays at v1 — no migration needed).
                original = thumbnail.ifEmpty { null },
            ),
            dimensionX = dimensionX,
            dimensionY = dimensionY,
            ratio = ratio,
            fileSize = fileSize,
            favorites = favoritesCount,
            category = category,
            tags = tags,
        )
    }

    companion object {
        fun fromWallpaper(
            wallpaper: Wallpaper,
            addedAt: Long,
            json: Json,
        ): FavoriteEntity =
            FavoriteEntity(
                id = wallpaper.id,
                url = wallpaper.url,
                path = wallpaper.path,
                // Never fall back to the full-resolution [path] here — the grid
                // would download multi-megabyte originals. Empty string means
                // "no thumbnail": the grid shows its placeholder instead.
                thumbnail = wallpaper.thumbs.original ?: wallpaper.thumbs.small ?: "",
                thumbnailLarge = wallpaper.thumbs.large,
                dimensionX = wallpaper.dimensionX,
                dimensionY = wallpaper.dimensionY,
                ratio = wallpaper.ratio,
                fileSize = wallpaper.fileSize,
                favoritesCount = wallpaper.favorites,
                category = wallpaper.category,
                tagsJson = json.encodeToString(ListSerializerTag, wallpaper.tags),
                addedAt = addedAt,
            )
    }
}

private val ListSerializerTag = kotlinx.serialization.builtins.ListSerializer(Tag.serializer())
