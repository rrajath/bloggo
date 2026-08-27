package com.rrajath.bloggo.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Drives the real [BasicTextField][androidx.compose.foundation.text.BasicTextField]
 * through Compose's test APIs rather than synthetic ADB key events. Written
 * after `adb shell input text` proved unreliable for this exact scenario in
 * manual verification — it silently dropped a literal space mid-string, which
 * is exactly the kind of tooling artifact this test format doesn't have.
 */
class EditorScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val startMarkdown = "---\ntitle: Untitled\nslug: untitled\ndraft: true\n---\n\n"
  private var latestMarkdown = ""

  // The slug sync in EditorScreen is debounced (SLUG_SYNC_DEBOUNCE_MS = 500ms) so a title
  // keystroke lands as one edit instead of two — see that constant's doc comment. Compose's
  // waitForIdle() only waits for pending recomposition, not for a plain delay()-backed
  // coroutine to resume, so assertions here wait for the settled result explicitly instead.
  private fun waitForSlug(expected: String) {
    composeTestRule.waitUntil(timeoutMillis = 2_000) {
      latestMarkdown.substringAfter("slug: ").substringBefore("\n") == expected
    }
  }

  private fun setEditor(markdown: String = startMarkdown) {
    latestMarkdown = markdown
    val post = Post(slug = "untitled-1", title = "Untitled", state = PostState.Draft, markdown = markdown)
    composeTestRule.setContent {
      BloggoTheme {
        EditorScreen(
          post = post,
          imagePath = "static/images/",
          tagPool = emptyList(),
          connection = RepoConnection(),
          remoteSlugs = emptySet(),
          stagedMediaForPost = emptyList(),
          isPublishing = false,
          publishResult = null,
          pendingInsertImage = null,
          onMarkdownChange = { markdown, _ -> latestMarkdown = markdown },
          onBack = {},
          onPreview = {},
          onFocus = {},
          onReview = {},
          onToast = {},
          onCoverGenerated = {},
          onDeletePost = {},
          onMoveToInbox = {},
          onPublish = { _, _ -> },
          onInsertImage = {},
          onPendingInsertConsumed = {},
        )
      }
    }
  }

  @Test
  fun typingIntoTheTitleUpdatesTheAutoTrackedSlugOnceTypingSettles() {
    setEditor()
    val titleEnd = startMarkdown.indexOf("title: Untitled") + "title: Untitled".length
    val field = composeTestRule.onNodeWithTag("editorMarkdownField")
    field.performTextInputSelection(TextRange(titleEnd))

    field.performTextInput("X")
    waitForSlug("untitledx")

    field.performTextInput("YZ")
    waitForSlug("untitledxyz")

    field.performTextInput(" More Words")
    waitForSlug("untitledxyz-more-words")
  }

  @Test
  fun editingSomewhereElseInTheFrontmatterDoesNotTouchTheSlug() {
    setEditor()
    val field = composeTestRule.onNodeWithTag("editorMarkdownField")
    val draftLineStart = startMarkdown.indexOf("draft: true")
    field.performTextInputSelection(TextRange(draftLineStart + "draft: ".length))

    field.performTextInput("REPLACED ")
    // Outlast the debounce window (500ms) so a delayed sync would have landed if one were
    // wrongly triggered, then confirm the slug really is untouched.
    Thread.sleep(800)
    composeTestRule.waitForIdle()

    assertEquals("untitled", latestMarkdown.substringAfter("slug: ").substringBefore("\n"))
  }
}
