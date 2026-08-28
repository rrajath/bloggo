package com.rrajath.bloggo.ui.settings

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.FrontmatterType
import com.rrajath.bloggo.data.PublishAction
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.ThemeMode
import com.rrajath.bloggo.data.github.ConnectionCheck
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.Banner
import com.rrajath.bloggo.designsystem.component.BannerTone
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.BloggoSwitch
import com.rrajath.bloggo.designsystem.component.ButtonTone
import com.rrajath.bloggo.designsystem.component.Cell
import com.rrajath.bloggo.designsystem.component.CellGroup
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.SegmentedControl
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.ui.review.ReadabilityCheck

/* ---------------------------------------------------------------------------
 * Shared scaffold
 * ------------------------------------------------------------------------- */

/** Every detail page: a back-chevron app bar and a scrolling, 18dp-inset body.
 * No tab bar — [com.rrajath.bloggo.Route.SettingsDetail] is not a tab. */
@Composable
private fun SettingsDetailScaffold(
  title: String,
  onBack: () -> Unit,
  content: @Composable () -> Unit,
) {
  Column(
    Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    BloggoAppBar(title = title, onBack = onBack)
    Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
      content()
      Column(Modifier.padding(bottom = 38.dp)) {}
    }
  }
}

/* ---------------------------------------------------------------------------
 * 1. GitHub Connection
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsConnectionScreen(
  connection: RepoConnection,
  storedToken: String?,
  checkResult: ConnectionCheck?,
  isChecking: Boolean,
  onSave: (repository: String, branch: String, siteUrl: String, authorName: String, token: String) -> Unit,
  onClearToken: () -> Unit,
  onVisitSite: () -> Unit,
  onBack: () -> Unit,
) {
  SettingsDetailScaffold("GitHub Connection", onBack) {
    ConnectionSection(
      connection = connection,
      storedToken = storedToken,
      onSave = onSave,
      onClearToken = onClearToken,
      onVisitSite = onVisitSite,
    )
    ConnectionStatus(checkResult = checkResult, isChecking = isChecking)
  }
}

/* ---------------------------------------------------------------------------
 * 2. Repo Settings
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsRepoScreen(
  connection: RepoConnection,
  checkResult: ConnectionCheck?,
  onSaveHugoConfigFile: (String) -> Unit,
  onSavePostPath: (String) -> Unit,
  onSaveImagePath: (String) -> Unit,
  onSaveFrontmatterFields: (String) -> Unit,
  onSaveFrontmatterType: (FrontmatterType) -> Unit,
  onBack: () -> Unit,
) {
  val framework = when {
    checkResult is ConnectionCheck.Connected && checkResult.hugoDetected -> "Hugo"
    checkResult is ConnectionCheck.Connected -> "Not detected"
    else -> "Hugo"
  }
  SettingsDetailScaffold("Repo Settings", onBack) {
    CellGroup(Modifier.padding(top = 4.dp)) {
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
      )
      Cell(
        title = "Frontmatter type",
        subtitle = when (connection.frontmatterType) {
          FrontmatterType.Yaml -> "New posts open with --- fences"
          FrontmatterType.Toml -> "New posts open with +++ fences"
        },
        icon = BloggoIcons.Code,
        showDivider = false,
        trailing = {
          SegmentedControl(
            options = listOf(FrontmatterType.Yaml, FrontmatterType.Toml),
            selected = connection.frontmatterType,
            onSelect = onSaveFrontmatterType,
            modifier = Modifier.width(130.dp),
            label = { if (it == FrontmatterType.Yaml) "YAML" else "TOML" },
          )
        },
      )
    }
  }
}

/* ---------------------------------------------------------------------------
 * 3. Publishing
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsPublishingScreen(
  connection: RepoConnection,
  onPublishActionChange: (PublishAction) -> Unit,
  onBack: () -> Unit,
) {
  SettingsDetailScaffold("Publishing", onBack) {
    CellGroup(Modifier.padding(top = 4.dp)) {
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
  }
}

/* ---------------------------------------------------------------------------
 * 4. Appearance
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsAppearanceScreen(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  onBack: () -> Unit,
) {
  SettingsDetailScaffold("Appearance", onBack) {
    CellGroup(Modifier.padding(top = 4.dp)) {
      Cell(
        title = "Theme",
        subtitle = when (themeMode) {
          ThemeMode.System -> "Matches your device"
          ThemeMode.Light -> "Always light"
          ThemeMode.Dark -> "Always dark"
        },
        icon = BloggoIcons.Sun,
        showDivider = false,
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
    }
  }
}

/* ---------------------------------------------------------------------------
 * 5. Readability Review
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsReadabilityScreen(
  readabilityChecks: Set<ReadabilityCheck>,
  onReadabilityChecksChange: (Set<ReadabilityCheck>) -> Unit,
  onBack: () -> Unit,
) {
  SettingsDetailScaffold("Readability Review", onBack) {
    CellGroup(Modifier.padding(top = 4.dp)) {
      readabilityCheckRows.forEachIndexed { index, (check, title, subtitle) ->
        Cell(
          title = title,
          subtitle = subtitle,
          showDivider = index != readabilityCheckRows.lastIndex,
          trailing = {
            BloggoSwitch(
              checked = check in readabilityChecks,
              onCheckedChange = { on ->
                onReadabilityChecksChange(
                  if (on) readabilityChecks + check else readabilityChecks - check
                )
              },
              contentDescription = title,
            )
          },
        )
      }
    }
  }
}

/* ---------------------------------------------------------------------------
 * 6. Import / Export
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsImportExportScreen(
  onExport: () -> Unit,
  onImport: () -> Unit,
  onBack: () -> Unit,
) {
  val colors = BloggoTheme.colors
  SettingsDetailScaffold("Import / Export", onBack) {
    Text(
      "Export writes every setting except your access token to a JSON file. " +
        "Import applies whatever it finds and leaves the rest untouched; it does " +
        "not reconnect, so open GitHub Connection and Save afterward.",
      style = BloggoTheme.type.body,
      color = colors.inkFaint,
      modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )
    CellGroup {
      Cell(
        title = "Export settings",
        subtitle = "Save a bloggo-settings.json file",
        icon = BloggoIcons.Upload,
        onClick = onExport,
        trailing = {
          BloggoIcon(BloggoIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = colors.inkFaint)
        },
      )
      Cell(
        title = "Import settings",
        subtitle = "Read settings from a JSON file",
        icon = BloggoIcons.Download,
        showDivider = false,
        onClick = onImport,
        trailing = {
          BloggoIcon(BloggoIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = colors.inkFaint)
        },
      )
    }
  }
}

/* ---------------------------------------------------------------------------
 * Shared privates (lifted from the old single RepoScreen)
 * ------------------------------------------------------------------------- */

