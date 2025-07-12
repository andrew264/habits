package com.andrew264.habits.ui.common.color_picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.common.color_picker.components.AlphaSlider
import com.andrew264.habits.ui.common.color_picker.components.HueSlider
import com.andrew264.habits.ui.common.color_picker.components.SatValPlane
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlphaSlider: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
    ) {
        SatValPlane(
            hsvColor = state.hsvColor,
            onSaturationValueChange = { s, v ->
                state.updateFromHsv(state.hsvColor.copy(saturation = s, value = v))
            }
        )

        HueSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            hue = state.hsvColor.hue,
            onHueChange = { newHue ->
                state.updateFromHsv(state.hsvColor.copy(hue = newHue))
            },
            onInteractionEnd = {}
        )

        if (showAlphaSlider) {
            AlphaSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                hsvColor = state.hsvColor,
                onAlphaChange = { newAlpha ->
                    state.updateFromHsv(state.hsvColor.copy(alpha = newAlpha))
                },
                onInteractionEnd = {}
            )
        }
    }
}
