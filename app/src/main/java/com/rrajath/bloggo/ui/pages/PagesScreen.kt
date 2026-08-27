package com.rrajath.bloggo.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.Cell
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.Post

/**
 * The top-level `.md` files directly under `content/` a Hugo site actually has (`about.md`,
 * `uses.md`, and so on) — as distinct from `content/posts/`. Rows open the
 * same Editor and Preview screens a post does: a page that's already been
 * pushed opens straight into Preview, one that only exists locally opens
 * into Editor — the same split a post's Draft/Published state drives, just
 * keyed on [Post.repoPath] instead, since a page has no draft flag of its
 * own. See docs/PROTOTYPE_NOTES.md's "Pages replaced Media".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
  pages: List<Post>,
  onOpenPage: (Post) -> Unit,
  onOpenLive: (Post) -> Unit,
  onNewPage: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  isRefreshing: Boolean = false,
) {
  val colors = BloggoTheme.colors

  Column(modifier.fillMaxSize()) {
    BloggoAppBar(
      title = "Pages",
      subtitle = "content/ · ${pages.size} page${if (pages.size == 1) "" else "s"}",
      actions = { BloggoIconButton(BloggoIcons.Refresh, "Refresh from GitHub", onRefresh) },
    )

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
        item {
          Row(
            Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp)
              .clip(BloggoTheme.shapes.medium)
              .border(1.5.dp, colors.rule, BloggoTheme.shapes.medium)
              .clickable(onClick = onNewPage)
              .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            BloggoIcon(BloggoIcons.Plus, contentDescription = null, tint = colors.inkFaint)
            Text(
              "New page",
              style = BloggoTheme.type.cellTitle,
              color = colors.inkFaint,
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }

        if (pages.isEmpty()) {
          item { PagesEmptyState() }
        } else {
          item { Eyebrow("Top-level pages") }
          items(pages, key = { it.slug }) { page ->
            Cell(
              title = page.title,
              subtitle = "content/${page.slug}.md" +
                (page.date?.let { " · $it" } ?: page.editedAgo?.let { " · edited $it" } ?: ""),
              icon = BloggoIcons.File,
              onClick = { onOpenPage(page) },
              showDivider = page.slug != pages.last().slug,
              trailing = if (page.repoPath != null) {
                {
                  BloggoIconButton(
                    icon = BloggoIcons.ExternalLink,
                    contentDescription = "Open the live page",
                    onClick = { onOpenLive(page) },
                    tint = colors.inkFaint,
                  )
                }
              } else {
                null
              },
            )
          }
        }

        item { Column(Modifier.padding(bottom = 38.dp)) {} }
      }
    }
  }
}

@Composable
private fun PagesEmptyState(modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text("No pages yet", style = BloggoTheme.type.cellTitle, color = BloggoTheme.colors.ink)
    Text(
      "Top-level content/*.md files will show up here once you connect a repo",
      style = BloggoTheme.type.meta,
      color = BloggoTheme.colors.inkFaint,
      modifier = Modifier.padding(top = 5.dp),
    )
  }
}

@androidx.compose.ui.tooling.preview.Preview(heightDp = 800)
@Composable
private fun PagesPreview() {
  BloggoTheme {
    PagesScreen(
      pages = SampleData.pages,
      onOpenPage = {},
      onOpenLive = {},
      onNewPage = {},
      onRefresh = {},
    )
  }
}
