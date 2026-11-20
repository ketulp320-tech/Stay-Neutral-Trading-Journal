package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val WhiteBlueGreenColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = PureWhite,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Green600,
    onSecondary = PureWhite,
    secondaryContainer = Green100,
    onSecondaryContainer = Green900,
    tertiary = NeutralBlue700,
    onTertiary = PureWhite,
    tertiaryContainer = NeutralBlue100,
    onTertiaryContainer = NeutralBlue900,
    background = PureWhite,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Blue50,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SnowSurface,
    surfaceContainer = IceBlueSurface,
    surfaceContainerHigh = Blue100,
    surfaceContainerHighest = Slate200,
    outline = Slate300,
    outlineVariant = MistBorder,
    error = Red600,
    onError = PureWhite,
    errorContainer = Red100,
    onErrorContainer = Red900,
)

private val StayNeutralShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun StayNeutralTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTradingColors provides LightTradingColors) {
        MaterialTheme(
            colorScheme = WhiteBlueGreenColorScheme,
            typography = StayNeutralTypography,
            shapes = StayNeutralShapes,
            content = content,
        )
    }
}
