package com.rrajath.bloggo.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.data.publish.PublishResult
import com.rrajath.bloggo.designsystem.BloggoFonts
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.SegmentedControl
import com.rrajath.bloggo.designsystem.component.ToolbarButton
import com.rrajath.bloggo.designsystem.editor.MarkdownHighlighter
import com.rrajath.bloggo.designsystem.editor.rememberMarkdownTransformation
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.FrontmatterEdits
import com.rrajath.bloggo.model.MediaFile
import com.rrajath.bloggo.model.PageFrontmatterEdits
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.StagedMedia
import com.rrajath.bloggo.model.currentFrontmatterTimestamp
import com.rrajath.bloggo.model.isPushed
import com.rrajath.bloggo.model.markdownWordCount
import com.rrajath.bloggo.model.syncSlugToTitle
import com.rrajath.bloggo.model.withFrontmatterEdits
import com.rrajath.bloggo.model.withPageFrontmatterEdits
import com.rrajath.bloggo.model.withUpdatedLastmod
import com.rrajath.bloggo.ui.sheet.PostDetailsSheet
import com.rrajath.bloggo.ui.sheet.PublishSheet
import kotlinx.coroutines.delay

/** How long typing has to pause before the slug is re-synced to the title.
 * [syncSlugToTitle] rewrites the `slug:` line elsewhere in the document, and
 * doing that inside the same [androidx.compose.foundation.text.BasicTextField.onValueChange]
 * that's already carrying the user's own edit means every title keystroke was
 * landing as two edits in one [TextFieldValue] — which is what was driving the
 * IME to treat it as a non-incremental change and restart the input
 * connection instead of updating it, stalling and dropping keystrokes on real
 * devices. Waiting for a pause keeps it to one edit per keystroke. */
private const val SLUG_SYNC_DEBOUNCE_MS = 500L

/** Preserves [selection] across a rewrite that happened somewhere else in the
 * document (the slug auto-sync), by diffing [old] against [new] rather than
 * assuming where the rewritten field sits relative to the cursor. A selection
 * inside the rewritten span collapses to the span's start. */
