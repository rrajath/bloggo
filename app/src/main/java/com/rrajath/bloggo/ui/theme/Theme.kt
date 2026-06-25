package com.rrajath.bloggo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private fun lightScheme(accent: Accent): ColorScheme = when (accent) {
    Accent.INDIGO -> lightColorScheme(
        primary = IndigoLightPrimary,
        onPrimary = IndigoLightOnPrimary,
        primaryContainer = IndigoLightPrimaryContainer,
        onPrimaryContainer = IndigoLightOnPrimaryContainer,
        secondary = IndigoLightPrimary,
        onSecondary = IndigoLightOnPrimary,
        secondaryContainer = IndigoLightPrimaryContainer,
        onSecondaryContainer = IndigoLightOnPrimaryContainer,
        tertiary = IndigoLightPrimary,
        onTertiary = IndigoLightOnPrimary,
        tertiaryContainer = IndigoLightPrimaryContainer,
        onTertiaryContainer = IndigoLightOnPrimaryContainer,
    )
    Accent.GREEN -> lightColorScheme(
        primary = GreenLightPrimary,
        onPrimary = GreenLightOnPrimary,
        primaryContainer = GreenLightPrimaryContainer,
        onPrimaryContainer = GreenLightOnPrimaryContainer,
        secondary = GreenLightPrimary,
        onSecondary = GreenLightOnPrimary,
        secondaryContainer = GreenLightPrimaryContainer,
        onSecondaryContainer = GreenLightOnPrimaryContainer,
        tertiary = GreenLightPrimary,
        onTertiary = GreenLightOnPrimary,
        tertiaryContainer = GreenLightPrimaryContainer,
        onTertiaryContainer = GreenLightOnPrimaryContainer,
    )
    Accent.AMBER -> lightColorScheme(
        primary = AmberLightPrimary,
        onPrimary = AmberLightOnPrimary,
        primaryContainer = AmberLightPrimaryContainer,
        onPrimaryContainer = AmberLightOnPrimaryContainer,
        secondary = AmberLightPrimary,
        onSecondary = AmberLightOnPrimary,
        secondaryContainer = AmberLightPrimaryContainer,
        onSecondaryContainer = AmberLightOnPrimaryContainer,
        tertiary = AmberLightPrimary,
        onTertiary = AmberLightOnPrimary,
        tertiaryContainer = AmberLightPrimaryContainer,
        onTertiaryContainer = AmberLightOnPrimaryContainer,
    )
    Accent.VIOLET -> lightColorScheme(
        primary = VioletLightPrimary,
        onPrimary = VioletLightOnPrimary,
        primaryContainer = VioletLightPrimaryContainer,
        onPrimaryContainer = VioletLightOnPrimaryContainer,
        secondary = VioletLightPrimary,
        onSecondary = VioletLightOnPrimary,
        secondaryContainer = VioletLightPrimaryContainer,
        onSecondaryContainer = VioletLightOnPrimaryContainer,
        tertiary = VioletLightPrimary,
        onTertiary = VioletLightOnPrimary,
        tertiaryContainer = VioletLightPrimaryContainer,
        onTertiaryContainer = VioletLightOnPrimaryContainer,
    )
}.withSharedLight()

private fun darkScheme(accent: Accent): ColorScheme = when (accent) {
    Accent.INDIGO -> darkColorScheme(
        primary = IndigoDarkPrimary,
        onPrimary = IndigoDarkOnPrimary,
        primaryContainer = IndigoDarkPrimaryContainer,
        onPrimaryContainer = IndigoDarkOnPrimaryContainer,
        secondary = IndigoDarkPrimary,
        onSecondary = IndigoDarkOnPrimary,
        secondaryContainer = IndigoDarkPrimaryContainer,
        onSecondaryContainer = IndigoDarkOnPrimaryContainer,
        tertiary = IndigoDarkPrimary,
        onTertiary = IndigoDarkOnPrimary,
        tertiaryContainer = IndigoDarkPrimaryContainer,
        onTertiaryContainer = IndigoDarkOnPrimaryContainer,
    )
    Accent.GREEN -> darkColorScheme(
        primary = GreenDarkPrimary,
        onPrimary = GreenDarkOnPrimary,
        primaryContainer = GreenDarkPrimaryContainer,
        onPrimaryContainer = GreenDarkOnPrimaryContainer,
        secondary = GreenDarkPrimary,
        onSecondary = GreenDarkOnPrimary,
        secondaryContainer = GreenDarkPrimaryContainer,
        onSecondaryContainer = GreenDarkOnPrimaryContainer,
        tertiary = GreenDarkPrimary,
        onTertiary = GreenDarkOnPrimary,
        tertiaryContainer = GreenDarkPrimaryContainer,
        onTertiaryContainer = GreenDarkOnPrimaryContainer,
    )
    Accent.AMBER -> darkColorScheme(
        primary = AmberDarkPrimary,
        onPrimary = AmberDarkOnPrimary,
        primaryContainer = AmberDarkPrimaryContainer,
        onPrimaryContainer = AmberDarkOnPrimaryContainer,
        secondary = AmberDarkPrimary,
        onSecondary = AmberDarkOnPrimary,
        secondaryContainer = AmberDarkPrimaryContainer,
        onSecondaryContainer = AmberDarkOnPrimaryContainer,
        tertiary = AmberDarkPrimary,
        onTertiary = AmberDarkOnPrimary,
        tertiaryContainer = AmberDarkPrimaryContainer,
        onTertiaryContainer = AmberDarkOnPrimaryContainer,
    )
    Accent.VIOLET -> darkColorScheme(
        primary = VioletDarkPrimary,
        onPrimary = VioletDarkOnPrimary,
        primaryContainer = VioletDarkPrimaryContainer,
        onPrimaryContainer = VioletDarkOnPrimaryContainer,
        secondary = VioletDarkPrimary,
        onSecondary = VioletDarkOnPrimary,
        secondaryContainer = VioletDarkPrimaryContainer,
        onSecondaryContainer = VioletDarkOnPrimaryContainer,
        tertiary = VioletDarkPrimary,
        onTertiary = VioletDarkOnPrimary,
        tertiaryContainer = VioletDarkPrimaryContainer,
        onTertiaryContainer = VioletDarkOnPrimaryContainer,
    )
}.withSharedDark()

private fun ColorScheme.withSharedLight(): ColorScheme = copy(
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    background = LightBackground,
    onBackground = LightOnSurface,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

private fun ColorScheme.withSharedDark(): ColorScheme = copy(
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

private fun lightBloggoColors() = BloggoColors(
    success = LightSuccess,
    onSuccess = Color(0xFFFFFFFF),
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer,
    warn = LightWarn,
    onWarn = Color(0xFFFFFFFF),
    warnContainer = LightWarnContainer,
    onWarnContainer = LightOnWarnContainer,
)

private fun darkBloggoColors() = BloggoColors(
    success = DarkSuccess,
    onSuccess = Color(0xFF1B1B1F),
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warn = DarkWarn,
    onWarn = Color(0xFF1B1B1F),
    warnContainer = DarkWarnContainer,
    onWarnContainer = DarkOnWarnContainer,
)

@Composable
fun BloggoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.INDIGO,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) darkScheme(accent) else lightScheme(accent)
    val bloggoColors = if (darkTheme) darkBloggoColors() else lightBloggoColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalBloggoColors provides bloggoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BloggoTypography,
            shapes = BloggoShapes,
            content = content,
        )
    }
}