/**
 * Paste a token, name the repo, save. Saving triggers a real
 * `GET /repos/{o}/{r}` validation and a Hugo detection check, per
 * ANDROID_TDD.md §7.1 — [ConnectionStatus] shows what came back.
 *
 * The PAT field is seeded from [storedToken] rather than left blank: a saved
 * token that vanishes from the screen the moment you save it is
 * indistinguishable from one that never saved at all.
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
  var siteUrl by remember(connection.siteUrl) { mutableStateOf(TextFieldValue(connection.siteUrl)) }
  var authorName by remember(connection.authorName) { mutableStateOf(connection.authorName) }
  var token by remember(connection.hasToken, storedToken) { mutableStateOf(storedToken.orEmpty()) }
  var revealToken by remember { mutableStateOf(false) }

  CellGroup(modifier) {
    Column(Modifier.padding(horizontal = 15.dp)) {
      SettingsField(
        label = "Repository",
        value = repository,
        onValueChange = { repository = it },
        placeholder = "username/blog",
      )
      SettingsField(
        label = "Branch",
        value = branch,
        onValueChange = { branch = it },
        placeholder = "main",
      )
      SiteUrlField(
        value = siteUrl,
        onValueChange = { siteUrl = it },
        onVisitSite = onVisitSite.takeIf { siteUrl.text.isNotBlank() },
      )
      SettingsField(
        label = "Author name",
        value = authorName,
        onValueChange = { authorName = it },
        placeholder = "John Doe",
      )
      SettingsField(
        label = "Fine-grained PAT",
        value = token,
        onValueChange = { token = it },
        placeholder = "GitHub PAT to push your changes",
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
          onClick = { onSave(repository, branch, siteUrl.text, authorName, token) },
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

/**
 * A connection text field. Shows [placeholder] in faint ink when empty
 * (prototype `.field input::placeholder{color:var(--ink-3)}`).
 */
