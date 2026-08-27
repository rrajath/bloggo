package com.rrajath.bloggo.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** `--r-s`, `--r-m`, `--r-l` plus the shapes the prototype hard codes. */
@Immutable
data class BloggoShapes(
  val small: RoundedCornerShape = RoundedCornerShape(8.dp),
  val medium: RoundedCornerShape = RoundedCornerShape(14.dp),
  val large: RoundedCornerShape = RoundedCornerShape(22.dp),
  val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
  val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
  val thumbnail: RoundedCornerShape = RoundedCornerShape(10.dp),
  val button: RoundedCornerShape = RoundedCornerShape(14.dp),
  val field: RoundedCornerShape = RoundedCornerShape(11.dp),
)

/** The prototype's padding rhythm. Screen gutters are 18, cards inset 14 to 18. */
@Immutable
data class BloggoSpacing(
  val gutter: Dp = 18.dp,
  val xs: Dp = 4.dp,
  val sm: Dp = 8.dp,
  val md: Dp = 14.dp,
  val lg: Dp = 22.dp,
  val xl: Dp = 38.dp,
  /** Vertical padding inside a list row. */
  val rowVertical: Dp = 15.dp,
  /** Minimum touch target, above the prototype's visual size where needed. */
  val touchTarget: Dp = 48.dp,
)

val LocalBloggoShapes = staticCompositionLocalOf { BloggoShapes() }
val LocalBloggoSpacing = staticCompositionLocalOf { BloggoSpacing() }

/**
 * The single entry point for the app's look.
 *
 * A Material scheme is derived from the tokens so that Material 3 components
 * (modal sheets, ripples, text selection handles) do not show up in Material
 * purple. App code should still read colours from [BloggoTheme.colors] rather
 * than from `MaterialTheme.colorScheme`.
 */
@Composable
fun BloggoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colors = BloggoColors.of(darkTheme)

  val material = if (darkTheme) {
    darkColorScheme(
      primary = colors.accent,
      onPrimary = colors.paper,
      background = colors.paper,
      onBackground = colors.ink,
      surface = colors.paperRaised,
      onSurface = colors.ink,
      surfaceContainerLow = colors.paperRaised,
      surfaceContainer = colors.paperRaised,
      surfaceContainerHigh = colors.paperRaised,
      outline = colors.rule,
      outlineVariant = colors.ruleSoft,
      error = colors.mark,
    )
  } else {
    lightColorScheme(
      primary = colors.accent,
      onPrimary = colors.paper,
      background = colors.paper,
      onBackground = colors.ink,
      surface = colors.paperRaised,
      onSurface = colors.ink,
      surfaceContainerLow = colors.paperRaised,
      surfaceContainer = colors.paperRaised,
      surfaceContainerHigh = colors.paperRaised,
      outline = colors.rule,
      outlineVariant = colors.ruleSoft,
      error = colors.mark,
    )
  }

  CompositionLocalProvider(
    LocalBloggoColors provides colors,
    LocalBloggoTypography provides BloggoTypography.Default,
    LocalBloggoShapes provides BloggoShapes(),
    LocalBloggoSpacing provides BloggoSpacing(),
    LocalContentColor provides colors.ink,
  ) {
    MaterialTheme(colorScheme = material, content = content)
  }
}

/** Token accessors, so screens read `BloggoTheme.colors.ink` and never a raw hex. */
object BloggoTheme {
  val colors: BloggoColors
    @Composable @ReadOnlyComposable get() = LocalBloggoColors.current

  val type: BloggoTypography
    @Composable @ReadOnlyComposable get() = LocalBloggoTypography.current

  val shapes: BloggoShapes
    @Composable @ReadOnlyComposable get() = LocalBloggoShapes.current

  val spacing: BloggoSpacing
    @Composable @ReadOnlyComposable get() = LocalBloggoSpacing.current
}
