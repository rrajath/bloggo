package com.rrajath.bloggo.ui.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.PublishAction
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.ThemeMode
import com.rrajath.bloggo.data.github.ConnectionCheck
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.ArtMode
import com.rrajath.bloggo.designsystem.component.Banner
import com.rrajath.bloggo.designsystem.component.BannerTone
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.ButtonTone
import com.rrajath.bloggo.designsystem.component.Cell
import com.rrajath.bloggo.designsystem.component.CellGroup
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.component.SegmentedControl
import com.rrajath.bloggo.designsystem.component.StatLine
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/**
 * What the app thinks your blog is, and how to correct it — plus the
 * app-wide preferences that don't belong to a single screen.
 *
 * Nothing about the connection is read-only: a Hugo site has no schema file
 * the app can trust blindly, so every path and field list below is a
 * correctable default rather than an assertion. Detection (the framework
 * name, whether a config file was found at all) still comes from the real
 * GitHub connection check; the specific filename, the paths, and the
 * frontmatter template are the writer's to set.
 */
@Composable
fun RepoScreen(
  connection: RepoConnection,
  publishedCount: Int,
  draftCount: Int,
  openPullRequestCount: Int,
  storedToken: String?,
  checkResult: ConnectionCheck?,
  isChecking: Boolean,
  onSaveConnection: (repository: String, branch: String, siteUrl: String, authorName: String, token: String) -> Unit,
  onClearToken: () -> Unit,
  onSavePostPath: (String) -> Unit,
  onSaveImagePath: (String) -> Unit,
  onSaveHugoConfigFile: (String) -> Unit,
  onSaveFrontmatterFields: (String) -> Unit,
  onPublishActionChange: (PublishAction) -> Unit,
  artMode: ArtMode,
  onArtModeChange: (ArtMode) -> Unit,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  onVisitSite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors

  val framework = when {
    checkResult is ConnectionCheck.Connected && checkResult.hugoDetected -> "Hugo"
    checkResult is ConnectionCheck.Connected -> "Not detected"
    else -> "Hugo"
  }

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
      Eyebrow("GitHub connection")
      ConnectionSection(
        connection = connection,
        storedToken = storedToken,
        onSave = onSaveConnection,
        onClearToken = onClearToken,
        onVisitSite = onVisitSite,
      )
      ConnectionStatus(checkResult = checkResult, isChecking = isChecking)

      StatLine(
        stats = listOf(
          "$publishedCount" to "Posts",
          "$draftCount" to "Drafts",
          "$openPullRequestCount" to "Open PR",
        ),
        modifier = Modifier.padding(top = 16.dp),
      )

      Eyebrow("Detected from your repo")
      CellGroup {
        ToggleEditableCell(
          icon = BloggoIcons.Framework,
          title = framework,
          value = connection.hugoConfigFile,
          buttonLabel = "Change",
          onSave = onSaveHugoConfigFile,
        )
        EditableCell(
          icon = BloggoIcons.File,
          title = "Post path",
          value = connection.postPath,
          onSave = onSavePostPath,
        )
        EditableCell(
          icon = BloggoIcons.Image,
          title = "Image path",
          value = connection.imagePath,
          onSave = onSaveImagePath,
        )
        ToggleEditableCell(
          icon = BloggoIcons.TextLines,
          title = "Frontmatter fields",
          value = connection.frontmatterFields,
          buttonLabel = "Edit",
          onSave = onSaveFrontmatterFields,
          showDivider = false,
        )
      }

      Eyebrow("Publishing")
      CellGroup {
        Cell(
          title = "Default action",
          subtitle = when (connection.publishAction) {
            PublishAction.CommitToMain -> "Commits straight to main"
            PublishAction.OpenPullRequest -> "Opens a pull request"
            PublishAction.AskEveryTime -> "Asks for every post"
          },
          icon = BloggoIcons.Branch,
          showDivider = false,
          trailing = {
            SegmentedControl(
              options = listOf(PublishAction.CommitToMain, PublishAction.OpenPullRequest, PublishAction.AskEveryTime),
              selected = connection.publishAction,
              onSelect = onPublishActionChange,
              modifier = Modifier.width(150.dp),
              label = {
                when (it) {
                  PublishAction.CommitToMain -> "Main"
                  PublishAction.OpenPullRequest -> "PR"
                  PublishAction.AskEveryTime -> "Ask"
                }
              },
            )
          },
        )
      }

      Eyebrow("Appearance")
      CellGroup {
        Cell(
          title = "Theme",
          subtitle = when (themeMode) {
            ThemeMode.System -> "Matches your device"
            ThemeMode.Light -> "Always light"
            ThemeMode.Dark -> "Always dark"
          },
          icon = BloggoIcons.Sun,
          trailing = {
            SegmentedControl(
              options = listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark),
              selected = themeMode,
              onSelect = onThemeModeChange,
              modifier = Modifier.width(180.dp),
              label = {
                when (it) {
                  ThemeMode.System -> "Auto"
                  ThemeMode.Light -> "Light"
                  ThemeMode.Dark -> "Dark"
                }
              },
            )
          },
        )
        Cell(
          title = "Cover art",
          subtitle = "Generated covers on post cards",
          icon = BloggoIcons.Image,
          trailing = {
            SegmentedControl(
              options = listOf(ArtMode.Generated, ArtMode.None),
              selected = artMode,
              onSelect = onArtModeChange,
              modifier = Modifier.width(150.dp),
              label = { if (it == ArtMode.Generated) "On" else "Off" },
            )
          },
        )
        Cell(
          title = "Reading typeface",
          subtitle = "Newsreader, matched to your site",
          icon = BloggoIcons.Typeface,
          showDivider = false,
        )
      }

      Column(Modifier.padding(bottom = 38.dp)) {}
    }
  }
}

