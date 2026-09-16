/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.provider

import androidx.core.content.FileProvider

/**
 * Thin wrapper around [FileProvider] that serves cached favorite images via
 * `content://` URIs. The actual path configuration lives in `file_paths.xml`
 * (already registered by the main `FileProvider` in the manifest).
 *
 * This provider exists solely to give the ContentProvider a distinct authority
 * so callers can resolve favorite-image URIs without colliding with the
 * general-purpose sharing FileProvider.
 */
class WallKraftFileProvider : FileProvider()
