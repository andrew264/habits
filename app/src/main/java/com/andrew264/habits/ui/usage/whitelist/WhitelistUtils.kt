package com.andrew264.habits.ui.usage.whitelist

import kotlin.math.abs

private val appColorPalette = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
    "#8BC34A", "#FFC107", "#FF9800", "#FF5722", "#795548",
    "#607D8B"
)

/**
 * Assigns a deterministic color from a predefined palette based on the package name's hash code.
 *
 * @param packageName The package name of the app.
 * @return A hex color string (e.g., "#F44336").
 */
fun assignColorForPackage(packageName: String): String {
    val index = abs(packageName.hashCode()) % appColorPalette.size
    return appColorPalette[index]
}