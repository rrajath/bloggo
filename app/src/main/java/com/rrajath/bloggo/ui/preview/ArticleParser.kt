package com.rrajath.bloggo.ui.preview

import com.rrajath.bloggo.model.markdownWordCount
import com.rrajath.bloggo.model.parseFrontmatter

/**
 * A markdown link found inside a block's [text][InlineLink], expressed as a
 * character range so the renderer can reconstruct an `AnnotatedString` without
 * this file needing to depend on Compose.
 */
data class InlineLink(val text: String, val url: String, val start: Int, val end: Int)

/** The inline emphasis a run of a block's text carries. */
enum class InlineStyle { Bold, Italic, Code }

/**
 * A run of emphasis (bold/italic/inline-code) found inside a block's text,
 * expressed as a character range for the same reason [InlineLink] is: so the
 * renderer can turn it into a styled `AnnotatedString` span without this file
 * depending on Compose.
 */
data class InlineSpan(val style: InlineStyle, val start: Int, val end: Int)

/**
 * A rendered article, block by block. Inline markdown syntax (`**bold**`,
 * `*italic*`, `` `code` ``) is stripped from [text][ArticleBlock.Paragraph.text]
 * down to plain characters; [spans][ArticleBlock.Paragraph.spans] records where
 * that emphasis applied so the renderer can style it back in.
 */
sealed interface ArticleBlock {
  data class Heading(
    val level: Int,
    val text: String,
    val links: List<InlineLink> = emptyList(),
    val spans: List<InlineSpan> = emptyList(),
  ) : ArticleBlock
  data class Paragraph(
    val text: String,
    val links: List<InlineLink> = emptyList(),
    val spans: List<InlineSpan> = emptyList(),
  ) : ArticleBlock
  data class Quote(
    val text: String,
    val links: List<InlineLink> = emptyList(),
    val spans: List<InlineSpan> = emptyList(),
  ) : ArticleBlock
  data class ListItem(
    val marker: String,
    val text: String,
    val links: List<InlineLink> = emptyList(),
    val spans: List<InlineSpan> = emptyList(),
  ) : ArticleBlock
  data class Callout(
    val text: String,
    val links: List<InlineLink> = emptyList(),
    val spans: List<InlineSpan> = emptyList(),
  ) : ArticleBlock
  data class Figure(val src: String, val caption: String) : ArticleBlock
  data class Video(val src: String) : ArticleBlock
  data class CodeBlock(val code: String, val language: String) : ArticleBlock
  data object Rule : ArticleBlock
}

data class Article(
  val frontmatter: Map<String, String>,
  val blocks: List<ArticleBlock>,
  val wordCount: Int,
)

/**
 * Markdown to blocks, for Read mode.
 *
 * Deliberately small. It handles what the app itself writes plus what a Hugo post
 * normally contains, and nothing else. A full CommonMark implementation is the
 * right call the moment posts start using tables or footnotes; until then this is
 * a hundred lines instead of a dependency.
 *
 * Hugo shortcodes are rendered as blocks rather than printed, since printing raw
 * template syntax in a preview makes the preview useless.
 */
object ArticleParser {

  private val headingRegex = Regex("^(#{1,6})\\s(.*)$")
  private val quoteRegex = Regex("^>\\s?(.*)$")
  private val orderedRegex = Regex("^(\\d+)\\.\\s(.*)$")
  private val unorderedRegex = Regex("^[-*+]\\s(.*)$")
  private val ruleRegex = Regex("^-{3,}$")
  private val imageRegex = Regex("^!\\[([^\\]]*)]\\(([^)]*)\\)$")
  private val codeFenceRegex = Regex("^```(\\w*)$")
  private val shortcodeOpen = Regex("^\\{\\{<\\s*(\\w+)([^>]*)>\\}\\}$")
  private val shortcodeClose = Regex("^\\{\\{<\\s*/(\\w+)\\s*>\\}\\}$")
  private val attrRegex = Regex("(\\w+)=\"([^\"]*)\"")

