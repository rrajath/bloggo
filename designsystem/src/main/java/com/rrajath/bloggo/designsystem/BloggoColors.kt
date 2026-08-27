package com.rrajath.bloggo.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The prototype's `:root` custom properties, one to one.
 *
 * These are deliberately *not* Material 3 role names. The prototype is built on
 * paper and ink, not primary and surface, and renaming the tokens on the way
 * across is how a design stops matching its reference. [BloggoTheme] does derive
 * a Material scheme from these so that Material components inherit sane colours,
 * but app code should reach for these names.
 */
@Immutable
data class BloggoColors(
  /** Page ground. `--paper` */
  val paper: Color,
  /** Raised cards and sheets. `--paper-2` */
  val paperRaised: Color,
  /** Recessed wells, code blocks. `--paper-3` */
  val paperSunk: Color,
  /** Behind generated art. `--paper-sunk` */
  val paperArt: Color,

  /** Primary text. `--ink` */
  val ink: Color,
  /** Secondary text. `--ink-2` */
  val inkMuted: Color,
  /** Tertiary text, metadata. `--ink-3` */
  val inkFaint: Color,

  /** Dividers. `--rule` */
  val rule: Color,
  /** Hairline dividers inside cards. `--rule-soft` */
  val ruleSoft: Color,

  /** Links, active state, the first ink. `--accent` */
  val accent: Color,
  /** Inline code. `--accent-2` */
  val accentBright: Color,
  /** `--accent-tint` */
  val accentTint: Color,
  /** `--accent-tint-2` */
  val accentTintStrong: Color,

  /** Deletions, quotes, warnings. `--mark` */
  val mark: Color,
  val markTint: Color,
  /** Additions, published state. `--add` */
  val add: Color,
  val addTint: Color,
  /** Drafts, pending state. `--amber` */
  val amber: Color,
  val amberTint: Color,

  /**
   * Writing-analysis washes, scoped to the Readability review screen only. The
   * one documented exception to "colour carries state": five simultaneous
   * highlight categories that describe prose quality, always shown with a
   * printed legend. The `*Ink` value is the solid legend swatch; the paired
   * wash (17 to 26% alpha) is the text highlight. `--an-*` in the prototype.
   */
  val analysisHard: Color,
  val analysisHardInk: Color,
  val analysisVeryHard: Color,
  val analysisVeryHardInk: Color,
  val analysisComplex: Color,
  val analysisComplexInk: Color,
  val analysisAdverb: Color,
  val analysisAdverbInk: Color,
  val analysisPassive: Color,
  val analysisPassiveInk: Color,

  /** True when this is the dark palette, for blend mode decisions. */
  val isDark: Boolean,
) {
  companion object {
    val Light = BloggoColors(
      paper = Color(0xFFEDE9E0),
      paperRaised = Color(0xFFF6F3EC),
      paperSunk = Color(0xFFE4DFD4),
      paperArt = Color(0xFFE7E2D7),
      ink = Color(0xFF191713),
      inkMuted = Color(0xFF5C574D),
      inkFaint = Color(0xFF8B8477),
      rule = Color(0xFFD6CFC1),
      ruleSoft = Color(0xFFE3DDD1),
      accent = Color(0xFF1D3F63),
      accentBright = Color(0xFF2F5B87),
      accentTint = Color(0x1A1D3F63),
      accentTintStrong = Color(0x2E1D3F63),
      mark = Color(0xFF8E3B3B),
      markTint = Color(0x1A8E3B3B),
      add = Color(0xFF3D6B4E),
      addTint = Color(0x1A3D6B4E),
      amber = Color(0xFF8A6A24),
      amberTint = Color(0x1F8A6A24),
      analysisHard = Color(0x33B38E2C),
      analysisHardInk = Color(0xFF8A6A24),
      analysisVeryHard = Color(0x3D964A3A),
      analysisVeryHardInk = Color(0xFF8E3B3B),
      analysisComplex = Color(0x387A5C96),
      analysisComplexInk = Color(0xFF6A4E86),
      analysisAdverb = Color(0x2B2D5C8C),
      analysisAdverbInk = Color(0xFF2F5B87),
      analysisPassive = Color(0x333D6B4E),
      analysisPassiveInk = Color(0xFF3D6B4E),
      isDark = false,
    )

    val Dark = BloggoColors(
      paper = Color(0xFF14130F),
      paperRaised = Color(0xFF1C1A15),
      paperSunk = Color(0xFF0F0E0B),
      paperArt = Color(0xFF100F0C),
      ink = Color(0xFFEDE8DC),
      inkMuted = Color(0xFF9C9484),
      inkFaint = Color(0xFF6E6759),
      rule = Color(0xFF2E2A22),
      ruleSoft = Color(0xFF241F19),
      accent = Color(0xFF8DB2D8),
      accentBright = Color(0xFFA9C6E4),
      accentTint = Color(0x218DB2D8),
      accentTintStrong = Color(0x388DB2D8),
      mark = Color(0xFFC97F76),
      markTint = Color(0x24C97F76),
      add = Color(0xFF84AE8E),
      addTint = Color(0x2484AE8E),
      amber = Color(0xFFC9A44E),
      amberTint = Color(0x24C9A44E),
      analysisHard = Color(0x3DC9A44E),
      analysisHardInk = Color(0xFFC9A44E),
      analysisVeryHard = Color(0x42C97F76),
      analysisVeryHardInk = Color(0xFFC97F76),
      analysisComplex = Color(0x3DB29ED6),
      analysisComplexInk = Color(0xFFB29ED6),
      analysisAdverb = Color(0x388DB2D8),
      analysisAdverbInk = Color(0xFF8DB2D8),
      analysisPassive = Color(0x3D84AE8E),
      analysisPassiveInk = Color(0xFF84AE8E),
      isDark = true,
    )

    fun of(dark: Boolean): BloggoColors = if (dark) Dark else Light
  }
}

val LocalBloggoColors = staticCompositionLocalOf { BloggoColors.Light }
