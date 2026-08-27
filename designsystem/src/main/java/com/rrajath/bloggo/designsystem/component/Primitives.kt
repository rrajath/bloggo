package com.rrajath.bloggo.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIconSize
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/** The states a post can be in, and the only colours a [BloggoChip] may take. */
enum class ChipTone { Draft, Live, PullRequest, Queued, Ghost, Capture }

/**
 * The prototype's status pill.
 *
 * Tone carries the meaning, so a chip is never given a free-form colour. A
 * [dot] is shown for states that describe the post itself (draft, live), not for
 * states that describe an object elsewhere (a PR number, a queue count).
 *
 * [onRemove], when given, adds a trailing "remove" affordance — the prototype's
 * tag chip `×` — at 60% opacity so it reads as secondary to the label, matching
 * `.tag button{opacity:.6}` in `bloggo-prototype.html`.
 */
@Composable
fun BloggoChip(
  label: String,
  tone: ChipTone,
  modifier: Modifier = Modifier,
  icon: BloggoIcon? = null,
  dot: Boolean = tone == ChipTone.Draft || tone == ChipTone.Live,
  onRemove: (() -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  val (background, content) = when (tone) {
    ChipTone.Draft -> colors.amberTint to colors.amber
    ChipTone.Live -> colors.addTint to colors.add
    ChipTone.PullRequest -> colors.accentTint to colors.accent
    ChipTone.Queued -> colors.markTint to colors.mark
    ChipTone.Ghost -> Color.Transparent to colors.inkFaint
    // A distinct shade of blue from PullRequest's accent/accentTint, so an
    // Inbox capture being previewed and an open pull request never read as
    // the same state: the stronger tint plus the brighter accent variant
    // (already used for inline code) rather than a new colour token.
    ChipTone.Capture -> colors.accentTintStrong to colors.accentBright
  }

  Row(
    modifier = modifier
      .clip(CircleShape)
      .background(background)
      .then(
        if (tone == ChipTone.Ghost) Modifier.border(1.dp, colors.rule, CircleShape) else Modifier
      )
      .padding(horizontal = 9.dp, vertical = 3.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (dot) {
      Box(Modifier.size(5.dp).clip(CircleShape).background(content))
    }
    if (icon != null) {
      BloggoIcon(icon, contentDescription = null, size = BloggoIconSize.Tiny, tint = content)
    }
    Text(label, style = BloggoTheme.type.chip, color = content)
    if (onRemove != null) {
      Box(
        modifier = Modifier
          .padding(start = 1.dp)
          .size(16.dp)
          .clip(CircleShape)
          .clickable(role = Role.Button, onClick = onRemove),
        contentAlignment = Alignment.Center,
      ) {
        BloggoIcon(
          BloggoIcons.Close,
          contentDescription = "Remove $label",
          size = BloggoIconSize.Tiny,
          tint = content.copy(alpha = 0.6f),
        )
      }
    }
  }
}

/**
 * Section heading: small caps label, then a rule running to the edge. The rule is
 * what stops a list of sections reading as a wall of text.
 */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 22.dp, bottom = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(9.dp),
  ) {
    Text(
      text.uppercase(),
      style = BloggoTheme.type.eyebrow,
      color = BloggoTheme.colors.inkFaint,
    )
    Box(
      Modifier
        .weight(1f)
        .height(1.dp)
        .background(BloggoTheme.colors.rule)
    )
  }
}

/** Monospaced metadata: dates, word counts, paths, anything machine-ish. */
@Composable
fun MetaText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = BloggoTheme.colors.inkFaint,
  maxLines: Int = 1,
) {
  Text(
    text,
    modifier = modifier,
    style = BloggoTheme.type.meta,
    color = color,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
  )
}

/** [MetaText] for an already-highlighted [AnnotatedString] — a search result's
 * matching line, with the query itself called out, rather than plain text. */
@Composable
fun MetaText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  color: Color = BloggoTheme.colors.inkFaint,
  maxLines: Int = 1,
) {
  Text(
    text,
    modifier = modifier,
    style = BloggoTheme.type.meta,
    color = color,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
  )
}