  // Compiling a Pattern is not free, and `inline()` runs once per paragraph,
  // heading, quote, list item and callout — a long article was compiling these
  // four hundreds of times per parse. Hoisted for the same reason the block
  // patterns above already were.
  //
  // A single alternation, tried left to right at each position, replaces what
  // used to be four sequential `.replace(...)` passes: bold has to be tried
  // before italic so `**x**` doesn't get read as `*` + literal `*x*` + `*`,
  // and code/link have no such ordering constraint but live here anyway so
  // one `findAll` walks the whole block once.
  private val inlineTokenRegex = Regex(
    "\\*\\*([^*]+)\\*\\*" +           // 1: **bold**
      "|__([^_]+)__" +                 // 2: __bold__
      "|(?<!\\*)\\*([^*]+)\\*(?!\\*)" + // 3: *italic*
      "|(?<!_)_([^_]+)_(?!_)" +        // 4: _italic_
      "|`([^`]+)`" +                   // 5: `code`
      "|\\[([^\\]]+)]\\(([^)]*)\\)"    // 6/7: [text](url)
  )

  fun parse(source: String): Article {
    val lines = source.split("\n")
    var index = 0
    val frontmatter = mutableMapOf<String, String>()

    val fence = lines.firstOrNull()?.trim()
    if (fence == "---" || fence == "+++") {
      frontmatter.putAll(source.parseFrontmatter())
      index = 1
      while (index < lines.size && lines[index].trim() != fence) index++
      index++
    }

    val blocks = mutableListOf<ArticleBlock>()
    val paragraph = StringBuilder()
    var calloutDepth = 0
    val callout = StringBuilder()

    fun flushParagraph() {
      if (paragraph.isNotEmpty()) {
        val parsed = inline(paragraph.toString().trim())
        blocks += ArticleBlock.Paragraph(parsed.text, parsed.links, parsed.spans)
        paragraph.clear()
      }
    }

    while (index < lines.size) {
      val line = lines[index]
      val trimmed = line.trim()
      index++

      if (shortcodeClose.matches(trimmed)) {
        if (calloutDepth > 0) {
          calloutDepth--
          val parsed = inline(callout.toString().trim())
          blocks += ArticleBlock.Callout(parsed.text, parsed.links, parsed.spans)
          callout.clear()
        }
        continue
      }

      val open = shortcodeOpen.find(trimmed)
      if (open != null) {
        flushParagraph()
        val name = open.groupValues[1]
        val attributes = attrRegex.findAll(open.groupValues[2])
          .associate { it.groupValues[1] to it.groupValues[2] }
        // `figure` and `video` are self-closing — there is no matching close
        // tag to wait for, so each renders as its own block immediately.
        // Every other shortcode name is assumed paired (`callout`, `aside`),
        // and its content accumulates until shortcodeClose is hit.
        when (name) {
          "figure" -> blocks += ArticleBlock.Figure(
            src = attributes["src"].orEmpty(),
            caption = attributes["caption"].orEmpty(),
          )
          "video" -> blocks += ArticleBlock.Video(src = attributes["src"].orEmpty())
          else -> calloutDepth++
        }
        continue
      }

      if (calloutDepth > 0) {
        if (trimmed.isNotEmpty()) callout.append(trimmed).append(' ')
        continue
      }

      // Each block shape is matched once and its groups reused. The earlier
      // `matches(...) -> find(...)!!` pairs scanned every matching line twice
      // with the same anchored pattern, and needed a `!!` to re-assert what the
      // first scan had already established.
      if (trimmed.isEmpty()) {
        flushParagraph()
        continue
      }

      // Fenced code is captured verbatim, line by line, so nothing inside it
      // (backticks, asterisks, `#` at line-start) is ever run back through
      // `inline()` or matched as some other block.
      val codeFence = codeFenceRegex.find(trimmed)
      if (codeFence != null) {
        flushParagraph()
        val language = codeFence.groupValues[1]
        val codeLines = mutableListOf<String>()
        while (index < lines.size && lines[index].trim() != "```") {
          codeLines += lines[index]
          index++
        }
        index++
        blocks += ArticleBlock.CodeBlock(codeLines.joinToString("\n"), language)
        continue
      }

      val heading = headingRegex.find(trimmed)
      if (heading != null) {
        flushParagraph()
        val parsed = inline(heading.groupValues[2])
        blocks += ArticleBlock.Heading(heading.groupValues[1].length, parsed.text, parsed.links, parsed.spans)
        continue
      }

      val image = imageRegex.find(trimmed)
      if (image != null) {
        flushParagraph()
        blocks += ArticleBlock.Figure(src = image.groupValues[2], caption = image.groupValues[1])
        continue
      }

      if (ruleRegex.matches(trimmed)) {
        flushParagraph()
        blocks += ArticleBlock.Rule
        continue
      }

      val quote = quoteRegex.find(trimmed)
      if (quote != null) {
        flushParagraph()
        val quoteLines = mutableListOf(quote.groupValues[1])
        while (index < lines.size) {
          val continuation = quoteRegex.find(lines[index].trim()) ?: break
          quoteLines += continuation.groupValues[1]
          index++
        }
        val parsedQuote = inline(quoteLines.joinToString(" "))
        blocks += ArticleBlock.Quote(parsedQuote.text, parsedQuote.links, parsedQuote.spans)
        continue
      }

      val ordered = orderedRegex.find(trimmed)
      if (ordered != null) {
        flushParagraph()
        val parsed = inline(ordered.groupValues[2])
        blocks += ArticleBlock.ListItem("${ordered.groupValues[1]}.", parsed.text, parsed.links, parsed.spans)
        continue
      }

      val unordered = unorderedRegex.find(trimmed)
      if (unordered != null) {
        flushParagraph()
        val parsed = inline(unordered.groupValues[1])
        blocks += ArticleBlock.ListItem("—", parsed.text, parsed.links, parsed.spans)
        continue
      }

      paragraph.append(line).append(' ')
    }
    flushParagraph()
    // A paired shortcode this parser doesn't recognize by name (anything but
    // `figure`/`video`) that turns out to have no closing tag would otherwise
    // hold everything after it hostage in `callout`, discarded once the loop
    // above runs out of lines. Flushing whatever was collected is not a
    // faithful render of that content, but it beats losing the rest of the
    // article to an unmatched open tag.
    if (callout.isNotEmpty()) {
      val parsed = inline(callout.toString().trim())
      blocks += ArticleBlock.Callout(parsed.text, parsed.links, parsed.spans)
    }

    return Article(frontmatter, blocks, source.markdownWordCount())
  }

