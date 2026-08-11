package com.wallkraft.app.util

import android.content.res.Resources
import com.wallkraft.app.R
import com.wallkraft.app.domain.repository.WallpaperError

/**
 * Converts a thrown error into a user-presentable, localized message.
 *
 * The technical detail stays in the exception's [message] (for logs); users
 * get a clean, localized line instead of raw strings like "API error: 500".
 */
fun Throwable.toUserMessage(resources: Resources): String = when (this) {
    is WallpaperError.RateLimited -> resources.getString(R.string.error_rate_limited)
    is WallpaperError.Api -> when (code) {
        400 -> resources.getString(R.string.error_invalid_query)
        401, 403 -> resources.getString(R.string.error_invalid_key)
        404 -> resources.getString(R.string.error_not_found)
        else -> resources.getString(R.string.error_network)
    }
    else -> message ?: resources.getString(R.string.error_generic)
}
