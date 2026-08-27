package com.rrajath.bloggo.ui.inbox

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.rrajath.bloggo.designsystem.BloggoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * `requestFocusOnOpen` is how the "Capture a thought" app shortcut
 * (res/xml/shortcuts.xml, BloggoApp's `requestInboxFocus`) gets the capture
 * overlay opened and its field focused with the keyboard up, without every
 * ordinary visit to the Inbox tab doing the same. This drives that flag
 * directly, the way BloggoApp does, rather than going through the
 * shortcut/Activity plumbing that needs a real device.
 *
 * Since the capture composer moved from an always-visible inline field to a
 * FAB-triggered [CaptureSheet] overlay, "doesn't steal focus" now means the
 * overlay never opens at all on ordinary navigation — there is no longer an
 * always-present capture field sitting unfocused in the tree to assert
 * against.
 */
class InboxScreenFocusTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private var focusConsumedCount = 0

  private fun setInbox(requestFocusOnOpen: Boolean) {
    focusConsumedCount = 0
    composeTestRule.setContent {
      BloggoTheme {
        InboxScreen(
          fragments = emptyList(),
          onOpen = {},
          onCapture = {},
          requestFocusOnOpen = requestFocusOnOpen,
          onFocusConsumed = { focusConsumedCount++ },
        )
      }
    }
  }

  @Test
  fun shortcutLaunchFocusesTheCaptureFieldAndConsumesTheRequestOnce() {
    setInbox(requestFocusOnOpen = true)

    // waitForIdle() only waits for pending recomposition, not for the
    // deliberate delay() this screen uses to dodge the cold-launch window-
    // focus race — see InboxScreen's LaunchedEffect(Unit) — so wait for the
    // settled result explicitly instead, the same way EditorScreenTest's
    // waitForSlug() does for its own debounced coroutine.
    composeTestRule.waitUntil(timeoutMillis = 2_000) { focusConsumedCount == 1 }

    composeTestRule.onNodeWithTag("inboxCaptureField").assertIsFocused()
    assertEquals(1, focusConsumedCount)
  }

  @Test
  fun ordinaryTabNavigationNeverStealsFocusOrPopsTheKeyboard() {
    setInbox(requestFocusOnOpen = false)
    composeTestRule.waitForIdle()

    // The capture overlay never auto-opens on ordinary navigation, so its
    // field doesn't even exist in the tree to steal focus or pop the
    // keyboard — the FAB is the only way in.
    composeTestRule.onNodeWithTag("inboxCaptureField").assertDoesNotExist()
    assertEquals(0, focusConsumedCount)
  }
}
