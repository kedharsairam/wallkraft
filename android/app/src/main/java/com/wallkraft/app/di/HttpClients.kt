/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.di

import javax.inject.Qualifier

/** OkHttpClient with [com.wallkraft.app.data.api.RetryInterceptor] for Wallhaven API + image fetches. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WallhavenClient

/** Plain OkHttpClient for GitHub API + APK downloads — no retry, fail-silent. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GithubClient
