/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.model

/**
 * Info about a newer GitHub release. Pure domain — no Android dependency.
 */
data class AppUpdateInfo(
    val tag: String,
    val version: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val notes: String,
)

/**
 * Compares dotted versions numerically ("1.10.0" > "1.9.2").
 * Returns >0 if [a] is newer than [b]. Strips leading v/V.
 */
fun compareVersions(a: String, b: String): Int {
    fun parse(v: String): List<Int> = v.trim()
        .removePrefix("v").removePrefix("V")
        .split("+").first()
        .split(".")
        .map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
    val pa = parse(a)
    val pb = parse(b)
    val n = maxOf(pa.size, pb.size)
    for (i in 0 until n) {
        val va = if (i < pa.size) pa[i] else 0
        val vb = if (i < pb.size) pb[i] else 0
        if (va != vb) return va - vb
    }
    return 0
}
