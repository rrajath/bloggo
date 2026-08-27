package com.rrajath.bloggo.ui.inbox

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** `- item`, `* item`, or an indented variant of either. */
private val bulletLineRegex = Regex("^(\\s*)([-*])( +)(.*)$")

/** `1. item`, `2. item`, … or an indented variant. */
private val numberedLineRegex = Regex("^(\\s*)(\\d+)\\.( +)(.*)$")

/**
 * List auto-continuation for the Inbox capture overlay: pressing Enter on a
 * bullet or numbered list line continues the list; pressing Enter on an
 * *empty* list line (just the marker, no text after it) removes that marker
 * instead — the usual "double Enter exits a list" behavior, collapsed to a
 * single Enter on an already-empty marker line, per the feature spec.
 *
 * Built on the same insertion-diff shape as `EditorScreen.kt`'s
 * `capitalizeHeadingFirstLetter`/`adjustSelectionForRewrite`: rather than
 * reacting to a raw keypress, this looks at the diff between the field's
 * previous and new value, and only acts when that diff is a single inserted
 * `\n` — never on a paste, an IME composition update, or a deletion, which
 * would otherwise misfire this on unrelated edits.
 */
internal fun continueListOnEnter(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
  val oldText = old.text
  val newText = new.text
  if (newText.length <= oldText.length) return new // not an insertion

  val maxPrefix = minOf(oldText.length, newText.length)
  var prefix = 0
  while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
  val maxSuffix = maxPrefix - prefix
  var suffix = 0
  while (suffix < maxSuffix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++
  val insertedEnd = newText.length - suffix

  // Only a bare, single Enter keypress triggers this — a paste or an IME
  // autocomplete replacing a whole word lands here too, and neither should
  // be treated as "the user pressed Enter on a list line."
  if (insertedEnd - prefix != 1 || newText[prefix] != '\n') return new

  val lineStart = newText.lastIndexOf('\n', prefix - 1).let { if (it == -1) 0 else it + 1 }
  val line = newText.substring(lineStart, prefix)

  val bulletMatch = bulletLineRegex.find(line)
  val numberedMatch = if (bulletMatch == null) numberedLineRegex.find(line) else null
  if (bulletMatch == null && numberedMatch == null) return new

  val (indent, marker, content) = when {
    bulletMatch != null -> Triple(bulletMatch.groupValues[1], "${bulletMatch.groupValues[2]} ", bulletMatch.groupValues[4])
    else -> {
      val next = numberedMatch!!.groupValues[2].toIntOrNull()?.plus(1) ?: 1
      Triple(numberedMatch.groupValues[1], "$next. ", numberedMatch.groupValues[4])
    }
  }

  return if (content.isBlank()) {
    // Empty marker line: pressing Enter exits the list instead of continuing
    // it. Removes the marker (and the indent before it) along with the
    // newline that was just inserted, leaving the cursor on a fresh, truly
    // empty line at the same spot.
    val rewritten = newText.substring(0, lineStart) + newText.substring(insertedEnd)
    TextFieldValue(rewritten, TextRange(lineStart), new.composition)
  } else {
    val rewritten = newText.substring(0, insertedEnd) + indent + marker + newText.substring(insertedEnd)
    val caret = insertedEnd + indent.length + marker.length
    TextFieldValue(rewritten, TextRange(caret), new.composition)
  }
}
