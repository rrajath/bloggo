package com.rrajath.bloggo.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * What the formatting toolbar does to the document.
 *
 * Split out from the screen so the selection arithmetic can be unit tested. Every
 * action is defined by what it does to a [TextFieldValue], including where the
 * caret ends up, because a formatting button that loses the caret is worse than
 * no button.
 */
sealed interface MarkdownAction {

  fun applyTo(value: TextFieldValue): TextFieldValue

  /**
   * Wraps the selection, or inserts [placeholder] and selects it if there is none.
   *
   * When there's a selection, the caret normally lands right after [after]. Pass
   * [afterCaretOffset] to land it inside [after] instead, e.g. Link uses it to drop the
   * caret right after "https://" so the placeholder URL is ready to be typed over.
   */
  data class Wrap(
    val before: String,
    val after: String,
    val placeholder: String,
    val afterCaretOffset: Int? = null,
  ) : MarkdownAction {
    override fun applyTo(value: TextFieldValue): TextFieldValue {
      val start = value.selection.min
      val end = value.selection.max
      val selected = value.text.substring(start, end)
      val content = selected.ifEmpty { placeholder }
      val text = value.text.substring(0, start) + before + content + after + value.text.substring(end)
      val contentStart = start + before.length
      return TextFieldValue(
        text = text,
        selection = if (selected.isEmpty()) {
          // No selection, so offer the placeholder ready to be typed over.
          TextRange(contentStart, contentStart + content.length)
        } else {
          TextRange(contentStart + content.length + (afterCaretOffset ?: after.length))
        },
      )
    }
  }

  /** Adds [prefix] to the start of the line the caret is on, once. */
  data class LinePrefix(val prefix: String) : MarkdownAction {
    override fun applyTo(value: TextFieldValue): TextFieldValue {
      val caret = value.selection.min
      val lineStart = value.text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
      if (value.text.startsWith(prefix, lineStart)) return value
      return TextFieldValue(
        text = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart),
        selection = TextRange(caret + prefix.length),
      )
    }
  }

  /**
   * Cycles the line the caret is on through heading levels: no marker -> `#` -> `##` ->
   * ... -> `######` -> back to `#`. Reads the line's existing marker (if any) rather than
   * blindly inserting a fixed level, so repeated taps step through all six levels.
   */
  data object CycleHeading : MarkdownAction {
    private val prefixRegex = Regex("^(#{1,6}) ")

    override fun applyTo(value: TextFieldValue): TextFieldValue {
      val caret = value.selection.min
      val lineStart = value.text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
      val lineEnd = value.text.indexOf('\n', lineStart).let { if (it == -1) value.text.length else it }
      val line = value.text.substring(lineStart, lineEnd)

      val match = prefixRegex.find(line)
      val currentLevel = match?.groupValues?.get(1)?.length ?: 0
      val nextLevel = if (currentLevel == 0 || currentLevel == 6) 1 else currentLevel + 1
      val oldPrefixLength = match?.value?.length ?: 0
      val newPrefix = "#".repeat(nextLevel) + " "

      val text = value.text.substring(0, lineStart) + newPrefix +
        value.text.substring(lineStart + oldPrefixLength)
      val delta = newPrefix.length - oldPrefixLength
      return TextFieldValue(text = text, selection = TextRange((caret + delta).coerceAtLeast(lineStart)))
    }
  }

  /** Appends a block at the end of the document, separated by a blank line. */
  data class AppendBlock(val block: String) : MarkdownAction {
    override fun applyTo(value: TextFieldValue): TextFieldValue {
      val trimmed = value.text.trimEnd()
      val text = "$trimmed\n\n$block\n"
      return TextFieldValue(text, TextRange(text.length))
    }
  }

  companion object {
    val Bold = Wrap("**", "**", "bold text")
    val Italic = Wrap("*", "*", "emphasis")
    val Code = Wrap("`", "`", "code")
    private const val LINK_URL_PLACEHOLDER = "https://"
    private val linkAfter = "]($LINK_URL_PLACEHOLDER)"
    val Link = Wrap(
      before = "[",
      after = linkAfter,
      placeholder = "link text",
      // Land the caret right after "https://" instead of past the closing ")".
      afterCaretOffset = linkAfter.indexOf(LINK_URL_PLACEHOLDER) + LINK_URL_PLACEHOLDER.length,
    )
    val Heading = CycleHeading
    val Quote = LinePrefix("> ")
    val OrderedList = LinePrefix("1. ")
    val Rule = AppendBlock("---")
    val CodeBlock = AppendBlock("```ts\nconst diff = await agent.propose();\n```")

    // Hugo shortcodes, read from layouts/shortcodes/ in the connected repo.
    val Callout = AppendBlock("{{< callout type=\"note\" >}}\nWorth saying out loud.\n{{< /callout >}}")
    val Aside = AppendBlock("{{< aside >}}\nA tangent that earns its place.\n{{< /aside >}}")

    fun figure(path: String, caption: String) =
      AppendBlock("{{< figure src=\"$path\" caption=\"$caption\" >}}")
  }
}