private fun adjustSelectionForRewrite(old: String, new: String, selection: TextRange): TextRange {
  if (old == new) return selection
  val maxPrefix = minOf(old.length, new.length)
  var prefix = 0
  while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++
  val maxSuffix = maxPrefix - prefix
  var suffix = 0
  while (suffix < maxSuffix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
  val oldChangedEnd = old.length - suffix
  val delta = new.length - old.length
  fun shift(offset: Int) = when {
    offset <= prefix -> offset
    offset >= oldChangedEnd -> offset + delta
    else -> prefix
  }
  return TextRange(shift(selection.start), shift(selection.end))
}

/**
 * Capitalizes the first character typed right at the start of a heading's
 * text (immediately after its `#`/`##`/… marker), and leaves everything else
 * alone.
 *
 * [KeyboardCapitalization] is an IME-level flag with no notion of markdown —
 * `Sentences` capitalizes after every newline, not just heading markers,
 * which is what over-applied capitalization to every line/paragraph in the
 * body. Doing it here instead, as a manual post-processing step on the same
 * shape of diff [adjustSelectionForRewrite] already computes, is what lets it
 * be conditioned on "is this line a heading" — and it fires for swipe/gesture
 * input the same as normal typing, since both land here as an ordinary
 * insertion rather than going through the IME's own auto-cap.
 */
private fun capitalizeHeadingFirstLetter(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
  val oldText = old.text
  val newText = new.text
  if (newText.length <= oldText.length) return new // not an insertion
  val maxPrefix = minOf(oldText.length, newText.length)
  var prefix = 0
  while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
  val maxSuffix = maxPrefix - prefix
  var suffix = 0
  while (suffix < maxSuffix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++
  val insertedEnd = newText.length - suffix
  if (insertedEnd <= prefix) return new

  val lineStart = newText.lastIndexOf('\n', prefix - 1).let { if (it == -1) 0 else it + 1 }
  val lineEnd = newText.indexOf('\n', prefix).let { if (it == -1) newText.length else it }
  val line = newText.substring(lineStart, lineEnd)
  val markerLength = MarkdownHighlighter.headingMarkerLength(line) ?: return new
  // Only the very first character typed right after the marker gets capitalized —
  // not every insertion anywhere in the heading's text.
  if (prefix != lineStart + markerLength) return new

  val typedChar = newText[prefix]
  if (!typedChar.isLowerCase()) return new
  val rewritten = newText.substring(0, prefix) + typedChar.uppercaseChar() + newText.substring(prefix + 1)
  return TextFieldValue(rewritten, new.selection, new.composition)
}

/**
 * Markdown source editing with live styling.
 *
 * The field holds the literal file contents, frontmatter and all. Anything that
 * hides characters from the writer would put the app's idea of the document and
 * the repo's out of step, which is the failure mode a git-backed editor cannot
 * afford.
 */
@Composable
fun EditorScreen(
  post: Post,
  tagPool: List<String>,
  connection: RepoConnection,
  /** [post.slug]'s already-pushed check ([com.rrajath.bloggo.model.isPushed]) —
   * `remotePostSlugs` or `remotePageSlugs` from `BloggoApp.kt`, whichever
   * matches [post.kind]. Threaded down only as far as [PostDetailsSheet]
   * actually needs it, the same way [tagPool] already is. */
  remoteSlugs: Set<String>,
  stagedMediaForPost: List<StagedMedia>,
  isPublishing: Boolean,
  publishResult: PublishResult?,
  /** Set once, right after a pick/upload returns from `Route.Media`'s picker
   * mode — see [onPendingInsertConsumed]. Threaded down rather than owned
   * here since the media pick itself happens on a screen this composable
   * never sees. */
  pendingInsertImage: MediaFile?,
  /** True only while [post] is a transient Inbox-fragment preview, never
   * added to `posts`/`LocalPostStore` until it's actually promoted — see
   * [PostDetailsSheet]'s `isFragmentPreview` param, which this passes
   * straight through. Also gates the Commit toolbar action off here: a
   * fragment preview has no real draft entry yet for a publish to update,
   * so committing it would either crash or silently orphan a published post
   * `posts` never heard about — it must be promoted first. */
  isFragmentPreview: Boolean = false,
  /** The caret/selection [post.slug] was left at the last time this screen
   * was open for it, threaded down from `BloggoApp`'s `editorSelectionBySlug`
   * — this composable (and everything `remember`ed inside it) is fully
   * disposed and re-mounted on a round trip through Preview/Review, so
   * nothing local survives that on its own. Null for a slug that's never
   * been open in this session, which falls back to the same end-of-document
   * default a genuinely fresh editor session already used. */
  initialSelection: TextRange? = null,
  /** Reports every selection change (cursor moves, not just text edits) so
   * the caller can remember it for the next time this slug is opened. Firing
   * on every change rather than only on `onBack`/navigation is what makes a
   * plain-scroll-then-switch-tabs round trip (no edit, no explicit "leaving"
   * moment) still land on the right spot — see [initialSelection]. */
  onSelectionChange: (TextRange) -> Unit = {},
  onMarkdownChange: (markdown: String, wordCount: Int) -> Unit,
  onBack: () -> Unit,
  onPreview: () -> Unit,
  onFocus: () -> Unit,
  onReview: () -> Unit,
  onToast: (String) -> Unit,
  onDeletePost: () -> Unit,
  onMoveToInbox: () -> Unit,
  onPromoteToPost: () -> Unit = {},
  onDeleteFragment: () -> Unit = {},
  onPublish: (message: String, date: String, setDraftFalse: Boolean) -> Unit,
  onInsertImage: () -> Unit,
  onPendingInsertConsumed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  var value by remember(post.slug) {
    // A restored selection is only trustworthy as far as the document it was
    // captured against — coerced into range rather than trusted outright,
    // since the markdown length this slug last had (before whatever caused
    // this remount) isn't guaranteed to match `post.markdown.length` now.
    // Falling back to end-of-document mirrors the pre-existing default for a
    // slug this session has never had a recorded selection for.
    val restored = initialSelection?.let {
      TextRange(
        it.start.coerceIn(0, post.markdown.length),
        it.end.coerceIn(0, post.markdown.length),
      )
    }
    mutableStateOf(TextFieldValue(post.markdown, restored ?: TextRange(post.markdown.length)))
  }
  // Recomputed once per actual edit below, not on every recomposition — see `edit()`. A large
  // post's word count is a full-document pass (model/Model.kt), and the toolbar re-reading
  // value.text.markdownWordCount() directly on every recomposition was doing that pass a
  // *second* time for the exact same string onMarkdownChange already measured, doubling real
  // per-keystroke cost on documents big enough for it to matter. The count is handed to
  // onMarkdownChange rather than left for the caller to recompute, which is where that same
  // duplicate pass had simply moved to.
  var wordCount by remember(post.slug) { mutableStateOf(post.markdown.markdownWordCount()) }
  var showDetails by remember { mutableStateOf(false) }
  var showInsert by remember { mutableStateOf(false) }
  val transformation = rememberMarkdownTransformation()

  // The markdown the slug was last synced against — see the debounced LaunchedEffect below.
  var slugSyncBaseline by remember(post.slug) { mutableStateOf(post.markdown) }
  // A page's counterpart: the markdown `lastmod` was last stamped against, so
  // the same pause-in-typing settle that re-syncs the slug also upserts
  // `lastmod` to now whenever the settled text has actually moved on from it —
  // "whenever a page is edited," without a frontmatter rewrite on every
  // keystroke. Irrelevant for a post, which has no `lastmod` concept.
  var lastmodBaseline by remember(post.slug) { mutableStateOf(post.markdown) }
  var showPublish by remember { mutableStateOf(false) }
  // Set right after the Link toolbar action drops the caret past a prefilled
  // "https://"; consumed by the very next edit so a pasted URL that already
  // carries its own scheme replaces the prefill instead of stacking onto it.
  var pendingLinkSchemeAt by remember(post.slug) { mutableStateOf<Int?>(null) }

  // The shell reserves space for the IME with imePadding() (BloggoApp.kt), but
  // that alone doesn't scroll anything — it only shrinks the space the editor
  // has left. Without this, a cursor near the bottom of that shrunken area
  // stays exactly where it is and ends up hidden under the keyboard. Asking
  // the field to bring itself into view (on focus, and again as the cursor
  // moves while typing) is what actually scrolls it clear.
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  var isFieldFocused by remember { mutableStateOf(false) }
  LaunchedEffect(value.selection, isFieldFocused) {
    if (isFieldFocused) bringIntoViewRequester.bringIntoView()
  }
  // Keeps the caller's per-slug record (BloggoApp's editorSelectionBySlug)
  // current as the cursor moves, not just when the text changes — a plain
  // scroll-then-switch-screens round trip never touches `value.selection`
  // itself, but whatever it was last set to (by a prior edit or tap) is
  // still the best restore point this screen can offer on the way back in.
  // See [onSelectionChange]/[initialSelection]'s doc comments.
  LaunchedEffect(value.selection) {
    onSelectionChange(value.selection)
  }

  fun edit(transform: (TextFieldValue) -> TextFieldValue) {
    val previousValue = value
    val previousText = previousValue.text
    value = capitalizeHeadingFirstLetter(previousValue, transform(previousValue))
    // BasicTextField's onValueChange also fires for selection-only changes (moving the cursor,
    // selecting text to copy) with the text unchanged. Fan those out to nothing: they used to
    // trigger the exact same downstream cost as a real edit — parseFrontmatter/markdownWordCount
    // here, plus the caller's own post-list update and everything that recomposes because of it —
    // for zero actual content change.
    if (value.text != previousText) {
      val counted = value.text.markdownWordCount()
      wordCount = counted
      onMarkdownChange(value.text, counted)
    }
  }

  // A successful publish closes the sheet on its own — the writer asked once,
  // there is nothing left to confirm. A failure leaves it open with the
  // Retry banner PublishSheet already shows.
  LaunchedEffect(publishResult) {
    if (publishResult is PublishResult.Success) showPublish = false
  }

  // One-shot, the same shape as InboxScreen's requestFocusOnOpen/onFocusConsumed:
  // a pick or upload on Route.Media's picker mode sets this once, from a screen
  // this composable never sees, and consuming it here (rather than reacting to
  // every recomposition) is what makes it insert exactly once per pick.
  LaunchedEffect(pendingInsertImage) {
    val picked = pendingInsertImage ?: return@LaunchedEffect
    edit { MarkdownAction.figure(picked.sitePath, "").applyTo(it) }
    onPendingInsertConsumed()
  }

  // Debounced rather than run inside `edit()` on every keystroke: see SLUG_SYNC_DEBOUNCE_MS.
  LaunchedEffect(value.text) {
    delay(SLUG_SYNC_DEBOUNCE_MS)
    val settledText = value.text
    val synced = syncSlugToTitle(slugSyncBaseline, settledText)
    slugSyncBaseline = synced
    var finalText = synced
    if (post.kind == DocKind.Page && finalText != lastmodBaseline) {
      finalText = finalText.withUpdatedLastmod(currentFrontmatterTimestamp())
      lastmodBaseline = finalText
    }
    if (finalText != settledText) {
      value = TextFieldValue(finalText, adjustSelectionForRewrite(settledText, finalText, value.selection))
      val counted = finalText.markdownWordCount()
      wordCount = counted
      onMarkdownChange(finalText, counted)
    }
  }

  Column(modifier.fillMaxSize()) {
    // header
    Row(
      Modifier
        .fillMaxWidth()
        .background(colors.paper)
        .padding(start = 4.dp, end = 10.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      BloggoIconButton(BloggoIcons.ChevronLeft, "Back to library", onBack)
      Column(Modifier.weight(1f)) {
        Text("Editor", style = BloggoTheme.type.rowTitle, color = colors.ink, maxLines = 1)
        Row(
          Modifier.padding(top = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (post.kind == DocKind.Page) {
            // A page doesn't carry a draft workflow the way a post does — a
            // neutral chip, no state-specific subtitle beside it.
            BloggoChip("Page", ChipTone.Ghost)
          } else if (isFragmentPreview) {
            // Still just an Inbox capture wearing the Editor's clothes — the
            // Draft chip/copy would claim it's already a real draft, when
            // nothing is saved as one until "Promote to Post" is tapped.
            BloggoChip("Capture", ChipTone.Capture)
          } else {
            when (post.state) {
              PostState.Draft -> BloggoChip("Draft", ChipTone.Draft)
              PostState.InReview -> BloggoChip("In review", ChipTone.PullRequest)
              PostState.Published -> BloggoChip("Live", ChipTone.Live)
            }
            Text(
              when (post.state) {
                PostState.Draft -> "saved locally"
                PostState.InReview -> "editing an open pull request"
                PostState.Published -> "editing a published post"
              },
              style = BloggoTheme.type.meta,
              color = colors.inkFaint,
            )
          }
        }
      }
      SegmentedControl(
        options = listOf(EditorMode.Edit, EditorMode.Read),
        selected = EditorMode.Edit,
        onSelect = { if (it == EditorMode.Read) onPreview() },
        modifier = Modifier.width(130.dp),
        label = { it.label },
      )
      BloggoIconButton(
        icon = BloggoIcons.MoreVertical,
        contentDescription = "Post settings",
        onClick = { showDetails = true },
      )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))

    BasicTextField(
      value = value,
      onValueChange = { newValue ->
        val prefillEnd = pendingLinkSchemeAt
        pendingLinkSchemeAt = null
        edit { prev ->
          if (prefillEnd != null) dedupePastedLinkScheme(prev, newValue, prefillEnd) else newValue
        }
      },
      modifier = Modifier
        .testTag("editorMarkdownField")
        .weight(1f)
        .fillMaxWidth()
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { isFieldFocused = it.isFocused }
        .padding(horizontal = 18.dp, vertical = 16.dp),
      textStyle = BloggoTheme.type.editorSource.copy(color = colors.ink),
      visualTransformation = transformation,
      cursorBrush = SolidColor(colors.accent),
      // Sentences still drives normal capitalize-after-period/paragraph behavior
      // in body text — that part works and shouldn't be removed. But it's an
      // IME-level hint with no notion of markdown: many keyboards (notably during
      // swipe/gesture typing) don't treat the text right after a "#"/"##" marker
      // as a sentence start, so headings specifically were inconsistently
      // capitalized. capitalizeHeadingFirstLetter (in `edit()`) is a deterministic
      // backstop for exactly that case — a no-op if the IME already capitalized it.
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        autoCorrectEnabled = true,
      ),
    )

    EditorToolbar(
      wordCount = wordCount,
      onFormat = { action ->
        edit { action.applyTo(it) }
        // After Link, the caret sits just past the prefilled "https://" — remember
        // that spot so a pasted URL with its own scheme can replace the prefill.
        pendingLinkSchemeAt = if (action === MarkdownAction.Link) value.selection.max else null
      },
      onFocus = onFocus,
      onReview = onReview,
      onCommit = {
        if (isFragmentPreview) {
          onToast("Promote to Post first")
        } else {
          showPublish = true
        }
      },
      onInsert = { showInsert = true },
    )
  }

  if (showDetails) {
    PostDetailsSheet(
      post = post,
      tagPool = tagPool,
      isPushed = post.isPushed(remoteSlugs),
      isFragmentPreview = isFragmentPreview,
      onDismiss = { showDetails = false },
      onSave = { title, slug, date, tags, draft ->
        showDetails = false
        val edited = if (post.kind == DocKind.Page) {
          value.text.withPageFrontmatterEdits(PageFrontmatterEdits(title, slug, date), currentFrontmatterTimestamp())
        } else {
          value.text.withFrontmatterEdits(FrontmatterEdits(title, slug, date, tags, draft))
        }
        value = TextFieldValue(edited, TextRange(edited.length))
        slugSyncBaseline = edited
        lastmodBaseline = edited
        val counted = edited.markdownWordCount()
        wordCount = counted
        onMarkdownChange(edited, counted)
        onToast(if (post.kind == DocKind.Page) "Page details updated" else "Frontmatter updated")
      },
      onDelete = {
        showDetails = false
        onDeletePost()
      },
      onMoveToInbox = {
        showDetails = false
        onMoveToInbox()
      },
      onPromoteToPost = {
        showDetails = false
        onPromoteToPost()
      },
      onDeleteFragment = {
        showDetails = false
        onDeleteFragment()
      },
    )
  }

  if (showPublish) {
    PublishSheet(
      post = post.copy(markdown = value.text),
      connection = connection,
      stagedMedia = stagedMediaForPost,
      isPublishing = isPublishing,
      publishResult = publishResult,
      onDismiss = { showPublish = false },
      onPublish = onPublish,
    )
  }

  if (showInsert) {
    InsertSheet(
      onDismiss = { showInsert = false },
      onInsertImage = {
        showInsert = false
        onInsertImage()
      },
      onFormat = { action -> edit { action.applyTo(it) } },
    )
  }
}

/** Shared with [com.rrajath.bloggo.ui.preview.PreviewScreen]: Edit and Read are
 * the same segmented control in two places, not two different controls. */
enum class EditorMode(val label: String) { Edit("Edit"), Read("Read") }

@Composable
private fun EditorToolbar(
  wordCount: Int,
  onFormat: (MarkdownAction) -> Unit,
  onFocus: () -> Unit,
  onReview: () -> Unit,
  onCommit: () -> Unit,
  onInsert: () -> Unit,
) {
  val colors = BloggoTheme.colors
  // The shell's imePadding() only covers the keyboard inset; the toolbar sits at the
  // very bottom of the screen, so it also needs its own bottom padding for the
  // navigation gesture bar or its buttons get overlapped by the system bar.
  Column(Modifier.navigationBarsPadding()) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
    Row(
      Modifier
        .fillMaxWidth()
        .background(colors.paperRaised)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ToolbarButton({ onFormat(MarkdownAction.Heading) }, "Heading") {
        Text("#", style = BloggoTheme.type.displaySmall.copy(fontSize = 17.sp), color = colors.inkMuted)
      }
      ToolbarButton({ onFormat(MarkdownAction.Bold) }, "Bold") {
        Text("B", style = BloggoTheme.type.button.copy(fontWeight = FontWeight.ExtraBold), color = colors.inkMuted)
      }
      ToolbarButton({ onFormat(MarkdownAction.Italic) }, "Italic") {
        Text(
          "I",
          style = BloggoTheme.type.articleQuote.copy(fontFamily = BloggoFonts.Reading, fontStyle = FontStyle.Italic),
          color = colors.inkMuted,
        )
      }
      ToolbarButton({ onFormat(MarkdownAction.Code) }, "Inline code") {
        Text("‹/›", style = BloggoTheme.type.meta, color = colors.inkMuted)
      }
      ToolbarIcon(BloggoIcons.Link, "Link", { onFormat(MarkdownAction.Link) })
      ToolbarIcon(BloggoIcons.ListNumbered, "Numbered list", { onFormat(MarkdownAction.OrderedList) })
      ToolbarIcon(BloggoIcons.Plus, "Insert", onInsert)
      // Focus mode entry point is intentionally hidden/unreachable for now.
      // See PROGRESS.md open items: FocusScreen/FocusMode is currently dead code.
      if (false) {
        ToolbarIcon(BloggoIcons.FocusMode, "Focus mode", onFocus, accent = true)
      }
      ToolbarIcon(BloggoIcons.Readability, "Readability review", onReview, accent = true)
      ToolbarIcon(BloggoIcons.Commit, "Commit or open a pull request", onCommit, accent = true)

      Text(
        "%,d w".format(wordCount),
        style = BloggoTheme.type.meta,
        color = colors.inkFaint,
        modifier = Modifier.padding(horizontal = 10.dp),
      )
    }
  }
}

@Composable
private fun ToolbarIcon(
  icon: BloggoIcon,
  description: String,
  onClick: () -> Unit,
  accent: Boolean = false,
) {
  ToolbarButton(onClick, description) {
    com.rrajath.bloggo.designsystem.icon.BloggoIcon(
      icon,
      contentDescription = null,
      tint = if (accent) BloggoTheme.colors.accent else BloggoTheme.colors.inkMuted,
    )
  }
}

@Preview(heightDp = 860)
@Composable
private fun EditorPreview() {
  BloggoTheme {
    EditorScreen(
      post = SampleData.draft,
      tagPool = listOf("ai", "tooling", "craft"),
      connection = RepoConnection(),
      remoteSlugs = emptySet(),
      stagedMediaForPost = emptyList(),
      isPublishing = false,
      publishResult = null,
      pendingInsertImage = null,
      onMarkdownChange = { _, _ -> },
      onBack = {},
      onPreview = {},
      onFocus = {},
      onReview = {},
      onToast = {},
      onDeletePost = {},
      onMoveToInbox = {},
      onPublish = { _, _, _ -> },
      onInsertImage = {},
      onPendingInsertConsumed = {},
    )
  }
}
