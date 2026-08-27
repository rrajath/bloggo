package com.rrajath.bloggo.ui.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoFab
import com.rrajath.bloggo.designsystem.component.CaptureRow
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.Fragment

/**
 * Unfiled fragments.
 *
 * Capture used to be an always-visible inline field at the top of this
 * screen; it is now a FAB that opens [CaptureSheet], a fixed-height overlay
 * with its own Save/Discard step — catching a thought is still a couple of
 * taps away, but no longer eats screen space above the list on every visit,
 * and now gets room to actually see what was just typed (the old inline
 * field was one line tall). Voice capture is P2 and its control is present
 * but inert.
 */
@Composable
fun InboxScreen(
  fragments: List<Fragment>,
  onOpen: (Fragment) -> Unit,
  onCapture: (String) -> Unit,
  modifier: Modifier = Modifier,
  // Set only when this screen was reached via the "Capture a thought" app
  // shortcut, never by ordinary tab navigation — see BloggoApp's
  // requestInboxFocus. Consumed once via onFocusConsumed so re-entering
  // Inbox normally afterwards doesn't steal focus or pop the keyboard.
  requestFocusOnOpen: Boolean = false,
  onFocusConsumed: () -> Unit = {},
) {
  var showCapture by remember { mutableStateOf(false) }
  // The shortcut's one-shot focus request now means "open the capture sheet
  // and focus it," not "focus an always-visible field" — same one-shot shape,
  // just handed to CaptureSheet instead of consumed directly here.
  var pendingShortcutFocus by remember { mutableStateOf(requestFocusOnOpen) }

  LaunchedEffect(requestFocusOnOpen) {
    if (requestFocusOnOpen) {
      pendingShortcutFocus = true
      showCapture = true
    }
  }

  Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
      BloggoAppBar(
        title = "Inbox",
        subtitle = "${fragments.size} fragments · unfiled",
      )

      LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)) {
        fragments.groupBy { it.bucket }.forEach { (bucket, items) ->
          item(key = "header-$bucket") { Eyebrow(bucket) }
          items(items.size, key = { items[it].id }) { position ->
            val fragment = items[position]
            CaptureRow(
              text = fragment.text,
              time = fragment.capturedAt,
              tag = fragment.tag,
              onClick = { onOpen(fragment) },
            )
          }
        }
      }
    }

    BloggoFab(
      icon = BloggoIcons.Plus,
      contentDescription = "Catch a thought",
      onClick = { showCapture = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(18.dp),
    )
  }

  if (showCapture) {
    CaptureSheet(
      onDismiss = { showCapture = false },
      onSave = { text ->
        showCapture = false
        onCapture(text)
      },
      requestFocusOnOpen = pendingShortcutFocus,
      onFocusConsumed = {
        pendingShortcutFocus = false
        onFocusConsumed()
      },
    )
  }
}

@Preview(heightDp = 800)
@Composable
private fun InboxPreview() {
  BloggoTheme {
    InboxScreen(fragments = SampleData.fragments, onOpen = {}, onCapture = {})
  }
}
