package com.rrajath.bloggo.ui.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleParserTest {

  @Test
  fun `parses frontmatter into a map`() {
    val article = ArticleParser.parse(
      """
      ---
      title: A post
      tags: [a, b]
      ---

      Body text.
      """.trimIndent()
    )
    assertEquals("A post", article.frontmatter["title"])
    assertEquals("[a, b]", article.frontmatter["tags"])
  }

  @Test
  fun `parses TOML fenced frontmatter into the same map shape`() {
    val article = ArticleParser.parse(
      """
      +++
      title = "A TOML post"
      draft = true
      +++

      Body text.
      """.trimIndent()
    )
    assertEquals("A TOML post", article.frontmatter["title"])
    assertEquals("true", article.frontmatter["draft"])
    assertEquals(ArticleBlock.Paragraph("Body text."), article.blocks[0])
  }

  @Test
  fun `parses a heading and a paragraph`() {
    val article = ArticleParser.parse("## A heading\n\nA paragraph.")
    assertEquals(ArticleBlock.Heading(2, "A heading"), article.blocks[0])
    assertEquals(ArticleBlock.Paragraph("A paragraph."), article.blocks[1])
  }

  @Test
  fun `parses a quote and a rule`() {
    val article = ArticleParser.parse("> A quote\n\n---")
    assertEquals(ArticleBlock.Quote("A quote"), article.blocks[0])
    assertEquals(ArticleBlock.Rule, article.blocks[1])
  }

  @Test
  fun `merges consecutive quote lines into one block`() {
    val article = ArticleParser.parse("> Line one continues\n> onto line two.")
    assertEquals(1, article.blocks.size)
    assertEquals(ArticleBlock.Quote("Line one continues onto line two."), article.blocks[0])
  }

  @Test
  fun `parses ordered and unordered list items`() {
    val article = ArticleParser.parse("1. First\n- Second")
    assertEquals(ArticleBlock.ListItem("1.", "First"), article.blocks[0])
    assertEquals(ArticleBlock.ListItem("—", "Second"), article.blocks[1])
  }

  @Test
  fun `renders a hugo callout shortcode as a block`() {
    // Regression test: the shortcode regexes must compile on Android's ICU
    // regex engine, which (unlike desktop java.util.regex) rejects a bare
    // unescaped `}`. This exact input used to crash ArticleParser's class
    // init with a PatternSyntaxException on-device.
    val article = ArticleParser.parse(
      "{{< callout >}}\nSomething worth noticing.\n{{< /callout >}}"
    )
    assertEquals(1, article.blocks.size)
    val callout = article.blocks[0] as ArticleBlock.Callout
    assertEquals("Something worth noticing.", callout.text)
  }

  @Test
  fun `renders a hugo figure shortcode with attributes`() {
    val article = ArticleParser.parse(
      """{{< figure src="/images/x.png" caption="A caption" >}}"""
    )
    val figure = article.blocks[0] as ArticleBlock.Figure
    assertEquals("/images/x.png", figure.src)
    assertEquals("A caption", figure.caption)
  }

  @Test
  fun `strips inline emphasis and code markers down to plain text`() {
    val article = ArticleParser.parse("**bold** *italic* `code` [text](url)")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("bold italic code text", paragraph.text)
  }

  @Test
  fun `records a bold span for double-asterisk and double-underscore emphasis`() {
    val article = ArticleParser.parse("**bold** and __also bold__")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("bold and also bold", paragraph.text)
    assertEquals(
      listOf(
        InlineSpan(InlineStyle.Bold, 0, 4),
        InlineSpan(InlineStyle.Bold, 9, 18),
      ),
      paragraph.spans,
    )
  }

  @Test
  fun `records an italic span for single-asterisk and single-underscore emphasis`() {
    val article = ArticleParser.parse("*italic* and _also italic_")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("italic and also italic", paragraph.text)
    assertEquals(
      listOf(
        InlineSpan(InlineStyle.Italic, 0, 6),
        InlineSpan(InlineStyle.Italic, 11, 22),
      ),
      paragraph.spans,
    )
  }

  @Test
  fun `records a code span for backtick-wrapped text`() {
    val article = ArticleParser.parse("run `bloggo sync` to publish")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("run bloggo sync to publish", paragraph.text)
    assertEquals(listOf(InlineSpan(InlineStyle.Code, 4, 15)), paragraph.spans)
  }

  @Test
  fun `does not read a double-asterisk bold run as two italic runs`() {
    val article = ArticleParser.parse("**bold** text")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("bold text", paragraph.text)
    assertEquals(listOf(InlineSpan(InlineStyle.Bold, 0, 4)), paragraph.spans)
  }

  @Test
  fun `spans and links can appear in the same block without their ranges colliding`() {
    val article = ArticleParser.parse("**bold** and a [link](https://example.com)")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("bold and a link", paragraph.text)
    assertEquals(listOf(InlineSpan(InlineStyle.Bold, 0, 4)), paragraph.spans)
    assertEquals(1, paragraph.links.size)
    val link = paragraph.links[0]
    assertEquals("link", link.text)
    assertEquals("https://example.com", link.url)
    assertEquals("link", paragraph.text.substring(link.start, link.end))
  }

  @Test
  fun `preserves link text and url for a markdown link`() {
    val article = ArticleParser.parse("Read the [docs](https://example.com/docs) for more.")
    val paragraph = article.blocks[0] as ArticleBlock.Paragraph
    assertEquals("Read the docs for more.", paragraph.text)
    assertEquals(1, paragraph.links.size)
    val link = paragraph.links[0]
    assertEquals("docs", link.text)
    assertEquals("https://example.com/docs", link.url)
    assertEquals("docs", paragraph.text.substring(link.start, link.end))
  }

  @Test
  fun `counts words excluding frontmatter and markdown punctuation`() {
    val article = ArticleParser.parse(
      """
      ---
      title: ignored words here
      ---

      One two three.
      """.trimIndent()
    )
    assertEquals(3, article.wordCount)
  }

  @Test
  fun `malformed frontmatter without a closing fence does not crash`() {
    val article = ArticleParser.parse("---\ntitle: no closing fence\n\nBody.")
    assertEquals("no closing fence", article.frontmatter["title"])
  }

  @Test
  fun `renders a standalone markdown image as a figure`() {
    val article = ArticleParser.parse("![](/images/foo.png)\nCaption paragraph.")
    val figure = article.blocks[0] as ArticleBlock.Figure
    assertEquals("/images/foo.png", figure.src)
    assertEquals("", figure.caption)
    assertEquals(ArticleBlock.Paragraph("Caption paragraph."), article.blocks[1])
  }

  @Test
  fun `renders a hugo video shortcode as a block`() {
    val article = ArticleParser.parse(
      """{{< video src="/videos/demo.webm" >}}"""
    )
    val video = article.blocks[0] as ArticleBlock.Video
    assertEquals("/videos/demo.webm", video.src)
  }

  @Test
  fun `a self-closing video shortcode does not swallow the rest of the article`() {
    // Regression test: the video shortcode has no closing tag. Before it was
    // recognized as self-closing, the parser treated it like an unmatched
    // `callout` open and discarded every block after it.
    val article = ArticleParser.parse(
      """
      ## Before

      {{< video src="/videos/demo.webm" >}}

      ## After
      """.trimIndent()
    )
    assertEquals(ArticleBlock.Heading(2, "Before"), article.blocks[0])
    assertEquals(ArticleBlock.Video("/videos/demo.webm"), article.blocks[1])
    assertEquals(ArticleBlock.Heading(2, "After"), article.blocks[2])
  }

  @Test
  fun `an unrecognized unclosed shortcode flushes its content instead of vanishing`() {
    val article = ArticleParser.parse(
      "{{< unknown-thing >}}\nTrailing text with no closing tag."
    )
    assertEquals(1, article.blocks.size)
    val callout = article.blocks[0] as ArticleBlock.Callout
    assertEquals("Trailing text with no closing tag.", callout.text)
  }

  @Test
  fun `a fenced code block is captured verbatim as its own block`() {
    val article = ArticleParser.parse(
      """
      ## Before

      ```kotlin
      fun greet(name: String) {
          println("Hello, ${'$'}name!")
      }
      ```

      ## After
      """.trimIndent()
    )
    assertEquals(ArticleBlock.Heading(2, "Before"), article.blocks[0])
    val code = article.blocks[1] as ArticleBlock.CodeBlock
    assertEquals("kotlin", code.language)
    assertEquals(
      "fun greet(name: String) {\n    println(\"Hello, \$name!\")\n}",
      code.code,
    )
    assertEquals(ArticleBlock.Heading(2, "After"), article.blocks[2])
  }

  @Test
  fun `a fenced code block is not run through inline formatting`() {
    // Asterisks and inline-code backticks inside a fence are literal source,
    // not markdown emphasis — a run of `**` in a diff or a shell flag should
    // survive untouched.
    val article = ArticleParser.parse(
      """
      ```
      echo **not bold**
      ```
      """.trimIndent()
    )
    val code = article.blocks[0] as ArticleBlock.CodeBlock
    assertEquals("echo **not bold**", code.code)
  }
}
