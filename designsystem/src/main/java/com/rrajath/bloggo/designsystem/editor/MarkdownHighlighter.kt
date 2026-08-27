package com.rrajath.bloggo.designsystem.editor

/** What a stretch of markdown source is, so the theme can decide how it looks. */
enum class MarkdownRole {
  /** `##`, `**`, backticks, brackets: syntax the writer should see but not read. */
  Marker,
  Heading1,
  Heading2,
  Bold,
  Italic,
  Code,
  LinkText,
  LinkUrl,
  Quote,
  Bullet,
  /** A YAML (`---`) or TOML (`+++`) line inside the opening frontmatter block. */
  Frontmatter,
  /** The key half of a frontmatter line. */
  FrontmatterKey,
  /** ``` and the fenced content. */
  Fence,
  FenceContent,
  /** `{{< shortcode >}}` */
  Shortcode,
}

data class MarkdownSpan(val start: Int, val end: Int, val role: MarkdownRole) {
  init {
    require(start <= end) { "span runs backwards: $start..$end" }
  }
}

/**
 * Turns markdown source into styling spans.
 *
 * Crucially this only ever *describes* the existing characters. Nothing is
 * inserted, removed or reordered, because the prototype dims its markers rather
 * than hiding them. That is what lets the editor use [androidx.compose.ui.text.input.OffsetMapping.Identity]
 * and sidestep the offset arithmetic that makes rich markdown fields fragile.
 *
 * Pure and free of Compose types so it can be tested directly.
 */
object MarkdownHighlighter {

