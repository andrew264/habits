package com.andrew264.habits.ui.common.color_picker.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.andrew264.habits.ui.common.color_picker.HsvColor

fun Color.toHsv(): HsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    return HsvColor(hsv[0], hsv[1], hsv[2], this.alpha)
}

fun HsvColor.toColor(): Color {
    return Color.hsv(hue, saturation, value, alpha)
}

fun Color.toHexCode(includeAlpha: Boolean = false): String {
    val argb = this.toArgb()
    return if (includeAlpha) {
        String.format("#%08X", argb)
    } else {
        String.format("#%06X", argb and 0xFFFFFF)
    }
}

fun String.toColorOrNull(): Color? {
    return try {
        val clean = this.removePrefix("#")
        val colorLong = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> null
        }
    } catch (_: NumberFormatException) {
        null
    }
}
