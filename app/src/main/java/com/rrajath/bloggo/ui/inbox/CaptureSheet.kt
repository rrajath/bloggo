package com.rrajath.bloggo.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.ButtonTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The capture text area's height as a fraction of the screen — "about 30%",
 * per the feature spec, fixed rather than growing with content so the sheet
 * itself never has to fight the keyboard for room; overflow past this height
 * scrolls within the field instead (see the auto-scroll effect below). */
private const val CAPTURE_AREA_HEIGHT_FRACTION = 0.3f

/**
 * The Inbox tab's capture overlay, opened from its FAB (or, once, from the
 * "Capture a thought" app shortcut via [requestFocusOnOpen]) rather than an
 * always-visible inline field — see `InboxScreen.kt`'s own doc comment for
 * why.
 *
 * A [ModalBottomSheet], the same overlay mechanism [com.rrajath.bloggo.ui.sheet.PostDetailsSheet]
 * already uses, chosen over a plain full-screen [androidx.compose.ui.window.Dialog]
 * for three reasons: it is the one overlay pattern already established in this
 * app, so a second, differently-behaved popup shape would be its own
 * inconsistency; it anchors naturally to the bottom of the screen, which is
 * where a fixed-height text area plus a Save/Discard footer wants to sit; and
 * Material3's `ModalBottomSheet` already participates in the system's normal
 * IME-avoidance for its own window, so the keyboard opening pushes the sheet
 * (and this text area) up rather than covering it, with only an explicit
 * [Modifier.imePadding] here as a defensive backstop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
  modifier: Modifier = Modifier,
  // Set only when this sheet was opened by the "Capture a thought" app
  // shortcut (`BloggoApp`'s `requestInboxFocus`), never by an ordinary FAB
  // tap — mirrors the same one-shot request/consume shape `InboxScreen`
  // already used for its old inline field.
  requestFocusOnOpen: Boolean = false,
  onFocusConsumed: () -> Unit = {},
) {
  val colors = BloggoTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  var value by remember { mutableStateOf(TextFieldValue("")) }
  var confirmingDiscard by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val scrollState = rememberScrollState()
  val captureAreaHeight = LocalConfiguration.current.screenHeightDp.dp * CAPTURE_AREA_HEIGHT_FRACTION

  // As the writer types past the bottom of the fixed-height text area, keep
  // the cursor visible rather than letting new text disappear below the
  // fold — only while the cursor is actually at the end, so this never
  // fights a deliberate scroll-back-up to edit something already typed.
  LaunchedEffect(value) {
    if (value.selection.end >= value.text.length) {
      scrollState.animateScrollTo(scrollState.maxValue)
    }
  }

  LaunchedEffect(Unit) {
    if (requestFocusOnOpen) {
      // Same cold-launch window-focus race `InboxScreen`'s old inline field
      // worked around; see its own comment for why the defer is needed.
      delay(120)
      focusRequester.requestFocus()
      keyboardController?.show()
      onFocusConsumed()
    }
  }

  fun dismiss(after: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) after() }
  }

  fun requestDiscard() {
    if (value.text.isBlank()) {
      // Nothing to lose — no need to make the writer confirm throwing away
      // an empty draft.
      dismiss(onDismiss)
    } else {
      confirmingDiscard = true
    }
  }

  ModalBottomSheet(
    onDismissRequest = { requestDiscard() },
    sheetState = sheetState,
    containerColor = colors.paperRaised,
    contentColor = colors.ink,
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 18.dp).imePadding()) {
      Text(
        "Catch a thought",
        style = BloggoTheme.type.displaySmall,
        modifier = Modifier.padding(bottom = 12.dp),
      )

      Box(
        Modifier
          .fillMaxWidth()
          .height(captureAreaHeight)
          .verticalScroll(scrollState),
      ) {
        if (value.text.isEmpty()) {
          Text("What's on your mind?", style = BloggoTheme.type.body, color = colors.inkFaint)
        }
        BasicTextField(
          value = value,
          onValueChange = { value = continueListOnEnter(value, it) },
          textStyle = BloggoTheme.type.body.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.accent),
          // Prose, not a technical field — mirrors the editor body's own
          // config (EditorScreen.kt), minus its heading-specific manual
          // capitalization, which has no equivalent here: a captured
          // thought has no markdown heading concept.
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("inboxCaptureField"),
        )
      }

      Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        BloggoButton("Discard", { requestDiscard() }, tone = ButtonTone.Ghost)
        BloggoButton(
          "Save",
          {
            dismiss {
              onSave(value.text)
            }
          },
          enabled = value.text.isNotBlank(),
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  if (confirmingDiscard) {
    AlertDialog(
      onDismissRequest = { confirmingDiscard = false },
      containerColor = colors.paperRaised,
      titleContentColor = colors.ink,
      textContentColor = colors.inkMuted,
      title = { Text("Discard this thought?", style = BloggoTheme.type.displaySmall) },
      text = { Text("What you've typed will be lost. This can't be undone.") },
      confirmButton = {
        TextButton(onClick = { confirmingDiscard = false; dismiss(onDismiss) }) {
          Text("Discard", style = BloggoTheme.type.button, color = colors.mark)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmingDiscard = false }) {
          Text("Cancel", style = BloggoTheme.type.button, color = colors.inkMuted)
        }
      },
    )
  }
}