/**
 * [text] with every case-insensitive occurrence of [query] called out in
 * [BloggoTheme.colors.accent], bold — how a search result's matching line
 * shows *why* it matched instead of just that it did. Falls back to plain
 * [text] wrapped as-is when [query] is blank or matches nothing, so a caller
 * never needs to branch between the two [MetaText] overloads itself.
 */
@Composable
fun highlightedMeta(text: String, query: String): AnnotatedString {
  val accent = BloggoTheme.colors.accent
  if (query.isBlank()) return AnnotatedString(text)

  return buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
      val matchIndex = text.indexOf(query, cursor, ignoreCase = true)
      if (matchIndex < 0) {
        append(text.substring(cursor))
        break
      }
      append(text.substring(cursor, matchIndex))
      withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
        append(text.substring(matchIndex, matchIndex + query.length))
      }
      cursor = matchIndex + query.length
    }
  }
}

enum class BannerTone { Info, Warning }

/**
 * An inline notice with an optional action. Used for the offline queue prompt and
 * for over-limit warnings when composing.
 */
@Composable
fun Banner(
  text: String,
  modifier: Modifier = Modifier,
  tone: BannerTone = BannerTone.Info,
  icon: BloggoIcon = if (tone == BannerTone.Info) BloggoIcons.Info else BloggoIcons.Warning,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  val (background, content) = when (tone) {
    BannerTone.Info -> colors.accentTint to colors.accent
    BannerTone.Warning -> colors.markTint to colors.mark
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(BloggoTheme.shapes.medium)
      .background(background)
      .padding(horizontal = 13.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(11.dp),
  ) {
    BloggoIcon(icon, contentDescription = null, size = BloggoIconSize.Small, tint = content)
    Text(
      text,
      modifier = Modifier.weight(1f),
      style = BloggoTheme.type.cellSubtitle.copy(fontSize = BloggoTheme.type.chip.fontSize),
      color = content,
    )
    if (actionLabel != null && onAction != null) {
      TextAction(actionLabel, content, onAction)
    }
  }
}

/** Three counters in a row, hairline separated. Settings screen header. */
@Composable
fun StatLine(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
  val colors = BloggoTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(BloggoTheme.shapes.medium)
      .background(colors.paperRaised)
      .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.medium),
  ) {
    stats.forEachIndexed { index, (value, label) ->
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(value, style = BloggoTheme.type.statNumber, color = colors.ink)
        Text(
          label.uppercase(),
          style = BloggoTheme.type.eyebrow.copy(fontSize = BloggoTheme.type.eyebrow.fontSize),
          color = colors.inkFaint,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      if (index != stats.lastIndex) {
        Box(Modifier.width(1.dp).height(52.dp).background(colors.ruleSoft))
      }
    }
  }
}

@Preview
@Composable
private fun PrimitivesPreview() {
  BloggoTheme {
    Column(
      Modifier
        .background(BloggoTheme.colors.paper)
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BloggoChip("Draft", ChipTone.Draft)
        BloggoChip("Live", ChipTone.Live)
        BloggoChip("#42", ChipTone.PullRequest, icon = BloggoIcons.Branch)
        BloggoChip("2", ChipTone.Queued)
        BloggoChip("Change", ChipTone.Ghost)
      }
      Eyebrow("Published")
      MetaText("Aug 4 · 840 words")
      Banner("2 posts waiting in the offline queue", actionLabel = "Push", onAction = {})
      Banner("Too long for hachyderm.io", tone = BannerTone.Warning)
      StatLine(listOf("38" to "Posts", "2" to "Drafts", "1" to "Open PR"))
    }
  }
}

@Composable
private fun TextAction(label: String, color: Color, onClick: () -> Unit) {
  Text(
    label,
    style = BloggoTheme.type.chip,
    color = color,
    modifier = Modifier
      .clip(BloggoTheme.shapes.small)
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}
