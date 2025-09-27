package com.andrew264.habits.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

private const val TRANSITION_DURATION = 300
private const val FADE_OUT_DURATION = (TRANSITION_DURATION * 0.35f).toInt()
private const val FADE_IN_DELAY = FADE_OUT_DURATION
private const val FADE_IN_DURATION = TRANSITION_DURATION - FADE_IN_DELAY
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

fun sharedAxisXEnter(forward: Boolean, slideDistance: Int): EnterTransition {
    val sign = if (forward) 1f else -1f
    return fadeIn(
        animationSpec = tween(
            durationMillis = FADE_IN_DURATION,
            delayMillis = FADE_IN_DELAY,
            easing = LinearEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = EmphasizedEasing),
        initialOffsetX = { (slideDistance * sign).toInt() }
    )
}

fun sharedAxisXExit(forward: Boolean, slideDistance: Int): ExitTransition {
    val sign = if (forward) 1f else -1f
    return fadeOut(
        animationSpec = tween(
            durationMillis = FADE_OUT_DURATION,
            easing = LinearEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = EmphasizedEasing),
        targetOffsetX = { -(slideDistance * sign).toInt() }
    )
}