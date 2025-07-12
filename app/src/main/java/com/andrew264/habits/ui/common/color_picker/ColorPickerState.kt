package com.andrew264.habits.ui.common.color_picker

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.andrew264.habits.ui.common.color_picker.utils.toColor
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.color_picker.utils.toHexCode
import com.andrew264.habits.ui.common.color_picker.utils.toHsv

@Stable
class ColorPickerState(initialColor: Color) {
    var hsvColor by mutableStateOf(initialColor.toHsv())
    var hexCode by mutableStateOf(initialColor.toHexCode(includeAlpha = true))
    var isValidHex by mutableStateOf(true)

    fun updateFromHex(input: String) {
        val cleaned = if (input.startsWith("#")) input else "#$input"
        val parsed = cleaned.toColorOrNull()

        if (parsed != null) {
            hsvColor = parsed.toHsv()
            isValidHex = true
        } else {
            isValidHex = false
        }
        hexCode = cleaned
    }

    fun updateFromHsv(newHsv: HsvColor) {
        hsvColor = newHsv
        hexCode = newHsv.toColor().toHexCode(includeAlpha = true)
        isValidHex = true
    }
}