/**
 * Paste a token, name the repo, save. Saving triggers a real
 * `GET /repos/{o}/{r}` validation and a Hugo detection check, per
 * ANDROID_TDD.md §7.1 — [ConnectionStatus] shows what came back.
 *
 * The PAT field is seeded from [storedToken] rather than left blank: a saved
 * token that vanishes from the screen the moment you save it is
 * indistinguishable from one that never saved at all. It stays masked by
 * default, with an eye toggle to reveal it in plain text on demand — never
 * logged, never sent anywhere but the field itself.
 */
@Composable
private fun ConnectionSection(
  connection: RepoConnection,
  storedToken: String?,
  onSave: (repository: String, branch: String, siteUrl: String, authorName: String, token: String) -> Unit,
  onClearToken: () -> Unit,
  onVisitSite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var repository by remember(connection.repository) { mutableStateOf(connection.repository) }
  var branch by remember(connection.branch) { mutableStateOf(connection.branch) }
  var siteUrl by remember(connection.siteHost) { mutableStateOf(connection.siteHost) }
  var authorName by remember(connection.authorName) { mutableStateOf(connection.authorName) }
  // Keyed on [storedToken] as well as on hasToken: the token is now read off the
  // main thread, so it arrives a frame or two after this first composes and the
  // field has to pick it up when it does.
  var token by remember(connection.hasToken, storedToken) { mutableStateOf(storedToken.orEmpty()) }
  var revealToken by remember { mutableStateOf(false) }

  CellGroup(modifier) {
    Column(Modifier.padding(horizontal = 15.dp)) {
      ConnectionField(label = "Repository", value = repository, onValueChange = { repository = it })
      ConnectionField(label = "Branch", value = branch, onValueChange = { branch = it })
      ConnectionField(
        label = "Site URL",
        value = siteUrl,
        onValueChange = { siteUrl = it },
        trailing = if (siteUrl.isNotBlank()) {
          { BloggoIconButton(BloggoIcons.ExternalLink, "Visit site", onVisitSite) }
        } else null,
      )
      ConnectionField(
        label = "Author name",
        value = authorName,
        onValueChange = { authorName = it },
      )
      ConnectionField(
        label = "Fine-grained PAT",
        value = token,
        onValueChange = { token = it },
        mask = !revealToken,
        showDivider = false,
        trailing = {
          BloggoIconButton(
            icon = if (revealToken) BloggoIcons.EyeOff else BloggoIcons.Eye,
            contentDescription = if (revealToken) "Hide token" else "Show token",
            onClick = { revealToken = !revealToken },
          )
        },
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BloggoChip(
        if (connection.hasToken) "Token saved" else "No token saved",
        if (connection.hasToken) ChipTone.Live else ChipTone.Ghost,
      )
      Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (connection.hasToken) {
          BloggoButton("Clear", onClick = onClearToken, tone = ButtonTone.Ghost)
        }
        BloggoButton(
          label = "Save",
          onClick = { onSave(repository, branch, siteUrl, authorName, token) },
          modifier = Modifier.padding(start = 9.dp),
        )
      }
    }
  }
}

