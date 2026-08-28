package com.rrajath.bloggo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.github.ConnectionCheck
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.Cell
import com.rrajath.bloggo.designsystem.component.CellGroup
import com.rrajath.bloggo.designsystem.component.StatLine
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/** The six Settings sub-pages, each a navigable row on the [SettingsScreen] menu. */
enum class SettingsPage { Connection, Repo, Publishing, Appearance, Readability, ImportExport }

private data class SettingsMenuRow(
  val page: SettingsPage,
  val title: String,
  val subtitle: String,
  val icon: BloggoIcon,
)

private val settingsMenuRows = listOf(
  SettingsMenuRow(
    SettingsPage.Connection, "GitHub Connection",
    "Repository, branch, and access token", BloggoIcons.Branch,
  ),
  SettingsMenuRow(
    SettingsPage.Repo, "Repo Settings",
    "Post and image paths, frontmatter", BloggoIcons.Framework,
  ),
  SettingsMenuRow(
    SettingsPage.Publishing, "Publishing",
    "What happens when you publish a post", BloggoIcons.Commit,
  ),
  SettingsMenuRow(
    SettingsPage.Appearance, "Appearance",
    "Light, dark, or match your device", BloggoIcons.Sun,
  ),
  SettingsMenuRow(
    SettingsPage.Readability, "Readability Review",
    "Which checks run on your drafts", BloggoIcons.Readability,
  ),
  SettingsMenuRow(
    SettingsPage.ImportExport, "Import / Export",
    "Back up your settings to a file", BloggoIcons.Download,
  ),
)

/**
 * The Settings tab: a menu of the six sub-pages, above the running
 * Posts / Drafts / Open PR stat line, with the installed app version in a
 * footer. Each row navigates to its detail page; nothing is edited here.
 */
@Composable
fun SettingsScreen(
  connection: RepoConnection,
  publishedCount: Int,
  draftCount: Int,
  openPullRequestCount: Int,
  checkResult: ConnectionCheck?,
  appVersion: String,
  onOpenPage: (SettingsPage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    BloggoAppBar(
      title = "Settings",
      subtitle = (checkResult as? ConnectionCheck.Connected)?.let { "connected · ${it.defaultBranch}" },
    )

    Column(Modifier.padding(horizontal = 18.dp)) {
      StatLine(
        stats = listOf(
          "$publishedCount" to "Posts",
          "$draftCount" to "Drafts",
          "$openPullRequestCount" to "Open PR",
        ),
        modifier = Modifier.padding(top = 4.dp),
      )

      CellGroup(Modifier.padding(top = 20.dp)) {
        settingsMenuRows.forEachIndexed { index, row ->
          Cell(
            title = row.title,
            subtitle = row.subtitle,
            icon = row.icon,
            showDivider = index != settingsMenuRows.lastIndex,
            onClick = { onOpenPage(row.page) },
            trailing = {
              BloggoIcon(
                BloggoIcons.ChevronRight,
                contentDescription = null,
                size = 18.dp,
                tint = BloggoTheme.colors.inkFaint,
              )
            },
          )
        }
      }

      VersionFooter(
        version = appVersion,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 28.dp, bottom = 38.dp),
      )
    }
  }
}

/** `Bloggo 1.1.0 (debug)` — name in the UI face, version in mono, both faint. */
@Composable
private fun VersionFooter(version: String, modifier: Modifier = Modifier) {
  if (version.isBlank()) return
  val colors = BloggoTheme.colors
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Bloggo ", style = BloggoTheme.type.cellSubtitle, color = colors.inkFaint)
    Text(version, style = BloggoTheme.type.meta, color = colors.inkFaint)
  }
}

@Preview(heightDp = 720)
@Composable
private fun SettingsMenuPreview() {
  BloggoTheme {
    SettingsScreen(
      connection = RepoConnection(repository = "rrajath/blog"),
      publishedCount = 38,
      draftCount = 2,
      openPullRequestCount = 1,
      checkResult = null,
      appVersion = "1.1.0 (debug)",
      onOpenPage = {},
    )
  }
}
