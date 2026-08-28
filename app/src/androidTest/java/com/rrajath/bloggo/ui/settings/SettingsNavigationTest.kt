package com.rrajath.bloggo.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.ui.review.ReadabilityCheck
import org.junit.Rule
import org.junit.Test

/**
 * The Settings tab is a menu of sub-pages now. This drives the same
 * menu -> detail -> back path BloggoApp wires (`go(Route.SettingsDetail(...))`
 * / `::back`) through a minimal back-stack harness, without the Activity and
 * repository plumbing the real shell needs.
 */
class SettingsNavigationTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun setSettings() {
    composeTestRule.setContent {
      BloggoTheme {
        var page by remember { mutableStateOf<SettingsPage?>(null) }
        when (page) {
          null -> SettingsScreen(
            connection = RepoConnection(),
            publishedCount = 3,
            draftCount = 1,
            openPullRequestCount = 0,
            checkResult = null,
            appVersion = "1.1.0 (debug)",
            onOpenPage = { page = it },
          )

          SettingsPage.Connection -> SettingsConnectionScreen(
            connection = RepoConnection(),
            storedToken = null,
            checkResult = null,
            isChecking = false,
            onSave = { _, _, _, _, _ -> },
            onClearToken = {},
            onVisitSite = {},
            onBack = { page = null },
          )

          SettingsPage.Readability -> SettingsReadabilityScreen(
            readabilityChecks = ReadabilityCheck.All,
            onReadabilityChecksChange = {},
            onBack = { page = null },
          )

          else -> Unit
        }
      }
    }
  }

  @Test
  fun menuShowsTheVersionFooterAndNavigatesIntoGitHubConnectionAndBack() {
    setSettings()

    // Version footer.
    composeTestRule.onNodeWithText("1.1.0 (debug)").assertIsDisplayed()

    // Menu row -> detail page.
    composeTestRule.onNodeWithText("GitHub Connection").performClick()
    composeTestRule.waitForIdle()

    // A placeholder that only the Connection detail page renders.
    composeTestRule.onNodeWithText("username/blog").assertIsDisplayed()

    // Back returns to the menu.
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Repo Settings").assertIsDisplayed()
  }

  @Test
  fun readabilityDetailPageRendersItsSwitches() {
    setSettings()

    composeTestRule.onNodeWithText("Readability Review").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Passive voice").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Import / Export").assertIsDisplayed()
  }
}
