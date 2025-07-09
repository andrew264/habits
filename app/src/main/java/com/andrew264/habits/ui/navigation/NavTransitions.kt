package com.andrew264.habits.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

private const val TRANSITION_DURATION = 300
private const val FADE_OUT_DURATION = 90
private const val FADE_IN_DELAY = FADE_OUT_DURATION
private const val FADE_IN_DURATION = TRANSITION_DURATION - FADE_IN_DELAY
private const val SLIDE_DISTANCE_PERCENT = 0.1f // Reduced for a more subtle effect

/**
 * Creates a reusable Shared Axis X transition.
 *
 * @param forward Determines the direction of the transition. `true` for forward (e.g., push), `false` for backward (e.g., pop).
 * @return A [ContentTransform] to be used in NavDisplay.
 */
fun sharedAxisX(forward: Boolean): ContentTransform {
    val sign = if (forward) 1f else -1f
    return fadeIn(
        animationSpec = tween(
            durationMillis = FADE_IN_DURATION,
            delayMillis = FADE_IN_DELAY,
            easing = LinearEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> (fullWidth * SLIDE_DISTANCE_PERCENT * sign).toInt() }
    ) togetherWith fadeOut(
        animationSpec = tween(
            durationMillis = FADE_OUT_DURATION,
            easing = LinearEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> -(fullWidth * SLIDE_DISTANCE_PERCENT * sign).toInt() }
    )
}