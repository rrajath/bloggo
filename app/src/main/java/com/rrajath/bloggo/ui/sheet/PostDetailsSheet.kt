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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.BloggoSwitch
import com.rrajath.bloggo.designsystem.component.BloggoTagField
import com.rrajath.bloggo.designsystem.component.ButtonTone
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.effectiveDate
import com.rrajath.bloggo.model.parseFrontmatter
import com.rrajath.bloggo.model.parseTagList
import com.rrajath.bloggo.model.slugify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long typing has to pause before the slug is re-synced to the title —
 * the sheet's counterpart to `EditorScreen.kt`'s `SLUG_SYNC_DEBOUNCE_MS`. Title
 * and slug used to be written from the same `onValueChange` call, and two state
 * writes landing from one [BasicTextField] callback is what was making the IME
 * restart its input connection instead of updating it, dropping keystrokes (and,
 * per the report this fixes, breaking third-party accessibility-service text
 * expanders that inject text into the focused field). Deferring the slug write
 * to a debounced pass keeps every keystroke's callback to one write. */
private const val SLUG_SYNC_DEBOUNCE_MS = 500L

/** Frontmatter editing. Every field reads from `post.markdown`'s actual
 * frontmatter, never from a stand-in, and every edited field is handed
 * back on save — [onSave] is the only way any of this reaches the real `Post`,
 * so nothing here may be state that dies with the composable.
 *
 * [tagPool] is the distinct union of tags already used across every post the
 * writer has, for the tag autocomplete. It is computed once by the caller —
 * not derived from a single post's frontmatter, and not recomputed here per
 * keystroke — the same per-keystroke-recomputation bug already fixed once for
 * `repoConfig` (PROGRESS.md, Milestone 10). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsSheet(
  post: Post,
  tagPool: List<String>,
  /** [com.rrajath.bloggo.model.isPushed] for [post] — whether it already
   * exists in the repo, as opposed to only on this device. Changes what the
   * delete confirmation warns about (dropping the remote file too, not just
   * the local copy) and gates [onMoveToInbox]'s button, which only makes
   * sense for a draft that has never left this device. */
  isPushed: Boolean,
  /** True only while [post] is a transient Inbox-fragment preview —
   * `BloggoApp.kt`'s `transientFragmentPost`, opened by tapping a fragment in
   * the Inbox but never added to `posts`/`LocalPostStore` until [onPromoteToPost]
   * is actually tapped. Gates [onMoveToInbox] off (a fragment preview has
   * nowhere further "back" to move to — it's already effectively in the
   * Inbox), swaps the draft/page [onDelete] button for [onDeleteFragment]'s
   * "Delete capture" one, and gates the "Promote to Post" button on instead. */
  isFragmentPreview: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (title: String, slug: String, date: String, tags: List<String>, draft: Boolean) -> Unit,
  onDelete: () -> Unit,
  onMoveToInbox: () -> Unit,
  /** Promotes [post] from a transient fragment preview to a real Draft: adds
   * it to `posts`, persists it via `LocalPostStore`, and deletes the source
   * fragment. Only ever called while [isFragmentPreview] is true — see that
   * param's own doc comment. */
  onPromoteToPost: () -> Unit = {},
  /** Deletes the capture behind this fragment preview outright, in place of
   * [onDelete] — only ever called while [isFragmentPreview] is true. Unlike a
   * draft/page delete, a capture is never pushed, so there is no remote file
   * to warn about. */
  onDeleteFragment: () -> Unit = {},
) {
  val colors = BloggoTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  var confirmingDelete by remember(post.slug) { mutableStateOf(false) }
  val frontmatter = remember(post.markdown) { post.markdown.parseFrontmatter() }
  var title by remember(post.markdown) { mutableStateOf(frontmatter["title"] ?: post.title) }
  var slug by remember(post.markdown) { mutableStateOf(frontmatter["slug"] ?: post.slug) }
  var date by remember(post.markdown) { mutableStateOf(frontmatter.effectiveDate().orEmpty()) }
  var draft by remember(post.markdown) {
    mutableStateOf(frontmatter["draft"]?.equals("true", ignoreCase = true) ?: (post.state == PostState.Draft))
  }
  // The title the slug was last synced against — see the debounced LaunchedEffect below.
  var slugSyncBaseline by remember(post.markdown) { mutableStateOf(title) }

  // Debounced rather than run inside Title's onValueChange on every keystroke: see
  // SLUG_SYNC_DEBOUNCE_MS. Only follows the title while the slug hasn't been hand-edited
  // away from what the title would generate, same rule as the editor's own slug sync.
  LaunchedEffect(title) {
    delay(SLUG_SYNC_DEBOUNCE_MS)
    if (slug == slugify(slugSyncBaseline)) slug = slugify(title)
    slugSyncBaseline = title
  }

  // ModalBottomSheet already animates its own scrim-tap/swipe-down dismissal,
  // but a button here that flips `onDismiss`'s backing state straight away —
  // as Cancel, Save and Delete all do — would unmount the sheet before that
  // animation gets to run. Waiting for `hide()` first is the same fix the
  // official Material3 samples use for exactly this case.
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
        if (post.kind == DocKind.Page) "Page details" else "Post details",
        style = BloggoTheme.type.displaySmall,
        modifier = Modifier.padding(bottom = 12.dp),
      )

      SheetField("Title", title, { title = it })
      SheetField("Slug", slug, { slug = it }, mono = true)
      SheetField("Date", date, { date = it }, mono = true)

      // A page like About isn't tagged or drafted — showing those fields would
      // ask the writer to fill in something meaningless.
      // docs/PROTOTYPE_NOTES.md, "Pages replaced Media".
      var tags by remember(post.markdown) { mutableStateOf(frontmatter["tags"]?.parseTagList().orEmpty()) }
      if (post.kind == DocKind.Post) {
        Text(
          "TAGS",
          style = BloggoTheme.type.fieldLabel,
          color = colors.inkFaint,
          modifier = Modifier.padding(top = 14.dp, bottom = 7.dp),
        )
        BloggoTagField(tags = tags, onTagsChange = { tags = it }, suggestions = tagPool)

        Row(
          Modifier.fillMaxWidth().padding(vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f)) {
            Text("Keep as draft", style = BloggoTheme.type.cellTitle)
            Text(
              "Writes draft: true to frontmatter",
              style = BloggoTheme.type.cellSubtitle,
              color = colors.inkFaint,
            )
          }
          BloggoSwitch(draft, { draft = it }, contentDescription = "Keep as draft")
        }
      }

      // A fragment preview is still just an Inbox item wearing the Editor's
      // clothes — Promote is how it becomes a real draft; Move to Inbox and
      // Delete don't apply until it is one (see isFragmentPreview's own doc
      // comment).
      if (isFragmentPreview) {
        BloggoButton(
          "Promote to Post",
          { dismiss(onPromoteToPost) },
          icon = BloggoIcons.Pen,
          modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )
      } else if (post.state == PostState.Draft && !isPushed) {
        // Moving to Inbox only makes sense for a draft that has never left
        // this device — the moment it's pushed, its content is a real file
        // in the repo, not a fragment to demote it back into.
        BloggoButton(
          "Move to Inbox",
          { dismiss(onMoveToInbox) },
          tone = ButtonTone.Ghost,
          icon = BloggoIcons.Inbox,
          modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )
      }

      if (isFragmentPreview) {
        BloggoButton(
          "Delete capture",
          { confirmingDelete = true },
          tone = ButtonTone.Danger,
          icon = BloggoIcons.Trash,
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
      } else if (post.state == PostState.Draft) {
        BloggoButton(
          if (post.kind == DocKind.Page) "Delete page" else "Delete draft",
          { confirmingDelete = true },
          tone = ButtonTone.Danger,
          icon = BloggoIcons.Trash,
          modifier = Modifier.fillMaxWidth().padding(top = if (isPushed) 20.dp else 12.dp),
        )
      }

      Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        BloggoButton("Cancel", { dismiss(onDismiss) }, tone = ButtonTone.Ghost)
        BloggoButton(
          "Save details",
          { dismiss { onSave(title, slug, date, tags, draft) } },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  if (confirmingDelete) {
    AlertDialog(
      onDismissRequest = { confirmingDelete = false },
      containerColor = colors.paperRaised,
      titleContentColor = colors.ink,
      textContentColor = colors.inkMuted,
      title = {
        Text(
          when {
            isFragmentPreview -> "Delete this capture?"
            post.kind == DocKind.Page -> "Delete this page?"
            else -> "Delete this draft?"
          },
          style = BloggoTheme.type.displaySmall,
        )
      },
      text = {
        Text(
          if (isPushed) {
            "\"${post.title}\" will be removed. This will also delete the file from GitHub. This can't be undone."
          } else {
            "\"${post.title}\" will be removed. This can't be undone."
          }
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmingDelete = false
            dismiss(if (isFragmentPreview) onDeleteFragment else onDelete)
          },
        ) {
          Text("Delete", style = BloggoTheme.type.button, color = colors.mark)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmingDelete = false }) {
          Text("Cancel", style = BloggoTheme.type.button, color = colors.inkMuted)
        }
      },
    )
  }
}

