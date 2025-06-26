package com.andrew264.habits.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitsTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    supportsDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    val darkColorScheme = darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80
    )
    val colorScheme =
        when {
            supportsDynamicColor && isDarkTheme -> {
                dynamicDarkColorScheme(LocalContext.current)
            }

            supportsDynamicColor && !isDarkTheme -> {
                dynamicLightColorScheme(LocalContext.current)
            }

            isDarkTheme -> darkColorScheme
            else -> expressiveLightColorScheme()
        }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}