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
   * caret right after "https://" so the placeholder URL is ready to be typed over. Pass
   * [afterHighlightRange] instead to *select* a span inside [after] rather than just
   * park the caret in it, e.g. Link highlights a URL pasted in from the clipboard so
   * it can be replaced with a single keystroke if it's not wanted.
   */
  data class Wrap(
    val before: String,
    val after: String,
    val placeholder: String,
    val afterCaretOffset: Int? = null,
    val afterHighlightRange: IntRange? = null,
  ) : MarkdownAction {
    override fun applyTo(value: TextFieldValue): TextFieldValue {
      val start = value.selection.min
      val end = value.selection.max
      val selected = value.text.substring(start, end)
      val content = selected.ifEmpty { placeholder }
      val text = value.text.substring(0, start) + before + content + after + value.text.substring(end)
      val contentStart = start + before.length
      val afterStart = contentStart + content.length
      return TextFieldValue(
        text = text,
        selection = when {
          selected.isEmpty() ->
            // No selection, so offer the placeholder ready to be typed over.
            TextRange(contentStart, contentStart + content.length)
          afterHighlightRange != null ->
            TextRange(afterStart + afterHighlightRange.first, afterStart + afterHighlightRange.last + 1)
          else -> TextRange(afterStart + (afterCaretOffset ?: after.length))
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

    /** The literal scheme [Link] prefills into `]( … )` and lands the caret after. */
    const val LINK_URL_PREFILL = LINK_URL_PLACEHOLDER

    /**
     * The Link action to use for a selection, given what's on the clipboard.
     * A usable [clipboardUrl] replaces the usual "https://" prefill outright, with
     * the URL highlighted in the result instead of just parking the caret after it,
     * so a writer who doesn't want it can delete it with a single keystroke while
     * the keyboard is still up. With no usable URL on the clipboard, falls back to
     * [Link] itself: the bare scheme, caret parked ready to type.
     */
    fun linkFor(clipboardUrl: String?): Wrap {
      if (clipboardUrl == null) return Link
      val after = "]($clipboardUrl)"
      val urlStart = after.indexOf(clipboardUrl)
      return Wrap(
        before = "[",
        after = after,
        placeholder = "link text",
        afterHighlightRange = urlStart until (urlStart + clipboardUrl.length),
      )
    }
  }
}

/** Regex form of the same `http(s)://` scheme convention [MarkdownAction.Link] itself
 * prefills into a link. Requires the whole (trimmed) string to be one URL with no
 * embedded whitespace, so a multi-line or prose clipboard isn't mistaken for one. */
private val urlSchemeRegex = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

/** [text] (typically the clipboard's contents) as a URL, if it looks like exactly
 * one — see [urlSchemeRegex]. Null for anything else, including blank text. */
fun urlOrNull(text: String?): String? = text?.trim()?.takeIf { urlSchemeRegex.matches(it) }

/**
 * [MarkdownAction.Link] wraps a selection as `[text](https://)` and leaves the
 * caret right after the prefilled `https://`, ready for the URL to be typed or
 * pasted. If what gets pasted there already carries its own `http://` or
 * `https://`, the two stack up as `https://https://example.com`. When
 * [prefillEnd] marks the end of that prefilled scheme and [new] is an insertion
 * starting exactly there whose text begins with a scheme, drop the prefill so
 * the pasted URL stands alone. Any other edit is returned untouched.
 */
fun dedupePastedLinkScheme(old: TextFieldValue, new: TextFieldValue, prefillEnd: Int): TextFieldValue {
  val prefill = MarkdownAction.LINK_URL_PREFILL
  val oldText = old.text
  val newText = new.text
  if (newText.length <= oldText.length) return new
  if (prefillEnd < prefill.length || prefillEnd > oldText.length) return new
  // The characters just before prefillEnd must be the prefill this is meant to undo.
  if (!oldText.regionMatches(prefillEnd - prefill.length, prefill, 0, prefill.length)) return new
  // The edit must be a pure insertion that begins exactly at prefillEnd.
  val tail = oldText.length - prefillEnd
  if (!newText.regionMatches(0, oldText, 0, prefillEnd)) return new
  if (!newText.regionMatches(newText.length - tail, oldText, prefillEnd, tail)) return new
  val inserted = newText.substring(prefillEnd, newText.length - tail)
  val lower = inserted.lowercase()
  if (!lower.startsWith("https://") && !lower.startsWith("http://")) return new
  val cutStart = prefillEnd - prefill.length
  val rewritten = newText.substring(0, cutStart) + newText.substring(prefillEnd)
  val caret = (new.selection.max - prefill.length).coerceAtLeast(cutStart)
  return TextFieldValue(rewritten, TextRange(caret))
}