  private val codeRegex = Regex("`([^`\\n]+)`")
  private val linkRegex = Regex("\\[([^\\]\\n]*)]\\(([^)\\n]*)\\)")
  private val boldRegex = Regex("\\*\\*([^*\\n]+)\\*\\*")
  private val italicRegex = Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)")
  private val headingRegex = Regex("^(#{1,6})\\s(.*)$")
  private val quoteRegex = Regex("^(>\\s?)(.*)$")
  private val orderedRegex = Regex("^(\\d+\\.\\s)(.*)$")
  private val unorderedRegex = Regex("^([-*+]\\s)(.*)$")
  /** YAML's `key: value`. */
  private val yamlFrontmatterKeyRegex = Regex("^([\\w-]+):")
  /** TOML's `key = value` (and `key = ["a", "b"]`, `key = 1`, etc.) — the
   * separator carries optional surrounding space that YAML's colon never has. */
  private val tomlFrontmatterKeyRegex = Regex("^([\\w-]+)\\s*=")

  /** The length of a heading line's marker prefix (the `#`/`##`/… plus the
   * single space after it), or `null` if [line] isn't a heading. Shared with
   * the editor's heading-only auto-capitalization, which needs the same
   * "is this a heading, and where does its text start" answer this class
   * already computes for syntax highlighting. */
  fun headingMarkerLength(line: String): Int? =
    headingRegex.find(line)?.let { it.groupValues[1].length + 1 }

  fun spans(source: String): List<MarkdownSpan> {
    val out = mutableListOf<MarkdownSpan>()
    var lineStart = 0
    var inFrontmatter = false
    var frontmatterClosed = false
    // Which literal fence opened the block, so the close check and the key
    // regex both match the format that was actually opened rather than
    // assuming YAML. Hugo accepts either --- (YAML) or +++ (TOML) here.
    var frontmatterFence: String? = null
    var inFence = false

    source.split("\n").forEachIndexed { index, line ->
      val end = lineStart + line.length
      val trimmed = line.trim()

      when {
        // Only a --- or +++ on the very first line opens frontmatter; anywhere
        // else it is a horizontal rule (or, for +++, just body text), which is
        // a different thing entirely.
        index == 0 && (trimmed == "---" || trimmed == "+++") -> {
          inFrontmatter = true
          frontmatterFence = trimmed
          out += MarkdownSpan(lineStart, end, MarkdownRole.Fence)
        }

        inFrontmatter && trimmed == frontmatterFence -> {
          inFrontmatter = false
          frontmatterClosed = true
          out += MarkdownSpan(lineStart, end, MarkdownRole.Fence)
        }

        inFrontmatter -> {
          out += MarkdownSpan(lineStart, end, MarkdownRole.Frontmatter)
          val keyRegex = if (frontmatterFence == "+++") tomlFrontmatterKeyRegex else yamlFrontmatterKeyRegex
          keyRegex.find(line)?.let {
            out += MarkdownSpan(lineStart, lineStart + it.groupValues[1].length, MarkdownRole.FrontmatterKey)
          }
        }

        trimmed.startsWith("```") -> {
          inFence = !inFence
          out += MarkdownSpan(lineStart, end, MarkdownRole.Fence)
        }

        inFence -> out += MarkdownSpan(lineStart, end, MarkdownRole.FenceContent)

        trimmed.startsWith("{{<") || trimmed.startsWith("{{%") ->
          out += MarkdownSpan(lineStart, end, MarkdownRole.Shortcode)

        else -> blockSpans(line, lineStart, out)
      }

      lineStart = end + 1
    }

    // An unterminated frontmatter block is a mistake worth seeing, so leave the
    // lines styled as frontmatter rather than silently reinterpreting them.
    if (inFrontmatter && !frontmatterClosed) Unit

    return out
  }

  private fun blockSpans(line: String, base: Int, out: MutableList<MarkdownSpan>) {
    headingRegex.find(line)?.let { match ->
      val hashes = match.groupValues[1]
      val markerEnd = base + hashes.length + 1
      out += MarkdownSpan(base, markerEnd, MarkdownRole.Marker)
      out += MarkdownSpan(
        markerEnd,
        base + line.length,
        if (hashes.length == 1) MarkdownRole.Heading1 else MarkdownRole.Heading2,
      )
      inlineSpans(line, base, out, from = hashes.length + 1)
      return
    }

    quoteRegex.find(line)?.let { match ->
      val marker = match.groupValues[1]
      out += MarkdownSpan(base, base + marker.length, MarkdownRole.Marker)
      out += MarkdownSpan(base + marker.length, base + line.length, MarkdownRole.Quote)
      inlineSpans(line, base, out, from = marker.length)
      return
    }

    val listMatch = orderedRegex.find(line) ?: unorderedRegex.find(line)
    if (listMatch != null) {
      val marker = listMatch.groupValues[1]
      out += MarkdownSpan(base, base + marker.length, MarkdownRole.Bullet)
      inlineSpans(line, base, out, from = marker.length)
      return
    }

    inlineSpans(line, base, out)
  }

  /**
   * Inline styling, applied in precedence order.
   *
   * Code wins over everything, so asterisks inside a code span stay literal.
   * Each match claims its characters, and later patterns skip anything claimed.
   */
  private fun inlineSpans(line: String, base: Int, out: MutableList<MarkdownSpan>, from: Int = 0) {
    if (from >= line.length) return
    // Every inline pattern below needs one of these three characters to match at
    // all, and most lines of prose contain none of them. One scan of the line
    // rules them all out, instead of four regex passes plus a BooleanArray the
    // size of the line — on every line of the document, on every keystroke.
    if (!hasInlineMarker(line, from)) return
    val claimed = BooleanArray(line.length)

    fun free(range: IntRange): Boolean =
      range.first >= from && range.all { !claimed[it] }

    fun claim(range: IntRange) = range.forEach { claimed[it] = true }

    codeRegex.findAll(line).forEach { match ->
      if (!free(match.range)) return@forEach
      claim(match.range)
      val open = match.range.first
      val close = match.range.last
      out += MarkdownSpan(base + open, base + open + 1, MarkdownRole.Marker)
      out += MarkdownSpan(base + open + 1, base + close, MarkdownRole.Code)
      out += MarkdownSpan(base + close, base + close + 1, MarkdownRole.Marker)
    }

    linkRegex.findAll(line).forEach { match ->
      if (!free(match.range)) return@forEach
      claim(match.range)
      val text = match.groups[1]!!.range
      val url = match.groups[2]!!.range
      // [
      out += MarkdownSpan(base + match.range.first, base + text.first, MarkdownRole.Marker)
      out += MarkdownSpan(base + text.first, base + text.last + 1, MarkdownRole.LinkText)
      // ](
      out += MarkdownSpan(base + text.last + 1, base + url.first, MarkdownRole.Marker)
      out += MarkdownSpan(base + url.first, base + url.last + 1, MarkdownRole.LinkUrl)
      // )
      out += MarkdownSpan(base + url.last + 1, base + match.range.last + 1, MarkdownRole.Marker)
    }

    boldRegex.findAll(line).forEach { match ->
      if (!free(match.range)) return@forEach
      claim(match.range)
      val open = match.range.first
      val close = match.range.last
      out += MarkdownSpan(base + open, base + open + 2, MarkdownRole.Marker)
      out += MarkdownSpan(base + open + 2, base + close - 1, MarkdownRole.Bold)
      out += MarkdownSpan(base + close - 1, base + close + 1, MarkdownRole.Marker)
    }

    italicRegex.findAll(line).forEach { match ->
      if (!free(match.range)) return@forEach
      claim(match.range)
      val open = match.range.first
      val close = match.range.last
      out += MarkdownSpan(base + open, base + open + 1, MarkdownRole.Marker)
      out += MarkdownSpan(base + open + 1, base + close, MarkdownRole.Italic)
      out += MarkdownSpan(base + close, base + close + 1, MarkdownRole.Marker)
    }
  }

  /** Whether [line] carries an opening character for any inline pattern at or
   * after [from]: a backtick (code), `[` (link) or `*` (bold, italic). */
  private fun hasInlineMarker(line: String, from: Int): Boolean {
    for (index in from until line.length) {
      when (line[index]) {
        '`', '[', '*' -> return true
      }
    }
    return false
  }
}
