/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.model

/** A wallpaper the user has favorited, persisted locally. */
data class Favorite(
    val wallpaper: Wallpaper,
    val addedAt: Long,
)