@Composable
private fun SheetField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  mono: Boolean = false,
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
      // Title/Slug/Date are all single-line frontmatter fields — a pasted or
      // Enter-inserted newline here would corrupt the `key: value` line it
      // lands in the same way a fragment's raw multi-line text used to (see
      // `newPostFromFragment` in BloggoApp.kt), silently breaking title
      // display and slug auto-sync for anything read back out of it.
      singleLine = true,
      textStyle = if (mono) {
        BloggoTheme.type.monoField.copy(color = colors.ink)
      } else {
        BloggoTheme.type.body.copy(color = colors.ink)
      },
      cursorBrush = SolidColor(colors.accent),
      // Left unset before (KeyboardOptions.Default) — that omission is a
      // plausible contributor to the text-expander bug this fixes (see
      // slugSyncBaseline/SLUG_SYNC_DEBOUNCE_MS above for the primary fix).
      // Title is prose, so it gets the same capitalization/autocorrect as
      // the editor body; Slug/Date are technical mono fields (lowercase
      // slugs, ISO dates) where autocorrect would just add noise.
      keyboardOptions = if (mono) {
        KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false)
      } else {
        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = true)
      },
      modifier = Modifier.fillMaxWidth(),
    )
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft).padding(top = 14.dp))
  }
}

/** The TAGS field exactly as it sits in the sheet: label, then the editable
 * chips, sourced from a sample tag pool wider than what's already applied —
 * "hugo" and "notes" are there but unused, so the autocomplete has something
 * to actually offer. */
@Preview(name = "Tags field")
@Composable
private fun PostDetailsTagsPreview() {
  BloggoTheme {
    val colors = BloggoTheme.colors
    Column(Modifier.background(colors.paperRaised).padding(18.dp)) {
      Text(
        "TAGS",
        style = BloggoTheme.type.fieldLabel,
        color = colors.inkFaint,
        modifier = Modifier.padding(bottom = 7.dp),
      )
      var tags by remember { mutableStateOf(listOf("ai", "tooling", "craft")) }
      BloggoTagField(
        tags = tags,
        onTagsChange = { tags = it },
        suggestions = listOf("ai", "tooling", "craft", "hugo", "notes"),
      )
    }
  }
}
