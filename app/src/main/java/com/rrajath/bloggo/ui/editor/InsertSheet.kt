package com.rrajath.bloggo.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.Cell
import com.rrajath.bloggo.designsystem.component.CellGroup
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import kotlinx.coroutines.launch

/**
 * The editor's Insert row: Image, Code block, Divider — the three rows the
 * HTML prototype's own Insert sheet has that don't depend on reading the
 * repo. The prototype's fourth section, shortcodes detected from
 * `layouts/shortcodes/`, isn't built yet; [MarkdownAction.Callout]/[MarkdownAction.Aside]
 * already exist for whenever it is, just not wired to a row here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertSheet(
  onDismiss: () -> Unit,
  onInsertImage: () -> Unit,
  onFormat: (MarkdownAction) -> Unit,
) {
  val colors = BloggoTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  fun dismiss(after: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) after() }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.paperRaised,
    contentColor = colors.ink,
  ) {
    Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
      Text("Insert", style = BloggoTheme.type.displaySmall, modifier = Modifier.padding(bottom = 12.dp))
      CellGroup {
        Cell(
          title = "Image",
          subtitle = "From the media library or camera",
          icon = BloggoIcons.Image,
          onClick = { dismiss(onInsertImage) },
        )
        Cell(
          title = "Code block",
          subtitle = "Fenced, with a language hint",
          icon = BloggoIcons.Code,
          onClick = { dismiss { onFormat(MarkdownAction.CodeBlock) } },
        )
        Cell(
          title = "Divider",
          subtitle = "Section break",
          icon = BloggoIcons.Minus,
          onClick = { dismiss { onFormat(MarkdownAction.Rule) } },
          showDivider = false,
        )
      }
      Text(
        "Shortcodes detected from your repo aren't available here yet.",
        style = BloggoTheme.type.cellSubtitle,
        color = colors.inkFaint,
        modifier = Modifier.padding(top = 14.dp, start = 2.dp, end = 2.dp),
      )
    }
  }
}