@Composable
private fun SettingsField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
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
      Box(Modifier.weight(1f)) {
        if (value.isEmpty()) {
          Text(
            placeholder,
            style = BloggoTheme.type.monoField,
            color = colors.inkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          singleLine = true,
          textStyle = BloggoTheme.type.monoField.copy(color = colors.ink),
          visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
          cursorBrush = SolidColor(colors.accent),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (trailing != null) trailing()
    }
    if (showDivider) {
      Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(1.dp).background(colors.ruleSoft))
    }
  }
}

/**
 * The Site URL field. Stores the full URL including scheme, and — the one
 * behavior that earns its own composable — prefills `https://` with the caret
 * after the slashes the first time an empty field gains focus.
 */
@Composable
private fun SiteUrlField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  onVisitSite: (() -> Unit)?,
) {
  val colors = BloggoTheme.colors
  Column(Modifier.padding(vertical = 14.dp)) {
    Text(
      "SITE URL",
      style = BloggoTheme.type.fieldLabel,
      color = colors.inkFaint,
      modifier = Modifier.padding(bottom = 7.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.weight(1f)) {
        if (value.text.isEmpty()) {
          Text(
            "https://your-blog.com",
            style = BloggoTheme.type.monoField,
            color = colors.inkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          singleLine = true,
          textStyle = BloggoTheme.type.monoField.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.accent),
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
              if (state.isFocused && value.text.isBlank()) {
                onValueChange(TextFieldValue("https://", selection = TextRange(8)))
              }
            },
        )
      }
      if (onVisitSite != null) {
        BloggoIconButton(BloggoIcons.ExternalLink, "Visit site", onVisitSite)
      }
    }
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(1.dp).background(colors.ruleSoft))
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

/** The order the readability toggles appear in Settings. Titles match the
 * review screen's legend and Notes wording. */
private val readabilityCheckRows: List<Triple<ReadabilityCheck, String, String>> = listOf(
  Triple(ReadabilityCheck.HardSentences, "Hard-to-read sentences", "Long, dense sentences by reading grade"),
  Triple(ReadabilityCheck.PassiveVoice, "Passive voice", "\"was written\" rather than \"wrote\""),
  Triple(ReadabilityCheck.Adverbs, "Adverbs", "\"-ly\" words a stronger verb could replace"),
  Triple(ReadabilityCheck.WeakQualifiers, "Weak qualifiers", "\"very\", \"really\", \"quite\""),
  Triple(ReadabilityCheck.ComplexWords, "Complex and wordy phrases", "\"utilize\", \"in order to\""),
  Triple(ReadabilityCheck.RepeatedWords, "Repeated words", "The same word twice in close range"),
  Triple(ReadabilityCheck.SameOpenerSentences, "Same-opener sentences", "Several sentences starting the same way"),
  Triple(ReadabilityCheck.LongParagraphs, "Long paragraphs", "Paragraphs over about 150 words"),
  Triple(ReadabilityCheck.DraftMarkers, "Leftover draft markers", "TODO, FIXME, TK, or [bracketed] notes"),
)

@Preview
@Composable
private fun ConnectionScreenPreview() {
  BloggoTheme {
    SettingsConnectionScreen(
      connection = RepoConnection(repository = "rrajath/blog", siteUrl = "https://rrajath.dev"),
      storedToken = null,
      checkResult = null,
      isChecking = false,
      onSave = { _, _, _, _, _ -> },
      onClearToken = {},
      onVisitSite = {},
      onBack = {},
    )
  }
}

@Preview
@Composable
private fun ReadabilityScreenPreview() {
  BloggoTheme {
    SettingsReadabilityScreen(
      readabilityChecks = ReadabilityCheck.All,
      onReadabilityChecksChange = {},
      onBack = {},
    )
  }
}
