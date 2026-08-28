package com.rrajath.bloggo.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIconSize
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/**
 * The in-progress card at the top of the library.
 *
 * Typography carries the hierarchy: a 3 dp amber top rule marks the card as the
 * primary object and the status chip sits inline above the title.
 */
@Composable
fun HeroCard(
  title: String,
  meta: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  chipLabel: String = "Draft",
  chipTone: ChipTone = ChipTone.Draft,
) {
  val colors = BloggoTheme.colors
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(BloggoTheme.shapes.large)
      .background(colors.paperRaised)
      .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.large)
      .clickable(role = Role.Button, onClick = onClick),
  ) {
    Box(Modifier.fillMaxWidth().height(3.dp).background(colors.amber))
    Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
      BloggoChip(chipLabel, chipTone, modifier = Modifier.padding(bottom = 12.dp))
      Text(title, style = BloggoTheme.type.displayHero, color = colors.ink)
      MetaText(meta, modifier = Modifier.padding(top = 9.dp))
    }
  }
}

/**
 * A post in a list.
 *
 * [onOpenLive] adds the trailing button that goes straight to the published page,
 * and is only meaningful for posts that are actually live.
 */
@Composable
fun PostRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  chip: (@Composable () -> Unit)? = null,
  meta: String? = null,
  metaHighlightQuery: String? = null,
  onOpenLive: (() -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 15.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        title,
        style = BloggoTheme.type.rowTitle,
        color = colors.ink,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        modifier = Modifier.padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        chip?.invoke()
        if (meta != null) {
          if (metaHighlightQuery != null) {
            // Search results need more than one line so the highlighted match
            // (which can land a line or two into the snippet) is actually
            // visible, rather than being clipped by the single-line default.
            MetaText(highlightedMeta(meta, metaHighlightQuery), maxLines = 3)
          } else {
            MetaText(meta)
          }
        }
      }
    }

    if (onOpenLive != null) {
      BloggoIconButton(
        icon = BloggoIcons.ExternalLink,
        contentDescription = "Open the live page",
        onClick = onOpenLive,
        modifier = Modifier.size(34.dp),
        tint = colors.inkFaint,
      )
    }
  }
}

/** A captured fragment in the inbox, marked by an oversized opening quote. */
@Composable
fun CaptureRow(
  text: String,
  time: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tag: String? = null,
) {
  val colors = BloggoTheme.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 15.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      "“",
      style = BloggoTheme.type.displaySmall,
      color = colors.rule,
      modifier = Modifier.size(width = 16.dp, height = 26.dp),
    )
    Column(Modifier.weight(1f)) {
      Text(text, style = BloggoTheme.type.captureBody, color = colors.ink)
      Row(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MetaText(time)
        if (tag != null) BloggoChip(tag, ChipTone.Ghost)
      }
    }
  }
}

/** Grouped settings rows with a shared card background and hairline separators. */
@Composable
fun CellGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val colors = BloggoTheme.colors
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(BloggoTheme.shapes.medium)
      .background(colors.paperRaised)
      .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.medium),
    content = content,
  )
}

@Composable
fun Cell(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  subtitleMono: Boolean = false,
  icon: BloggoIcon? = null,
  iconTint: Color = BloggoTheme.colors.accent,
  iconBackground: Color = BloggoTheme.colors.accentTint,
  showDivider: Boolean = true,
  onClick: (() -> Unit)? = null,
  trailing: (@Composable () -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  Column {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(horizontal = 15.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (icon != null) {
        Box(
          Modifier
            .size(32.dp)
            .clip(BloggoTheme.shapes.small)
            .background(iconBackground),
          contentAlignment = Alignment.Center,
        ) {
          BloggoIcon(icon, contentDescription = null, size = BloggoIconSize.Small, tint = iconTint)
        }
      }
      Column(Modifier.weight(1f)) {
        Text(title, style = BloggoTheme.type.cellTitle, color = colors.ink)
        if (subtitle != null) {
          Text(
            subtitle,
            style = if (subtitleMono) BloggoTheme.type.meta else BloggoTheme.type.cellSubtitle,
            color = colors.inkFaint,
            modifier = Modifier.padding(top = 2.dp),
          )
        }
      }
      trailing?.invoke()
    }
    if (showDivider) {
      Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
    }
  }
}

@Preview(heightDp = 640)
@Composable
private fun RowsPreview() {
  BloggoTheme {
    Column(Modifier.background(BloggoTheme.colors.paper).padding(18.dp)) {
      HeroCard(
        title = "On agents that actually ship",
        meta = "1,204 words · edited 9m ago",
        onClick = {},
      )
      Eyebrow("Published")
      PostRow(
        title = "Why I left Obsidian for plain files",
        onClick = {},
        chip = { BloggoChip("Live", ChipTone.Live) },
        meta = "Aug 4 · 840 words",
        onOpenLive = {},
      )
      CaptureRow(
        text = "The difference between a tool that respects your files and one that quietly owns them.",
        time = "08:12",
        tag = "promote to post",
        onClick = {},
      )
      CellGroup(Modifier.padding(top = 12.dp)) {
        Cell(
          title = "Hugo",
          subtitle = "hugo.toml · archetypes/default.md",
          subtitleMono = true,
          icon = BloggoIcons.Framework,
          onClick = {},
          trailing = { BloggoChip("Change", ChipTone.Ghost) },
        )
        Cell(
          title = "Post path",
          subtitle = "content/posts/{slug}.md",
          subtitleMono = true,
          icon = BloggoIcons.File,
          showDivider = false,
        )
      }
    }
  }
}

