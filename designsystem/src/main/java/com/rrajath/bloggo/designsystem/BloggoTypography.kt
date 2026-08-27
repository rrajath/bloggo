package com.rrajath.bloggo.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Four families, each with one job. Mixing them up is the fastest way to stop
 * looking like the prototype.
 *
 * - [Display] Instrument Serif: titles, and only titles.
 * - [Reading] Newsreader: body prose, in the editor's read mode and post rows.
 * - [Ui] Archivo: labels, buttons, chips, navigation.
 * - [Mono] IBM Plex Mono: metadata, paths, diffs, markdown source.
 */
object BloggoFonts {

  /**
   * Archivo and Newsreader ship as variable fonts, so one file covers every
   * weight. [FontVariation] is still marked experimental in Compose text; the
   * alternative is nine more static font files in the APK.
   */
  @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
  private fun variable(resId: Int, weight: FontWeight, style: FontStyle = FontStyle.Normal) =
    Font(
      resId = resId,
      weight = weight,
      style = style,
      variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

  val Display = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
  )

  val Reading = FontFamily(
    variable(R.font.newsreader_variable, FontWeight.Normal),
    variable(R.font.newsreader_variable, FontWeight.Medium),
    variable(R.font.newsreader_variable, FontWeight.SemiBold),
    variable(R.font.newsreader_italic_variable, FontWeight.Normal, FontStyle.Italic),
  )

  val Ui = FontFamily(
    variable(R.font.archivo_variable, FontWeight.Normal),
    variable(R.font.archivo_variable, FontWeight.Medium),
    variable(R.font.archivo_variable, FontWeight.SemiBold),
    variable(R.font.archivo_variable, FontWeight.Bold),
  )

  val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_mono_italic, FontWeight.Normal, FontStyle.Italic),
  )
}

/**
 * Named styles lifted from the prototype's CSS. Sizes are the CSS pixel values
 * read as sp, which lines up because the prototype frame is 393 CSS px wide and
 * a phone is 360 to 412 dp wide.
 *
 * Line heights are set explicitly with [LineHeightStyle.Trim.None] so that
 * Compose does not add the half leading that CSS does not have.
 */
