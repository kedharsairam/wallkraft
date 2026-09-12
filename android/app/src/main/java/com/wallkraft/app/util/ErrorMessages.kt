package com.wallkraft.app.util

import android.content.res.Resources
import com.wallkraft.app.R
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.domain.repository.WallpaperError

/**
 * Converts a thrown error into a user-presentable, localized message.
 *
 * Prefer [AppError.toUserMessage] for new code — [Throwable.toUserMessage] is
 * retained for legacy callers (e.g. rotation, download) that still throw.
 */
fun Throwable.toUserMessage(resources: Resources): String = when (this) {
    is WallpaperError.RateLimited -> resources.getString(R.string.error_rate_limited)
    is WallpaperError.Api -> when (code) {
        400 -> resources.getString(R.string.error_invalid_query)
        401, 403 -> resources.getString(R.string.error_invalid_key)
        404 -> resources.getString(R.string.error_not_found)
        429 -> resources.getString(R.string.error_rate_limited)
        else -> resources.getString(R.string.error_network)
    }
    else -> message ?: resources.getString(R.string.error_generic)
}

fun AppError.toUserMessage(resources: Resources): String = when (this) {
    is AppError.NetworkError.RateLimited -> resources.getString(R.string.error_rate_limited)
    is AppError.NetworkError.NoConnection -> resources.getString(R.string.error_network)
    is AppError.NetworkError.Timeout -> resources.getString(R.string.error_network)
    is AppError.NetworkError.ServerError -> resources.getString(R.string.error_network)
    is AppError.DataError.NotFound -> resources.getString(R.string.error_not_found)
    is AppError.DataError.Validation -> resources.getString(R.string.error_invalid_query)
    is AppError.DataError.Parse -> resources.getString(R.string.error_network)
    is AppError.AuthError.Unauthorized -> resources.getString(R.string.error_invalid_key)
    is AppError.AuthError.Expired -> resources.getString(R.string.error_invalid_key)
    is AppError.StorageError.DiskFull -> resources.getString(R.string.error_generic)
    is AppError.StorageError.PermissionDenied -> resources.getString(R.string.error_generic)
    is AppError.Unknown -> resources.getString(R.string.error_generic)
}