/** What the last [onSave] found, per the error categories in ANDROID_TDD.md §5.5. */
@Composable
private fun ConnectionStatus(
  checkResult: ConnectionCheck?,
  isChecking: Boolean,
  modifier: Modifier = Modifier,
) {
  when {
    isChecking -> Banner(
      text = "Checking connection…",
      icon = BloggoIcons.Branch,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult is ConnectionCheck.Connected -> Banner(
      text = "Connected · ${checkResult.defaultBranch} · " +
        (if (checkResult.isPrivate) "private" else "public") + " · " +
        (if (checkResult.hugoDetected) "Hugo detected" else "no Hugo config found at the root"),
      icon = BloggoIcons.Check,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult == ConnectionCheck.Unauthorized -> Banner(
      text = "Reconnect: the token is missing, expired, or can't see this repo",
      tone = BannerTone.Warning,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult == ConnectionCheck.RateLimited -> Banner(
      text = "GitHub rate limit reached — try again shortly",
      tone = BannerTone.Warning,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult == ConnectionCheck.NotFound -> Banner(
      text = "Repository not found, or the token can't see it",
      tone = BannerTone.Warning,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult == ConnectionCheck.NoNetwork -> Banner(
      text = "No connection right now — check again once you're online",
      tone = BannerTone.Warning,
      modifier = modifier.padding(top = 12.dp),
    )

    checkResult is ConnectionCheck.Unknown -> Banner(
      text = "Something went wrong" + (checkResult.httpCode?.let { " (HTTP $it)" } ?: ""),
      tone = BannerTone.Warning,
      modifier = modifier.padding(top = 12.dp),
    )
  }
}

@Composable
private fun ConnectionField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  mask: Boolean = false,
  showDivider: Boolean = true,
  trailing: (@Composable () -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  Column(modifier.padding(vertical = 14.dp)) {
    Text(
      label.uppercase(),
      style = BloggoTheme.type.fieldLabel,
      color = colors.inkFaint,
      modifier = Modifier.padding(bottom = 7.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = BloggoTheme.type.monoField.copy(color = colors.ink),
        visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush = SolidColor(colors.accent),
        modifier = Modifier.weight(1f),
      )
      if (trailing != null) trailing()
    }
    if (showDivider) {
      Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(1.dp).background(colors.ruleSoft))
    }
  }
}

/** A [Cell]-shaped row whose value is always a live text field, saved on every
 * keystroke — for settings with no reason to gate behind a separate edit mode. */
@Composable
private fun EditableCell(
  icon: BloggoIcon,
  title: String,
  value: String,
  onSave: (String) -> Unit,
  showDivider: Boolean = true,
) {
  val colors = BloggoTheme.colors
  var text by remember(value) { mutableStateOf(value) }
  Column {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier.size(32.dp).clip(BloggoTheme.shapes.small).background(colors.accentTint),
        contentAlignment = Alignment.Center,
      ) {
        BloggoIcon(icon, contentDescription = null, size = 16.dp, tint = colors.accent)
      }
      Column(Modifier.weight(1f)) {
        Text(title, style = BloggoTheme.type.cellTitle, color = colors.ink)
        BasicTextField(
          value = text,
          onValueChange = { text = it; onSave(it) },
          singleLine = true,
          textStyle = BloggoTheme.type.meta.copy(color = colors.inkFaint),
          cursorBrush = SolidColor(colors.accent),
          modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
      }
    }
    if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
  }
}

/** A [Cell]-shaped row that opens into a text field only once its trailing
 * button is tapped — for settings worth a deliberate "I meant to change this"
 * gesture rather than editing on contact. */
@Composable
private fun ToggleEditableCell(
  icon: BloggoIcon,
  title: String,
  value: String,
  buttonLabel: String,
  onSave: (String) -> Unit,
  showDivider: Boolean = true,
) {
  val colors = BloggoTheme.colors
  var editing by remember(value) { mutableStateOf(false) }
  var text by remember(value) { mutableStateOf(value) }
  Column {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier.size(32.dp).clip(BloggoTheme.shapes.small).background(colors.accentTint),
        contentAlignment = Alignment.Center,
      ) {
        BloggoIcon(icon, contentDescription = null, size = 16.dp, tint = colors.accent)
      }
      Column(Modifier.weight(1f)) {
        Text(title, style = BloggoTheme.type.cellTitle, color = colors.ink)
        if (editing) {
          BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = BloggoTheme.type.meta.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
          )
        } else {
          Text(value, style = BloggoTheme.type.meta, color = colors.inkFaint, modifier = Modifier.padding(top = 2.dp))
        }
      }
      BloggoButton(
        label = if (editing) "Done" else buttonLabel,
        onClick = {
          if (editing && text != value) onSave(text)
          editing = !editing
        },
        tone = ButtonTone.Ghost,
      )
    }
    if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
  }
}

@Preview(heightDp = 1200)
@Composable
private fun RepoPreview() {
  BloggoTheme {
    RepoScreen(
      connection = RepoConnection(
        repository = "rrajath/blog",
        branch = "main",
        siteHost = "rrajath.dev",
      ),
      publishedCount = 38,
      draftCount = 2,
      openPullRequestCount = 1,
      storedToken = null,
      checkResult = null,
      isChecking = false,
      onSaveConnection = { _, _, _, _, _ -> },
      onClearToken = {},
      onSavePostPath = {},
      onSaveImagePath = {},
      onSaveHugoConfigFile = {},
      onSaveFrontmatterFields = {},
      onPublishActionChange = {},
      artMode = ArtMode.Generated,
      onArtModeChange = {},
      themeMode = ThemeMode.System,
      onThemeModeChange = {},
      onVisitSite = {},
    )
  }
}
