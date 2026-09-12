package com.wallkraft.app.core.errors

/**
 * Unified error hierarchy for the app (ARCHITECTURE.md §3).
 *
 * Keeps domain and presentation free of exception-type checks — every
 * failure is mapped to a typed [AppError] and surfaced via [com.wallkraft.app.core.utils.Result].
 *
 * The hierarchy is intentionally shallow: 4 top-level buckets (Network,
 * Data, Storage, Auth) plus Unknown. Add leaf types as needed, but
 * never leak raw exceptions or HTTP codes outside the data layer.
 */
sealed interface AppError {

    /** Network / transport failures. */
    sealed interface NetworkError : AppError {
        /** No Internet / DNS / socket. */
        data object NoConnection : NetworkError

        /** Request timed out (connect/read/call timeout). */
        data object Timeout : NetworkError

        /** 5xx or non-2xx server failure. */
        data class ServerError(val code: Int? = null, val message: String? = null) : NetworkError

        /** 429 Too Many Requests — rate limit hit. */
        data object RateLimited : NetworkError
    }

    /** Local data / parsing / validation failures. */
    sealed interface DataError : AppError {
        /** JSON / model parsing failed. */
        data class Parse(val message: String? = null, val cause: Throwable? = null) : DataError

        /** Business-rule validation failed (e.g. blank collection name is handled elsewhere, but kept for symmetry). */
        data class Validation(val message: String? = null) : DataError

        /** Requested entity not found. */
        data object NotFound : DataError
    }

    /** Device storage / file-system failures. */
    sealed interface StorageError : AppError {
        /** No space left on device. */
        data object DiskFull : StorageError

        /** Media / file permission denied. */
        data object PermissionDenied : StorageError
    }

    /** Authentication / authorization failures. */
    sealed interface AuthError : AppError {
        /** 401 Unauthorized / invalid API key. */
        data object Unauthorized : AuthError

        /** 403 / token or session expired. */
        data object Expired : AuthError
    }

    /** Fallback — preserves the original throwable for logging, but callers should not branch on it. */
    data class Unknown(val throwable: Throwable? = null, val message: String? = null) : AppError
}
