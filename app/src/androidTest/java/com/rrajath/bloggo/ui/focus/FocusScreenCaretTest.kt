package com.rrajath.bloggo.ui.focus

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.rrajath.bloggo.designsystem.BloggoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Focus mode's editing state used to be keyed on the markdown itself.
 *
 * Since `onMarkdownChange` rewrites that markdown on every keystroke, the key
 * changed on every keystroke too, and the `remember` block rebuilt its
 * `TextFieldValue` with `TextRange(body.length)` — planting the caret at the end
 * of the document after every single character. Typing anywhere but the tail was
 * impossible, which defeats the entire mode.
 *
 * The state is keyed on the post's slug now. This test drives the screen the way
 * [com.rrajath.bloggo.BloggoApp] does — feeding each change straight back in as
 * the `markdown` parameter — because that feedback loop is what made the bug
 * appear at all.
 */
class FocusScreenCaretTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val frontmatter = "---\ntitle: Focus\ndate: 2026-01-01\n---\n\n"
  private val body = "First line of the body.\nSecond line of the body.\nThird line of the body."

  private var latestMarkdown = ""

  private fun setFocusScreen() {
    latestMarkdown = frontmatter + body
    composeTestRule.setContent {
      // Held across recompositions, exactly as BloggoApp's post list is.
      var markdown by remember { mutableStateOf(frontmatter + body) }
      BloggoTheme {
        FocusScreen(
          slug = "focus-caret",
          markdown = markdown,
          onMarkdownChange = { updated, _ ->
            markdown = updated
            latestMarkdown = updated
          },
          onExit = {},
        )
      }
    }
  }

  private fun downEvent(keyCode: Int) =
    KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, keyCode))

  @Test
  fun typingMidDocumentLeavesTheCaretWhereItWasInsteadOfJumpingToTheEnd() {
    setFocusScreen()
    val field = composeTestRule.onNodeWithTag("focusBodyField")
    // End of the first line — deliberately not the end of the document.
    val insertAt = body.indexOf('\n')
    field.performTextInputSelection(TextRange(insertAt))
    composeTestRule.waitForIdle()

    // Distinct characters, so a caret that resets between keystrokes scatters
    // them at the tail in a way no coincidence could reproduce.
    val typed = "abcdef"
    for (c in typed) {
      field.performKeyPress(downEvent(NativeKeyEvent.keyCodeFromString("KEYCODE_${c.uppercaseChar()}")))
    }
    composeTestRule.waitForIdle()

    assertEquals(
      "every character must land contiguously at the caret, not at the end of the document",
      frontmatter + body.substring(0, insertAt) + typed + body.substring(insertAt),
      latestMarkdown,
    )
  }

  @Test
  fun theFrontmatterIsStrippedForEditingAndRestoredOnEveryChange() {
    setFocusScreen()
    val field = composeTestRule.onNodeWithTag("focusBodyField")
    field.performTextInputSelection(TextRange(body.length))
    composeTestRule.waitForIdle()

    field.performKeyPress(downEvent(NativeKeyEvent.KEYCODE_Z))
    composeTestRule.waitForIdle()

    assertEquals(frontmatter + body + "z", latestMarkdown)
  }
}