@Immutable
data class BloggoTypography(
  /** Rail brand, 34/1.0. Not used in app chrome. */
  val displayLarge: TextStyle,
  /** Screen titles in the app bar, 27/1.1. */
  val displayMedium: TextStyle,
  /** Hero card title, 29/1.12. */
  val displayHero: TextStyle,
  /** Sheet titles, 23/1.15. */
  val displaySmall: TextStyle,
  /** Rendered article h1, 36/1.08. */
  val articleTitle: TextStyle,
  /** Rendered article h2, 25/1.18. */
  val articleHeading: TextStyle,
  /** Rendered article h3, 18/600. */
  val articleSubheading: TextStyle,
  /** Rendered article body, 17.5/1.66. */
  val articleBody: TextStyle,
  /** Pull quote, 18.5/1.5 italic. */
  val articleQuote: TextStyle,
  /** Post row title, 17/1.28 medium. */
  val rowTitle: TextStyle,
  /** Inbox capture text, 15.5/1.5. */
  val captureBody: TextStyle,
  /** Focus mode text, 19/1.78. */
  val focusBody: TextStyle,
  /** Default UI text, 16/1.5. */
  val body: TextStyle,
  /** Cell titles, 14/1.3 medium. */
  val cellTitle: TextStyle,
  /** Cell subtitles, 11.5/1.35. */
  val cellSubtitle: TextStyle,
  /** Buttons, 14.5/600. */
  val button: TextStyle,
  /** Chips, 10.5/600. */
  val chip: TextStyle,
  /** Section eyebrows, 9.5/700 with .17em tracking. */
  val eyebrow: TextStyle,
  /** Field labels, 9.5/700 with .16em tracking. */
  val fieldLabel: TextStyle,
  /** Tab bar labels, 10/600. */
  val tabLabel: TextStyle,
  /** Metadata, 11 mono. */
  val meta: TextStyle,
  /** App bar subtitles, 10.5 mono. */
  val metaSmall: TextStyle,
  /** Markdown source in the editor, 14/1.72 mono. */
  val editorSource: TextStyle,
  /** Diff rows, 11.5/1.62 mono. */
  val diff: TextStyle,
  /** Mono form values, 13. */
  val monoField: TextStyle,
  /** Big numbers in the stat line, 17 mono. */
  val statNumber: TextStyle,
) {
  companion object {
    private val trim = LineHeightStyle(
      alignment = LineHeightStyle.Alignment.Center,
      trim = LineHeightStyle.Trim.None,
    )

    private fun style(
      family: FontFamily,
      size: Double,
      lineHeight: Double,
      weight: FontWeight = FontWeight.Normal,
      tracking: Double = 0.0,
      italic: Boolean = false,
    ) = TextStyle(
      fontFamily = family,
      fontSize = size.sp,
      lineHeight = lineHeight.sp,
      fontWeight = weight,
      letterSpacing = tracking.sp,
      fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
      lineHeightStyle = trim,
    )

    val Default = BloggoTypography(
      displayLarge = style(BloggoFonts.Display, 34.0, 34.0, tracking = -0.34),
      displayMedium = style(BloggoFonts.Display, 27.0, 29.7, tracking = -0.14),
      displayHero = style(BloggoFonts.Display, 29.0, 32.5, tracking = -0.29),
      displaySmall = style(BloggoFonts.Display, 23.0, 26.5),
      articleTitle = style(BloggoFonts.Display, 36.0, 38.9, tracking = -0.54),
      articleHeading = style(BloggoFonts.Display, 25.0, 29.5, tracking = -0.25),
      articleSubheading = style(BloggoFonts.Reading, 18.0, 25.0, FontWeight.SemiBold),
      articleBody = style(BloggoFonts.Reading, 17.5, 29.0),
      articleQuote = style(BloggoFonts.Reading, 18.5, 27.8, italic = true),
      rowTitle = style(BloggoFonts.Reading, 17.0, 21.8, FontWeight.Medium, tracking = -0.09),
      captureBody = style(BloggoFonts.Reading, 15.5, 23.3),
      focusBody = style(BloggoFonts.Reading, 19.0, 33.8),
      body = style(BloggoFonts.Ui, 16.0, 24.0),
      cellTitle = style(BloggoFonts.Ui, 14.0, 18.2, FontWeight.Medium),
      cellSubtitle = style(BloggoFonts.Ui, 11.5, 15.5),
      button = style(BloggoFonts.Ui, 14.5, 20.0, FontWeight.SemiBold),
      chip = style(BloggoFonts.Ui, 10.5, 14.0, FontWeight.SemiBold, tracking = 0.32),
      eyebrow = style(BloggoFonts.Ui, 9.5, 13.0, FontWeight.Bold, tracking = 1.62),
      fieldLabel = style(BloggoFonts.Ui, 9.5, 13.0, FontWeight.Bold, tracking = 1.52),
      tabLabel = style(BloggoFonts.Ui, 10.0, 13.0, FontWeight.SemiBold, tracking = 0.15),
      meta = style(BloggoFonts.Mono, 11.0, 15.0, tracking = 0.11),
      metaSmall = style(BloggoFonts.Mono, 10.5, 14.0, tracking = 0.21),
      editorSource = style(BloggoFonts.Mono, 14.0, 24.1, tracking = -0.07),
      diff = style(BloggoFonts.Mono, 11.5, 18.6),
      monoField = style(BloggoFonts.Mono, 13.0, 18.0),
      statNumber = style(BloggoFonts.Mono, 17.0, 18.7, FontWeight.Medium),
    )
  }
}

val LocalBloggoTypography = staticCompositionLocalOf { BloggoTypography.Default }