  private data class InlineResult(
    val text: String,
    val links: List<InlineLink>,
    val spans: List<InlineSpan>,
  )

  /**
   * Resolves inline markdown syntax down to plain text plus the ranges it
   * applied to: `**bold**`/`__bold__` and `*italic*`/`_italic_` become
   * [InlineSpan]s and `` `code` `` becomes an [InlineSpan] of its own, while
   * `[text](url)` resolves to its display text with the URL recorded as an
   * [InlineLink]. Both kinds of range are expressed in terms of the returned
   * plain text so the renderer (which does depend on Compose) can turn them
   * into `AnnotatedString` spans without this file needing to.
   */
  private fun inline(text: String): InlineResult {
    val result = StringBuilder()
    val links = mutableListOf<InlineLink>()
    val spans = mutableListOf<InlineSpan>()
    var last = 0

    for (match in inlineTokenRegex.findAll(text)) {
      result.append(text, last, match.range.first)
      val start = result.length
      val groups = match.groups

      when {
        groups[1] != null || groups[2] != null -> {
          result.append(groups[1]?.value ?: groups[2]!!.value)
          spans += InlineSpan(InlineStyle.Bold, start, result.length)
        }
        groups[3] != null || groups[4] != null -> {
          result.append(groups[3]?.value ?: groups[4]!!.value)
          spans += InlineSpan(InlineStyle.Italic, start, result.length)
        }
        groups[5] != null -> {
          result.append(groups[5]!!.value)
          spans += InlineSpan(InlineStyle.Code, start, result.length)
        }
        else -> {
          val linkText = groups[6]!!.value
          val url = groups[7]!!.value
          result.append(linkText)
          links += InlineLink(linkText, url, start, result.length)
        }
      }
      last = match.range.last + 1
    }
    result.append(text, last, text.length)

    return InlineResult(result.toString(), links, spans)
  }
}
