package com.andrew264.habits.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = White,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    inversePrimary = Primary80,
    secondary = Secondary40,
    onSecondary = White,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = White,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Primary40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    error = Error40,
    onError = White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Black,

    // M3 Tonal Surface Colors
    surfaceBright = Neutral99,
    surfaceDim = NeutralVariant80,
    surfaceContainer = Neutral95,
    surfaceContainerLow = Neutral95,
    surfaceContainerLowest = White,
    surfaceContainerHigh = Neutral90,
    surfaceContainerHighest = Neutral90,

    // M3 Fixed Variant Colors
    primaryFixed = Primary90,
    primaryFixedDim = Primary80,
    onPrimaryFixed = Primary10,
    onPrimaryFixedVariant = Primary30,
    secondaryFixed = Secondary90,
    secondaryFixedDim = Secondary80,
    onSecondaryFixed = Secondary10,
    onSecondaryFixedVariant = Secondary30,
    tertiaryFixed = Tertiary90,
    tertiaryFixedDim = Tertiary80,
    onTertiaryFixed = Tertiary10,
    onTertiaryFixedVariant = Tertiary30
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    inversePrimary = Primary40,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Primary80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Black,

    // M3 Tonal Surface Colors
    surfaceBright = Neutral20,
    surfaceDim = Neutral10,
    surfaceContainer = Neutral10,
    surfaceContainerLow = Neutral10,
    surfaceContainerLowest = Neutral10,
    surfaceContainerHigh = Neutral20,
    surfaceContainerHighest = Neutral20,

    // M3 Fixed Variant Colors
    primaryFixed = Primary90,
    primaryFixedDim = Primary80,
    onPrimaryFixed = Primary10,
    onPrimaryFixedVariant = Primary30,
    secondaryFixed = Secondary90,
    secondaryFixedDim = Secondary80,
    onSecondaryFixed = Secondary10,
    onSecondaryFixedVariant = Secondary30,
    tertiaryFixed = Tertiary90,
    tertiaryFixedDim = Tertiary80,
    onTertiaryFixed = Tertiary10,
    onTertiaryFixedVariant = Tertiary30
)

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitsTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    supportsDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            supportsDynamicColor && isDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
            supportsDynamicColor && !isDarkTheme -> dynamicLightColorScheme(LocalContext.current)
            isDarkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
    ) {
        CompositionLocalProvider {
            content()
        }
    }
}