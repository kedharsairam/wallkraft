package com.wallkraft.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope

/**
 * A [FlingBehavior] that glides a little further than the platform default.
 *
 * The default Compose fling uses Android's spline decay, which stops fairly
 * quickly. This uses an exponential decay with a moderately low friction
 * multiplier, so a swipe coasts a bit longer — a smooth glide without the
 * "slides forever" feel. Lower [frictionMultiplier] = longer glide.
 */
class SmoothFlingBehavior(
    private val frictionMultiplier: Float = 0.6f,
) : FlingBehavior {

    private val decay: DecayAnimationSpec<Float> =
        exponentialDecay(frictionMultiplier = frictionMultiplier)

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var lastValue = 0f
        var velocityLeft = 0f
        val animatable = Animatable(0f)
        animatable.animateDecay(initialVelocity, decay) {
            val delta = value - lastValue
            lastValue = value
            velocityLeft = velocity
            scrollBy(delta)
        }
        return velocityLeft
    }
}