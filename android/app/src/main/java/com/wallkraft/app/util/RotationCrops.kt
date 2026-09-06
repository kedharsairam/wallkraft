package com.wallkraft.app.util

import com.wallkraft.app.domain.model.CropRect
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Pure encode/decode for the saved-crop map (wallpaper id → crop rect).
 * Corrupt payloads decode to empty — a bad cache must never wedge rotation.
 */
object RotationCrops {

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        // Explicit: every rect field is always written, so a stored payload
        // is self-describing (a default-valued rect still round-trips).
        encodeDefaults = true
    }
    private val serializer = MapSerializer(String.serializer(), CropRect.serializer())

    fun encode(crops: Map<String, CropRect>): String =
        json.encodeToString(serializer, crops)

    fun decode(raw: String): Map<String, CropRect> =
        if (raw.isBlank()) emptyMap()
        else runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyMap())
}
