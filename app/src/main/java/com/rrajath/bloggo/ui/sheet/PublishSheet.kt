package com.rrajath.bloggo.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.github.describe
import com.rrajath.bloggo.data.publish.PublishResult
import com.rrajath.bloggo.data.resolvePagePath
import com.rrajath.bloggo.data.resolvePostPath
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.designsystem.component.Banner
import com.rrajath.bloggo.designsystem.component.BannerTone
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoSwitch
import com.rrajath.bloggo.designsystem.component.ButtonTone
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIconSize
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.StagedMedia
import com.rrajath.bloggo.model.currentFrontmatterTimestamp
import com.rrajath.bloggo.model.effectiveDate
import com.rrajath.bloggo.model.parseFrontmatter
import kotlinx.coroutines.launch

/**
 * v1 of the write/publish loop: a commit message, a plain list of what's
 * about to change, and a Publish button that commits straight to
 * [RepoConnection.branch] via [com.rrajath.bloggo.data.publish.PostPublishRepository].
 * No "open a pull request" flow and no line-level diff yet — see
 * `docs/WRITE_PUBLISH_MEDIA_PLAN.md`'s "Known limitations" for what's
 * deliberately deferred.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishSheet(
  post: Post,
  connection: RepoConnection,
  stagedMedia: List<StagedMedia>,
  isPublishing: Boolean,
  publishResult: PublishResult?,
  onDismiss: () -> Unit,
  onPublish: (message: String, date: String) -> Unit,
) {
  val colors = BloggoTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  val defaultMessage = remember(post.slug, post.state, post.kind) {
    if (post.kind == DocKind.Page) {
      "${if (post.repoPath == null) "Add page" else "Update page"}: ${post.title}"
    } else {
      "${if (post.state == PostState.Draft) "New post" else "Update"}: ${post.title}"
    }
  }
  var message by remember(post.slug) { mutableStateOf(defaultMessage) }
  val path = remember(post.repoPath, connection.postPath, post.slug, post.kind) {
    post.repoPath ?: if (post.kind == DocKind.Page) resolvePagePath(post.slug) else resolvePostPath(connection.postPath, post.slug)
  }

  // Drafts can take multiple days to finish, so the frontmatter `date:` may
  // not already be today — the toggle defaults on (the common case, finishing
  // and publishing the same day) but a writer who started this draft earlier
  // needs to see, and be able to keep, its real date.
  var setDateToToday by remember(post.slug) { mutableStateOf(true) }
  var dateText by remember(post.slug) {
    mutableStateOf(post.markdown.parseFrontmatter().effectiveDate().orEmpty())
  }
  fun resolvedDate() = if (setDateToToday) currentFrontmatterTimestamp() else dateText.trim()

  // Same hide-then-callback shape PostDetailsSheet already uses: dismissing
  // straight through `onDismiss` would skip ModalBottomSheet's own close
  // animation.
  fun dismiss(after: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) after() }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.paperRaised,
    contentColor = colors.ink,
  ) {
    Column(Modifier.padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
      Text(
        "Publish",
        style = BloggoTheme.type.displaySmall,
        modifier = Modifier.padding(bottom = 12.dp),
      )

      PublishField(label = "Commit message", value = message, onValueChange = { message = it })

      Eyebrow("Changes", modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
      ChangedFileRow(icon = BloggoIcons.File, path = path)
      stagedMedia.forEach { media -> ChangedFileRow(icon = BloggoIcons.Image, path = media.repoPath, isNew = true) }

      Eyebrow("Frontmatter", modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
      Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text("Set date to Today", style = BloggoTheme.type.cellTitle)
          Text(
            if (setDateToToday) {
              "The date on the blog post will be set to the current date"
            } else {
              "The date on the blog post will be set to the following date"
            },
            style = BloggoTheme.type.cellSubtitle,
            color = colors.inkFaint,
          )
        }
        BloggoSwitch(setDateToToday, { setDateToToday = it }, contentDescription = "Set date to Today")
      }
      if (!setDateToToday) {
        PublishField(label = "Date", value = dateText, onValueChange = { dateText = it }, mono = true, singleLine = true)
      }

      if (publishResult is PublishResult.Failed) {
        Banner(
          text = "Couldn't publish: ${publishResult.error.describe()}",
          tone = BannerTone.Warning,
          actionLabel = "Retry",
          onAction = { onPublish(message, resolvedDate()) },
          modifier = Modifier.padding(top = 14.dp),
        )
      }
      if (isPublishing) {
        Banner(text = "Publishing…", modifier = Modifier.padding(top = 14.dp))
      }

      Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        BloggoButton("Later", { dismiss(onDismiss) }, tone = ButtonTone.Ghost, enabled = !isPublishing)
        BloggoButton(
          "Publish",
          { onPublish(message, resolvedDate()) },
          modifier = Modifier.weight(1f),
          enabled = !isPublishing,
        )
      }
    }
  }
}

@Composable
private fun ChangedFileRow(icon: BloggoIcon, path: String, isNew: Boolean = false) {
  val colors = BloggoTheme.colors
  Row(
    Modifier.fillMaxWidth().padding(vertical = 9.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    BloggoIcon(icon, contentDescription = null, size = BloggoIconSize.Small, tint = colors.inkFaint)
    Text(
      path,
      style = BloggoTheme.type.monoField.copy(fontSize = BloggoTheme.type.meta.fontSize),
      color = colors.ink,
      modifier = Modifier.weight(1f),
    )
    if (isNew) BloggoChip("New", ChipTone.Queued)
  }
}

@Composable
private fun PublishField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  mono: Boolean = false,
  singleLine: Boolean = false,
) {
  val colors = BloggoTheme.colors
  Column(Modifier.padding(vertical = 14.dp)) {
    Text(
      label.uppercase(),
      style = BloggoTheme.type.fieldLabel,
      color = colors.inkFaint,
      modifier = Modifier.padding(bottom = 7.dp),
    )
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = singleLine,
      textStyle = if (mono) {
        BloggoTheme.type.monoField.copy(color = colors.ink)
      } else {
        BloggoTheme.type.body.copy(color = colors.ink)
      },
      cursorBrush = SolidColor(colors.accent),
      modifier = Modifier.fillMaxWidth(),
    )
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft).padding(top = 14.dp))
  }
}
