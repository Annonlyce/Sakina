package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val SakinaDarkColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = OnEmeraldAccent,
    primaryContainer = EmeraldSubtle,
    onPrimaryContainer = EmeraldAccent,
    secondary = EmeraldAccent,
    onSecondary = OnEmeraldAccent,
    secondaryContainer = GraphiteRaised,
    onSecondaryContainer = PrimaryText,
    tertiary = EmeraldAccent,
    onTertiary = OnEmeraldAccent,
    background = CharcoalBg,
    onBackground = PrimaryText,
    surface = GraphiteSurface,
    onSurface = PrimaryText,
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = MutedText,
    surfaceContainerLowest = CharcoalBg,
    surfaceContainerLow = GraphiteSurface,
    surfaceContainer = GraphiteSurface,
    surfaceContainerHigh = GraphiteRaised,
    surfaceContainerHighest = GraphiteRaised,
    outline = GraphiteBorder,
    outlineVariant = GraphiteBorder,
    error = SoftError,
    onError = CharcoalBg
)

val SakinaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SakinaTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = SakinaDarkColorScheme,
            typography = SakinaTypography,
            shapes = SakinaShapes,
            content = content
        )
    }
}
