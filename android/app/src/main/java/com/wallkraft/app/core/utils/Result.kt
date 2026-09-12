package com.wallkraft.app.core.utils

import com.wallkraft.app.core.errors.AppError

/**
 * Typed result wrapper for repository / use-case returns.
 *
 * Success carries data; Failure carries a typed [AppError]. This is the
 * single success/failure type for the app (ARCHITECTURE.md §3) — callers
 * should switch on it instead of catching exceptions.
 *
 * Note: this is distinct from Kotlin's stdlib [Result]. Import this type
 * explicitly when you need typed errors (`com.wallkraft.app.core.utils.Result`).
 * WallpaperRepository still returns stdlib Result for now; it will migrate
 * to this type in a follow-up without breaking the current build.
 */
sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Failure(val error: AppError) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success

    val isFailure: Boolean get() = this is Failure

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorOrNull(): AppError? = (this as? Failure)?.error
}

/** Convenience: wrap a value as [Result.Success]. */
fun <T> T.asSuccess(): Result<T> = Result.Success(this)

/** Convenience: wrap an [AppError] as [Result.Failure]. */
fun AppError.asFailure(): Result<Nothing> = Result.Failure(this)
