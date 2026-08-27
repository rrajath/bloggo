package com.rrajath.bloggo.designsystem.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.rrajath.bloggo.designsystem.BloggoColors
import com.rrajath.bloggo.designsystem.BloggoTheme

/**
 * Styles markdown source in place, the way the prototype's editor does.
 *
 * Uses [OffsetMapping.Identity] and can only do so because [MarkdownHighlighter]
 * never changes the character count. If someone later makes markers collapse to
 * zero width, this needs a real mapping and a great many more tests.
 */
class MarkdownTransformation(private val colors: BloggoColors) : VisualTransformation {

  // A one-entry memo of the last source styled. filter() is called for every
  // layout of the field, not only for every edit, and re-running the highlighter
  // is a full pass over the document each time. Only ever touched from the
  // composition thread, so no synchronisation.
  private var lastSource: String? = null
  private var lastStyled: AnnotatedString? = null

  override fun filter(text: AnnotatedString): TransformedText {
    val source = text.text
    val memoised = lastStyled
    val styled = if (memoised != null && lastSource == source) {
      memoised
    } else {
      buildAnnotatedString(source, colors).also {
        lastSource = source
        lastStyled = it
      }
    }
    return TransformedText(styled, OffsetMapping.Identity)
  }

  override fun equals(other: Any?): Boolean = other is MarkdownTransformation && other.colors == colors

  override fun hashCode(): Int = colors.hashCode()

  companion object {
    fun buildAnnotatedString(source: String, colors: BloggoColors): AnnotatedString =
      AnnotatedString.Builder(source).apply {
        MarkdownHighlighter.spans(source).forEach { span ->
          addStyle(styleFor(span.role, colors), span.start, span.end.coerceAtMost(source.length))
        }
      }.toAnnotatedString()

    private fun styleFor(role: MarkdownRole, colors: BloggoColors): SpanStyle = when (role) {
      MarkdownRole.Marker -> SpanStyle(color = colors.inkFaint.copy(alpha = 0.62f))
      MarkdownRole.Heading1 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.ink)
      MarkdownRole.Heading2 -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, color = colors.ink)
      MarkdownRole.Bold -> SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.ink)
      MarkdownRole.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
      MarkdownRole.Code -> SpanStyle(color = colors.accentBright, background = colors.accentTint)
      MarkdownRole.LinkText -> SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline)
      MarkdownRole.LinkUrl -> SpanStyle(color = colors.inkFaint, fontSize = 12.5.sp)
      MarkdownRole.Quote -> SpanStyle(color = colors.inkMuted, fontStyle = FontStyle.Italic)
      MarkdownRole.Bullet -> SpanStyle(color = colors.accent, fontWeight = FontWeight.SemiBold)
      MarkdownRole.Frontmatter -> SpanStyle(color = colors.inkMuted, fontSize = 12.5.sp)
      MarkdownRole.FrontmatterKey -> SpanStyle(color = colors.accent, fontWeight = FontWeight.Medium)
      MarkdownRole.Fence -> SpanStyle(color = colors.inkFaint, fontSize = 11.5.sp, letterSpacing = 1.6.sp)
      MarkdownRole.FenceContent -> SpanStyle(color = colors.inkMuted, fontSize = 12.5.sp)
      MarkdownRole.Shortcode -> SpanStyle(color = colors.accent, fontSize = 12.5.sp)
    }
  }
}

@Composable
fun rememberMarkdownTransformation(): MarkdownTransformation {
  val colors = BloggoTheme.colors
  return remember(colors) { MarkdownTransformation(colors) }
}
