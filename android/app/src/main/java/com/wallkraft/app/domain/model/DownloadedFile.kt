package com.wallkraft.app.domain.model

import android.net.Uri

/** A wallpaper file the app has downloaded into the public Downloads folder. */
data class DownloadedFile(
    val wallpaperId: String,
    val name: String,
    val size: Long,
    val uriString: String,
    val relativePath: String,
) {
    /** Compatibility getter — prefer [uriString] as source of truth. */
    @Deprecated("Use uriString", ReplaceWith("Uri.parse(uriString)"))
    val uri: Uri get() = Uri.parse(uriString)
}
