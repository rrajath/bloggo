package com.rrajath.bloggo.ui.editor

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Two bug reports, both about the raw-markdown [BasicTextField][androidx.compose.foundation.text.BasicTextField]
 * under real, back-to-back keystrokes with no idle time in between (which is what a physical/IME
 * key-repeat burst or fast typing actually looks like -- unlike [EditorScreenTest], which calls
 * `waitForIdle()` after every input and so never exercises this):
 *
 * 1. Press-and-hold backspace gets stuck after a letter or two.
 * 2. Fast, one-keystroke-per-character typing drops some characters.
 *
 * Investigation (on an emulator, and cross-checked with on-device `KeyEvent` timing) found no
 * point where [EditorScreen] itself ever loses or reorders a character -- `BasicTextField`'s
 * `EditProcessor` accumulates edits on its own internal buffer regardless of how slow
 * recomposition is, so these tests correctly find nothing to fail on that front. What they *did*
 * catch, and what these tests guard against regressing: `EditorScreen` was doing meaningfully
 * more per-keystroke work than it needed to --
 * [com.rrajath.bloggo.model.markdownWordCount] was computed twice per edit (once here for the
 * toolbar, once again by the caller), and every `onValueChange` -- including pure cursor moves
 * and selections, not just real edits -- fired the caller's `onMarkdownChange`, which in
 * `BloggoApp.kt` re-sorted and re-filtered the *entire* post list on every keystroke regardless
 * of which screen was even visible (the same "recomputed in full on every keystroke" shape
 * Milestone 10, PROGRESS.md, already fixed once for `repoConfig`). Both are fixed here.
 *
 * On a document around ANDROID_TDD.md §8.3's own stated worst case (~2,000 words), measured
 * per-keystroke cost is still well past a 16ms frame budget even after that fix -- the
 * dominant remaining cost is `BasicTextField`'s full, non-incremental re-layout of the whole
 * document on every keystroke, compounded by a heavily multi-span `AnnotatedString` from live
 * syntax highlighting (confirmed by benchmarking a plain `BasicTextField` with
 * `VisualTransformation.None` on the identical document: still tens of ms/keystroke on its own).
 * That is the "measure before optimizing" ANDROID_TDD.md §8.3/§14 already flagged, now measured;
 * closing it needs windowing the highlighter or migrating off the legacy `TextFieldValue`-based
 * `BasicTextField`, both real redesigns out of scope for this pass. The generous timing budget
 * below is a tripwire against a *gross* regression on top of that already-known ceiling, not a
 * claim that per-keystroke cost is fully fixed for very large posts.
 *
 * These tests fire real [KeyEvent]s -- the same code path
 * [androidx.compose.foundation.text.TextFieldKeyInput] uses for both hardware keyboards and the
 * software keyboards that go through it directly -- back to back with no `waitForIdle()` between
 * them, which is what makes them a meaningful stand-in for "a burst of key-repeat events arrives
 * faster than the app can keep up."
 */
class EditorScreenRapidInputTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private var latestMarkdown = ""
  private var onMarkdownChangeCallCount = 0

  /** Roughly a 2,000-word post -- ANDROID_TDD.md §8.3's own stated worst case -- with the mix of
   * headings, emphasis, links and code the highlighter treats differently line to line. */
  private fun largeMarkdown(): String {
    val para = "Every agent demo ends at the moment of *generation*. The model writes " +
      "the code, the video cuts, everyone claps. Then comes **the part that decides** " +
      "whether any of it matters, see [the tooling notes](/notes/tooling) for more, " +
      "and `inline code` too.\n\n"
    val body = buildString { repeat(75) { append(para) } }
    return "---\ntitle: Bench\ndate: 2026-01-01\ntags: [a, b]\ndraft: true\n---\n\n" +
      "## A heading to type after\n\n" + body
  }

  private fun setEditor(markdown: String) {
    latestMarkdown = markdown
    onMarkdownChangeCallCount = 0
    val post = Post(slug = "bench", title = "Bench", state = PostState.Draft, markdown = markdown)
    composeTestRule.setContent {
      BloggoTheme {
        EditorScreen(
          post = post,
          tagPool = emptyList(),
          connection = RepoConnection(),
          remoteSlugs = emptySet(),
          stagedMediaForPost = emptyList(),
          isPublishing = false,
          publishResult = null,
          pendingInsertImage = null,
          onMarkdownChange = { markdown, _ ->
            onMarkdownChangeCallCount++
            latestMarkdown = markdown
          },
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
  }

  private fun downEvent(keyCode: Int) =
    KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, keyCode))

  /** A run of distinct lowercase letters, so a dropped or reordered character is unambiguous
   * (no repeated char could hide a loss by coincidence). */
  private val typedSequence = "thequickbrownfxjmpsoverlazydg"

  @Test
  fun rapidTypingWithNoIdleBetweenKeystrokesDropsNoCharacters() {
    setEditor(largeMarkdown())
    val field = composeTestRule.onNodeWithTag("editorMarkdownField")
    val insertAt = largeMarkdown().indexOf("A heading to type after") +
      "A heading to type after".length
    field.performTextInputSelection(TextRange(insertAt))
    composeTestRule.waitForIdle()

    val start = System.nanoTime()
    for (c in typedSequence) {
      val keyCode = NativeKeyEvent.keyCodeFromString("KEYCODE_${c.uppercaseChar()}")
      field.performKeyPress(downEvent(keyCode))
    }
    val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
    composeTestRule.waitForIdle()

    val headingLine = latestMarkdown.lineSequence().first { it.startsWith("## ") }
    assertEquals(
      "every character of a fast, uninterrupted keystroke burst must land -- none silently dropped",
      "## A heading to type after$typedSequence",
      headingLine,
    )
    // A tripwire against a *gross* regression on top of the already-known, already-documented
    // per-keystroke cost ceiling at this document size (class doc above) -- not a claim that
    // this is already fast enough.
    assertTrue(
      "typing $typedSequence (${typedSequence.length} keystrokes) into a ~2,000-word document " +
        "took ${elapsedMs}ms end to end -- something got much slower than the already-known cost",
      elapsedMs < 8_000,
    )
  }

  @Test
  fun rapidBackspaceWithNoIdleBetweenPressesDeletesExactlyAsManyCharactersAsPressed() {
    val markdown = largeMarkdown()
    setEditor(markdown)
    val field = composeTestRule.onNodeWithTag("editorMarkdownField")
    val cursorAt = markdown.indexOf("A heading to type after") + "A heading to type after".length
    field.performTextInputSelection(TextRange(cursorAt))
    composeTestRule.waitForIdle()

    val presses = 20
    val start = System.nanoTime()
    repeat(presses) { field.performKeyPress(downEvent(NativeKeyEvent.KEYCODE_DEL)) }
    val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
    composeTestRule.waitForIdle()

    val expectedHeading = "## " + "A heading to type after".dropLast(presses)
    val headingLine = latestMarkdown.lineSequence().first { it.startsWith("## A") || it == "## " }
    assertEquals(
      "a burst of $presses backspace presses with no gap between them must remove exactly " +
        "$presses characters -- getting 'stuck' mid-burst and leaving extra characters behind " +
        "is the bug this test guards against",
      expectedHeading,
      headingLine,
    )
    assertTrue(
      "deleting $presses characters from a ~2,000-word document took ${elapsedMs}ms end to end -- " +
        "something got much slower than the already-known cost",
      elapsedMs < 8_000,
    )
  }

  /** Every keystroke fires exactly one [onMarkdownChange][EditorScreen] call -- this is mostly a
   * sanity check that the rapid-fire loop above isn't coalescing or duplicating edits at the
   * `EditorScreen` boundary, which would hide a real drop behind an accidental compensating
   * duplicate. */
  @Test
  fun eachKeystrokeFiresExactlyOneMarkdownChangeCall() {
    val markdown = largeMarkdown()
    setEditor(markdown)
    val field = composeTestRule.onNodeWithTag("editorMarkdownField")
    val insertAt = markdown.indexOf("A heading to type after") + "A heading to type after".length
    field.performTextInputSelection(TextRange(insertAt))
    composeTestRule.waitForIdle()

    repeat(10) { field.performKeyPress(downEvent(NativeKeyEvent.keyCodeFromString("KEYCODE_X"))) }
    composeTestRule.waitForIdle()

    assertEquals(10, onMarkdownChangeCallCount)
  }
}
