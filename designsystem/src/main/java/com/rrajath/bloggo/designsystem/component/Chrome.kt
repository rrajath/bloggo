package com.rrajath.bloggo.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Screen header: a serif title, an optional monospaced subtitle carrying repo
 * state, and trailing actions.
 *
 * The subtitle is where the app admits what it is connected to. Dropping it makes
 * the screens look cleaner and tells the writer nothing.
 */
@Composable
fun BloggoAppBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(BloggoTheme.colors.paper)
      .padding(start = if (onBack != null) 4.dp else 18.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (onBack != null) {
      BloggoIconButton(BloggoIcons.ChevronLeft, "Back", onBack)
    }
    Column(Modifier.weight(1f)) {
      Text(
        title,
        style = BloggoTheme.type.displayMedium,
        color = BloggoTheme.colors.ink,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle != null) {
        Text(
          subtitle,
          style = BloggoTheme.type.metaSmall,
          color = BloggoTheme.colors.inkFaint,
          modifier = Modifier.padding(top = 1.dp),
        )
      }
    }
    actions()
  }
}

/** One destination in the bottom bar. */
data class BloggoTab(val icon: BloggoIcon, val label: String, val route: String)

/**
 * Five slots with the compose action raised in the middle.
 *
 * The compose button is deliberately not a tab: it does not have a selected
 * state, because it starts a new post rather than navigating anywhere.
 */
@Composable
fun BloggoTabBar(
  tabs: List<BloggoTab>,
  selectedRoute: String,
  onSelect: (String) -> Unit,
  onCompose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  require(tabs.size == 4) { "the tab bar holds four tabs around the compose action" }
  val colors = BloggoTheme.colors

  Column(modifier.background(colors.paper)) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.take(2).forEach { TabItem(it, it.route == selectedRoute, { onSelect(it.route) }, Modifier.weight(1f)) }

      Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(colors.ink)
            .clickable(role = Role.Button, onClick = onCompose),
          contentAlignment = Alignment.Center,
        ) {
          BloggoIcon(BloggoIcons.Pen, "New post", size = 21.dp, tint = colors.paper)
        }
      }

      tabs.drop(2).forEach { TabItem(it, it.route == selectedRoute, { onSelect(it.route) }, Modifier.weight(1f)) }
    }
  }
}

@Composable
private fun TabItem(tab: BloggoTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
  val colors = BloggoTheme.colors
  Column(
    modifier = modifier
      .clip(BloggoTheme.shapes.small)
      .clickable(role = Role.Tab, onClick = onClick)
      .padding(vertical = 5.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    BloggoIcon(
      tab.icon,
      contentDescription = null,
      tint = if (selected) colors.accent else colors.inkFaint,
    )
    Text(
      tab.label,
      style = BloggoTheme.type.tabLabel,
      color = if (selected) colors.ink else colors.inkFaint,
    )
  }
}

/**
 * The permalink strip under the preview header.
 *
 * Shows the URL in both states. A draft's URL is predictable before it exists, and
 * seeing it is useful even when the button is inert, so this never hides.
 */
@Composable
fun LinkBar(
  url: String,
  published: Boolean,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  Column(modifier.fillMaxWidth().background(colors.paperRaised)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      BloggoIcon(
        BloggoIcons.Globe,
        contentDescription = null,
        size = BloggoIconSize.Small,
        tint = colors.inkFaint,
      )
      Text(
        url,
        modifier = Modifier.weight(1f),
        style = BloggoTheme.type.meta,
        color = if (published) colors.inkMuted else colors.inkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Box(
        Modifier
          .clip(CircleShape)
          .background(if (published) colors.accentTint else Color.Transparent)
          .clickable(enabled = published, role = Role.Button, onClick = onOpen)
          .padding(horizontal = 11.dp, vertical = 6.dp)
      ) {
        Text(
          if (published) "Open" else "Not published",
          style = BloggoTheme.type.chip,
          color = if (published) colors.accent else colors.inkFaint,
        )
      }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
  }
}

/** Transient confirmation. Never used for anything the writer must act on. */
@Composable
fun BloggoToast(message: String?, modifier: Modifier = Modifier) {
  AnimatedVisibility(
    visible = message != null,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier,
  ) {
    Box(
      Modifier
        .clip(CircleShape)
        .background(BloggoTheme.colors.ink)
        .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
      Text(
        message.orEmpty(),
        style = BloggoTheme.type.cellTitle,
        color = BloggoTheme.colors.paper,
      )
    }
  }
}

@Preview
@Composable
private fun ChromePreview() {
  BloggoTheme {
    Column(Modifier.background(BloggoTheme.colors.paper)) {
      BloggoAppBar(
        title = "Library",
        subtitle = "rrajath/blog · main · hugo",
        actions = { BloggoIconButton(BloggoIcons.Search, "Search", {}) },
      )
      LinkBar("rrajath.dev/posts/on-agents-that-actually-ship", false, {})
      Box(Modifier.padding(18.dp)) { BloggoToast("Queued. It will push when you reconnect.") }
      BloggoTabBar(
        tabs = listOf(
          BloggoTab(BloggoIcons.Library, "Library", "library"),
          BloggoTab(BloggoIcons.Inbox, "Inbox", "inbox"),
          BloggoTab(BloggoIcons.MediaImage, "Media", "media"),
          BloggoTab(BloggoIcons.Settings, "Settings", "settings"),
        ),
        selectedRoute = "library",
        onSelect = {},
        onCompose = {},
      )
    }
  }
}
