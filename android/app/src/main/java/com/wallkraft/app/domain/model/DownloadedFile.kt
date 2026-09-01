package com.wallkraft.app.domain.model

import android.net.Uri

/** A wallpaper file the app has downloaded into the public Downloads folder. */
data class DownloadedFile(
    val wallpaperId: String,
    val name: String,
    val size: Long,
    val uri: Uri,
    val relativePath: String,
)
