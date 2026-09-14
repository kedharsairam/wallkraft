package com.wallkraft.app.data.api

import com.wallkraft.app.core.design.KraftConstants
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Centralized GET retry for Wallhaven traffic.
 *
 * Retries [IOException] (network blips) and HTTP 500..599 with exponential
 * backoff + full jitter. Never retries 400/401/403/404 or 429 — a 429 is
 * account-wide, so hammering would only extend the ban; callers map it to
 * [com.wallkraft.app.core.errors.AppError.NetworkError.RateLimited] instead.
 *
 * Only GET requests are retried; other methods pass through untouched.
 *
 * @param maxAttempts max retries after the initial call (total calls =
 * maxAttempts + 1). Matches the old WallhavenApi MAX_RETRIES semantics.
 * @param sleeper injectable suspend delay for tests (defaults to [delay]).
 */
class RetryInterceptor(
    private val maxAttempts: Int = KraftConstants.RetryMax,
    private val sleeper: suspend (Long) -> Unit = ::delay,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") {
            return chain.proceed(request)
        }
        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                when {
                    response.isSuccessful -> return response
                    response.code in 500..599 && attempt < maxAttempts -> {
                        response.close()
                        runBlocking { sleeper(computeDelayMillis(attempt)) }
                        attempt++
                    }
                    else -> return response
                }
            } catch (e: IOException) {
                if (chain.call().isCanceled()) {
                    throw CancellationException("Call canceled", e)
                }
                if (attempt >= maxAttempts) throw e
                runBlocking { sleeper(computeDelayMillis(attempt)) }
                attempt++
            }
        }
    }

    companion object {
        /**
         * Exponential backoff with full jitter: cap = base * 2^attempt,
         * actual delay uniform in [0, cap).
         */
        fun computeDelayMillis(
            attempt: Int,
            baseMs: Long = KraftConstants.RetryBackoffBaseMs,
            random: Random = Random.Default,
        ): Long {
            val cap = baseMs * (1L shl attempt.coerceAtLeast(0))
            if (cap <= 0L) return 0L
            return random.nextLong(0L, cap)
        }
    }
}
