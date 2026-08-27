package com.rrajath.bloggo.coverart

import androidx.compose.runtime.Immutable

/**
 * The four inks and the paper a cover is printed on.
 *
 * Values are the design tokens from the prototype. The module keeps its own copy
 * rather than depending on `:designsystem`, so the generator can be dropped into
 * any app, or run from a build script, without dragging a theme along.
 */
@Immutable
data class CoverArtPalette(
  val paper: Int,
  val inks: List<Int>,
  val rule: Int,
  val ruleAlpha: Float,
  val blend: CoverBlend,
) {
  init {
    require(inks.size == INK_COUNT) { "expected $INK_COUNT inks, got ${inks.size}" }
  }

  companion object {
    const val INK_COUNT = 4

    /** `--paper-sunk` with `--accent`, `--mark`, `--amber`, `--add` on top. */
    val Light = CoverArtPalette(
      paper = 0xFFE7E2D7.toInt(),
      inks = listOf(0xFF1D3F63.toInt(), 0xFF8E3B3B.toInt(), 0xFF8A6A24.toInt(), 0xFF3D6B4E.toInt()),
      rule = 0xFF191713.toInt(),
      ruleAlpha = 0.13f,
      blend = CoverBlend.Multiply,
    )

    val Dark = CoverArtPalette(
      paper = 0xFF100F0C.toInt(),
      inks = listOf(0xFF8DB2D8.toInt(), 0xFFC97F76.toInt(), 0xFFC9A44E.toInt(), 0xFF84AE8E.toInt()),
      rule = 0xFFEDE8DC.toInt(),
      ruleAlpha = 0.16f,
      blend = CoverBlend.Screen,
    )

    fun of(dark: Boolean): CoverArtPalette = if (dark) Dark else Light
  }
}

/**
 * Overlapping inks darken on paper and lighten on a dark ground. Matching the
 * prototype's `globalCompositeOperation` swap is what keeps the two themes
 * looking like the same print rather than an inversion.
 */
enum class CoverBlend { Multiply, Screen }
