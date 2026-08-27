package com.rrajath.bloggo.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme

/**
 * Editable tag chips, plus an "+ add" affordance that reveals an inline
 * autocomplete panel.
 *
 * [suggestions] is the writer's own tag pool — every distinct tag already used
 * across their posts — and must be computed once by the caller, not
 * recomputed here per keystroke; this composable only filters the list it is
 * given as the query changes. Filtering excludes tags already applied to this
 * field. A query that matches nothing offers a "create" row instead, adding a
 * brand-new tag rather than silently dropping what was typed.
 *
 * The chip look (rounded, tinted, a 60%-opacity `×`) and the dashed-look "+
 * add" affordance follow `.tag` / `.tag-add` in `bloggo-prototype.html`; the
 * autocomplete panel itself has no prototype counterpart to match, since the
 * prototype's `.tag-add` is inert.
 */
@Composable
fun BloggoTagField(
  tags: List<String>,
  onTagsChange: (List<String>) -> Unit,
  suggestions: List<String>,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  var adding by remember { mutableStateOf(false) }
  var query by remember { mutableStateOf("") }

  fun commit(tag: String) {
    val trimmed = tag.trim()
    if (trimmed.isNotEmpty() && tags.none { it.equals(trimmed, ignoreCase = true) }) {
      onTagsChange(tags + trimmed)
    }
    query = ""
    adding = false
  }

  Column(modifier) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      tags.forEach { tag ->
        BloggoChip(tag, ChipTone.PullRequest, dot = false, onRemove = { onTagsChange(tags - tag) })
      }
      Box(
        modifier = Modifier
          .clip(BloggoTheme.shapes.small)
          .border(1.dp, colors.rule, BloggoTheme.shapes.small)
          .clickable(role = Role.Button) { adding = !adding; query = "" }
          .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Text(if (adding) "− cancel" else "+ add", style = BloggoTheme.type.chip, color = colors.inkFaint)
      }
    }

    if (adding) {
      val matches = remember(suggestions, tags, query) {
        val used = tags.map { it.lowercase() }.toSet()
        suggestions.filter { it.lowercase() !in used && it.contains(query, ignoreCase = true) }
      }
      val exactMatch = matches.any { it.equals(query.trim(), ignoreCase = true) }

      Column(
        Modifier
          .fillMaxWidth()
          .padding(top = 8.dp)
          .clip(BloggoTheme.shapes.small)
          .background(colors.paperSunk)
          .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.small)
          .padding(4.dp),
      ) {
        BasicTextField(
          value = query,
          onValueChange = { query = it },
          textStyle = BloggoTheme.type.monoField.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.accent),
          // Left unset before (KeyboardOptions.Default) — that omission is a
          // plausible contributor to the text-expander bug this fixes. Tags
          // are keywords, not prose (monoField styling, like Slug/Date in
          // PostDetailsSheet), so no capitalization/autocorrect here either.
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
          ),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
          decorationBox = { inner ->
            if (query.isEmpty()) {
              Text("Search or create a tag", style = BloggoTheme.type.monoField, color = colors.inkFaint)
            }
            inner()
          },
        )

        matches.forEach { suggestion ->
          TagSuggestionRow(suggestion, onClick = { commit(suggestion) })
        }
        if (query.isNotBlank() && !exactMatch) {
          TagSuggestionRow("Create “${query.trim()}”", onClick = { commit(query) })
        }
      }
    }
  }
}

@Composable
private fun TagSuggestionRow(label: String, onClick: () -> Unit) {
  val colors = BloggoTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(BloggoTheme.shapes.small)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = BloggoTheme.type.cellTitle, color = colors.ink)
  }
}

@Preview
@Composable
private fun TagFieldPreview() {
  BloggoTheme {
    Column(Modifier.background(BloggoTheme.colors.paperRaised).padding(18.dp)) {
      BloggoTagField(
        tags = listOf("ai", "tooling", "craft"),
        onTagsChange = {},
        suggestions = listOf("ai", "tooling", "craft", "hugo", "writing", "notes"),
      )
    }
  }
}

/** The edge case that's easy to get wrong: no tags yet, and no tag pool to
 * suggest from either — a brand-new writer's first post. Only "+ add" and,
 * once tapped, the "create" row should be reachable. */
@Preview(name = "Empty, no pool")
@Composable
private fun TagFieldEmptyPreview() {
  BloggoTheme {
    Column(Modifier.background(BloggoTheme.colors.paperRaised).padding(18.dp)) {
      var tags by remember { mutableStateOf(emptyList<String>()) }
      BloggoTagField(tags = tags, onTagsChange = { tags = it }, suggestions = emptyList())
    }
  }
}
