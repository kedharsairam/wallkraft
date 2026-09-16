/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Favorites

@Serializable
data object Settings

@Serializable
data object History

@Serializable
data class Browse(val query: String = "", val title: String = "")

@Serializable
data class Detail(val id: String, val thumb: String = "", val path: String = "")
