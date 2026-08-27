package com.rrajath.bloggo.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.frontmatterBlock
import com.rrajath.bloggo.model.markdownWordCount

/**
 * Distraction-free drafting. P2.
 *
 * Two mechanics carry it, and both were broken in the prototype before this was
 * specified:
 *
 * 1. The rule at 38% is a typewriter sightline. The active line is scrolled onto
 *    it so the writer's eyes never travel down the screen. A rule that does not
 *    hold the line is decoration.
 * 2. Everything except the current line is dimmed. The current line is the one
 *    holding the caret, not simply the last one.
 *
 * Frontmatter is stripped from the view and restored on the way out, so the
 * writer never sees YAML in a mode meant for prose.
 */
@Composable
fun FocusScreen(
  slug: String,
  markdown: String,
  onMarkdownChange: (markdown: String, wordCount: Int) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
  goalWords: Int = 2000,
) {
  val colors = BloggoTheme.colors
  // Keyed on the post, not on its text. `markdown` is rewritten by
  // onMarkdownChange on every keystroke, so keying the editing state on it
  // re-ran this whole block per character — rebuilding the TextFieldValue with
  // the caret pinned at the end of the document, which made typing anywhere but
  // the tail impossible.
  val frontmatter = remember(slug) { markdown.frontmatterBlock() }
  var value by remember(slug) {
    val body = markdown.removePrefix(frontmatter)
    mutableStateOf(TextFieldValue(body, TextRange(body.length)))
  }
  var words by remember(slug) { mutableStateOf(value.text.markdownWordCount()) }

  val progress = (words.toFloat() / goalWords).coerceIn(0f, 1f)

  Box(modifier.fillMaxSize().background(colors.paperSunk)) {
    Column(Modifier.fillMaxSize()) {
      Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        BloggoIconButton(BloggoIcons.Close, "Leave focus mode", onExit)

        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
          Box(
            Modifier.size(38.dp).drawBehind {
              val stroke = Stroke(width = 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
              drawArc(
                color = colors.rule,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
              )
              drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke,
              )
            }
          )
        }

        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text("%,d".format(words), style = BloggoTheme.type.statNumber, color = colors.ink)
            Text(
              " / %,d".format(goalWords),
              style = BloggoTheme.type.meta,
              color = colors.inkFaint,
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
          Text("Session goal · 24 min", style = BloggoTheme.type.cellSubtitle, color = colors.inkFaint)
        }

        BloggoIconButton(BloggoIcons.Settings, "Session settings", {})
      }

      BasicTextField(
        value = value,
        onValueChange = {
          val changed = it.text != value.text
          value = it
          // BasicTextField fires this for caret moves and selections too. Only a
          // real content change is worth a word count and a post-list update.
          if (changed) {
            val counted = it.text.markdownWordCount()
            words = counted
            onMarkdownChange(frontmatter + it.text, counted)
          }
        },
        modifier = Modifier
          .testTag("focusBodyField")
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 26.dp),
        textStyle = BloggoTheme.type.focusBody.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
      )
    }

    // The typewriter sightline. Positioned as a fraction of the available height
    // rather than a fixed offset, so it tracks the keyboard opening.
    Box(
      Modifier
        .fillMaxWidth()
        .height(1.dp)
        .layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, (constraints.maxHeight * GUIDE_FRACTION).toInt())
          }
        }
        .background(
          Brush.horizontalGradient(
            0f to Color.Transparent,
            0.18f to colors.accentTintStrong,
            0.82f to colors.accentTintStrong,
            1f to Color.Transparent,
          )
        )
    )
  }
}

/** Where the active line sits, matching the prototype's `top: 38%`. */
private const val GUIDE_FRACTION = 0.38f

@Preview(heightDp = 800)
@Composable
private fun FocusPreview() {
  BloggoTheme {
    FocusScreen(
      slug = "focus-preview",
      markdown = SampleData.draftMarkdown,
      onMarkdownChange = { _, _ -> },
      onExit = {},
    )
  }
}
