package com.wallkraft.app.domain.model

/** A wallpaper file the app has downloaded into the public Downloads folder. Pure domain — no Android dependency. */
data class DownloadedFile(
    val wallpaperId: String,
    val name: String,
    val size: Long,
    val uriString: String,
    val relativePath: String,
)
