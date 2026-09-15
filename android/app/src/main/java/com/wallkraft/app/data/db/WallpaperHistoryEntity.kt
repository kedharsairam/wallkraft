package com.wallkraft.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "wallpaper_history",
    primaryKeys = ["wallpaper_id", "set_at"],
)
data class WallpaperHistoryEntity(
    @ColumnInfo(name = "wallpaper_id") val wallpaperId: String,
    val path: String,
    val thumbnail: String,
    @ColumnInfo(name = "set_at") val setAt: Long,
    val source: String,
)
