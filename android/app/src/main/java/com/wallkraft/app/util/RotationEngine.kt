package com.wallkraft.app.util

import com.wallkraft.app.domain.model.Wallpaper
import kotlin.random.Random

data class PickResult(val index: Int, val id: String)

object RotationEngine {

    /**
     * Picks a random wallpaper from [candidates] that is not in [recentIds].
     *
     * When all candidates have been recently shown (pool ≤ window), the recent
     * list is gracefully reset and any candidate may be picked.
     *
     * @param candidates  available wallpapers (must be non-empty for a valid result).
     * @param recentIds   IDs of recently shown wallpapers to avoid.
     * @param seed        RNG seed for deterministic picks; defaults to a random seed.
     * @return [PickResult] with the chosen index and id, or (-1, "") when [candidates] is empty.
     */
    fun pickNoRepeat(
        candidates: List<Wallpaper>,
        recentIds: List<String>,
        seed: Long = Random.nextLong(),
    ): PickResult {
        if (candidates.isEmpty()) return PickResult(-1, "")
        val eligible = candidates.filter { it.id !in recentIds }
            .ifEmpty { candidates }
        val picked = eligible[Random(seed).nextInt(eligible.size)]
        return PickResult(candidates.indexOf(picked), picked.id)
    }
}
