package com.andrew264.habits.ui.usage.whitelist

import com.andrew264.habits.R
import kotlin.math.abs

private val appColorPalette = listOf(
    R.string.app_color_palette_1, R.string.app_color_palette_2, R.string.app_color_palette_3, R.string.app_color_palette_4, R.string.app_color_palette_5,
    R.string.app_color_palette_6, R.string.app_color_palette_7, R.string.app_color_palette_8, R.string.app_color_palette_9, R.string.app_color_palette_10,
    R.string.app_color_palette_11, R.string.app_color_palette_12, R.string.app_color_palette_13, R.string.app_color_palette_14, R.string.app_color_palette_15,
    R.string.app_color_palette_16
)

/**
 * Assigns a deterministic color from a predefined palette based on the package name's hash code.
 *
 * @param packageName The package name of the app.
 * @return A hex color string (e.g., "#F44336").
 */
fun assignColorForPackage(packageName: String): String {
    val index = abs(packageName.hashCode()) % appColorPalette.size
    return appColorPalette[index].toString()
}