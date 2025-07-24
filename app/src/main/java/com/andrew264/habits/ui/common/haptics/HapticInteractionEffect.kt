package com.andrew264.habits.ui.common.haptics

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun HapticInteractionEffect(
    interactionSource: InteractionSource,
    onPress: Int? = HapticFeedbackConstants.VIRTUAL_KEY,
    onRelease: Int? = HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
) {
    val view = LocalView.current
    LaunchedEffect(interactionSource, onPress, onRelease) {
        interactionSource.interactions.collect { interaction: Interaction ->
            when (interaction) {
                is PressInteraction.Press -> if (onPress != null) view.performHapticFeedback(onPress)
                is PressInteraction.Release,
                is PressInteraction.Cancel -> if (onRelease != null) view.performHapticFeedback(onRelease)
            }
        }
    }
}