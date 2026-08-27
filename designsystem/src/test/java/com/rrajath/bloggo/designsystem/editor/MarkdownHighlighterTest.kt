package com.rrajath.bloggo.designsystem.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHighlighterTest {

  private fun spans(source: String) = MarkdownHighlighter.spans(source)

  private fun textOf(source: String, span: MarkdownSpan) = source.substring(span.start, span.end)

  private fun roled(source: String, role: MarkdownRole) =
    spans(source).filter { it.role == role }.map { textOf(source, it) }

  // ---- the invariant the whole design rests on ----

  @Test
  fun `every span stays inside the source`() {
    val source = SAMPLE
    spans(source).forEach {
      assertTrue("span ${it.start}..${it.end} escapes ${source.length}", it.end <= source.length)
      assertTrue("negative start", it.start >= 0)
    }
  }

  @Test
  fun `highlighting never changes the text length`() {
    // Identity offset mapping is only sound because nothing is inserted or
    // removed. This is the test that protects that promise.
    listOf(SAMPLE, "", "\n\n\n", "plain", "**", "`", "[](", "*a*").forEach { source ->
      val styled = spans(source)
      styled.forEach { assertTrue(it.end <= source.length) }
    }
  }

  // ---- frontmatter ----

  @Test
  fun `frontmatter is only frontmatter at the top of the file`() {
    val source = "---\ntitle: Hello\n---\n\nBody\n"
    assertEquals(listOf("title"), roled(source, MarkdownRole.FrontmatterKey))
    assertEquals(listOf("title: Hello"), roled(source, MarkdownRole.Frontmatter))
  }

  @Test
  fun `a rule in the body is not treated as frontmatter`() {
    val source = "Some text\n\n---\n\ntitle: not a key\n"
    assertTrue(roled(source, MarkdownRole.Frontmatter).isEmpty())
    assertTrue(roled(source, MarkdownRole.FrontmatterKey).isEmpty())
  }

  @Test
  fun `unterminated frontmatter keeps styling rather than swallowing the document`() {
    val source = "---\ntitle: Hello\ndate: 2026-08-17\n"
    assertEquals(listOf("title", "date"), roled(source, MarkdownRole.FrontmatterKey))
  }

  // ---- TOML frontmatter ----

  @Test
  fun `TOML frontmatter is only frontmatter at the top of the file`() {
    val source = "+++\ntitle = \"Hello\"\n+++\n\nBody\n"
    assertEquals(listOf("title"), roled(source, MarkdownRole.FrontmatterKey))
    assertEquals(listOf("title = \"Hello\""), roled(source, MarkdownRole.Frontmatter))
  }

  @Test
  fun `a plus rule in the body is not treated as TOML frontmatter`() {
    val source = "Some text\n\n+++\n\ntitle = \"not a key\"\n"
    assertTrue(roled(source, MarkdownRole.Frontmatter).isEmpty())
    assertTrue(roled(source, MarkdownRole.FrontmatterKey).isEmpty())
  }

  @Test
  fun `unterminated TOML frontmatter keeps styling rather than swallowing the document`() {
    val source = "+++\ntitle = \"Hello\"\ndate = 2026-08-17\n"
    assertEquals(listOf("title", "date"), roled(source, MarkdownRole.FrontmatterKey))
  }

  @Test
  fun `a YAML fence never closes a TOML block or vice versa`() {
    // A --- inside a +++ block (or +++ inside a --- block) is body content of
    // the frontmatter, not a close: only the fence that opened it can close it.
    val toml = "+++\ntitle = \"Hello\"\n---\ndate = 2026-08-17\n+++\n\nBody\n"
    assertEquals(listOf("title", "date"), roled(toml, MarkdownRole.FrontmatterKey))

    val yaml = "---\ntitle: Hello\n+++\ndate: 2026-08-17\n---\n\nBody\n"
    assertEquals(listOf("title", "date"), roled(yaml, MarkdownRole.FrontmatterKey))
  }

  @Test
  fun `TOML array and table lines are still styled as frontmatter without a false key match`() {
    val source = "+++\ntitle = \"Hello\"\n  \"nested\",\n[extra]\n+++\n"
    // The whole block still reads as frontmatter, same as an unmatched YAML line.
    assertEquals(3, roled(source, MarkdownRole.Frontmatter).size)
    assertEquals(listOf("title"), roled(source, MarkdownRole.FrontmatterKey))
  }

  // ---- block level ----

  @Test
  fun `heading marker is dimmed and the text is styled by level`() {
    val h1 = "# Big"
    assertEquals(listOf("# "), roled(h1, MarkdownRole.Marker))
    assertEquals(listOf("Big"), roled(h1, MarkdownRole.Heading1))

    val h2 = "## The gap nobody talks about"
    assertEquals(listOf("## "), roled(h2, MarkdownRole.Marker))
    assertEquals(listOf("The gap nobody talks about"), roled(h2, MarkdownRole.Heading2))
  }

  @Test
  fun `a hash without a space is not a heading`() {
    assertTrue(roled("#nothashtag", MarkdownRole.Heading1).isEmpty())
  }

  @Test
  fun `blockquote marker and body`() {
    val source = "> A generated diff is a hypothesis."
    assertEquals(listOf("> "), roled(source, MarkdownRole.Marker))
    assertEquals(listOf("A generated diff is a hypothesis."), roled(source, MarkdownRole.Quote))
  }

  @Test
  fun `ordered and unordered bullets`() {
    assertEquals(listOf("1. "), roled("1. They write to files I already own", MarkdownRole.Bullet))
    assertEquals(listOf("- "), roled("- a point", MarkdownRole.Bullet))
    assertEquals(listOf("12. "), roled("12. twelfth", MarkdownRole.Bullet))
  }

  @Test
  fun `fenced code toggles and its content is styled apart`() {
    val source = "```ts\nconst x = 1\n```\nafter\n"
    assertEquals(listOf("```ts", "```"), roled(source, MarkdownRole.Fence))
    assertEquals(listOf("const x = 1"), roled(source, MarkdownRole.FenceContent))
  }

  @Test
  fun `markdown inside a fence is left alone`() {
    val source = "```\n**not bold** and *not italic*\n```\n"
    assertTrue(roled(source, MarkdownRole.Bold).isEmpty())
    assertTrue(roled(source, MarkdownRole.Italic).isEmpty())
  }

  @Test
  fun `hugo shortcodes get their own role`() {
    assertEquals(
      listOf("{{< callout type=\"note\" >}}"),
      roled("{{< callout type=\"note\" >}}", MarkdownRole.Shortcode),
    )
    assertEquals(listOf("{{% note %}}"), roled("{{% note %}}", MarkdownRole.Shortcode))
  }

  // ---- inline ----

  @Test
  fun `bold and italic`() {
    assertEquals(listOf("decides whether"), roled("it **decides whether** it matters", MarkdownRole.Bold))
    assertEquals(listOf("generation"), roled("the moment of *generation*", MarkdownRole.Italic))
  }

  @Test
  fun `bold is not mistaken for two italics`() {
    val source = "**bold**"
    assertEquals(listOf("bold"), roled(source, MarkdownRole.Bold))
    assertTrue(roled(source, MarkdownRole.Italic).isEmpty())
  }

  @Test
  fun `inline code beats emphasis inside it`() {
    val source = "use `a * b * c` carefully"
    assertEquals(listOf("a * b * c"), roled(source, MarkdownRole.Code))
    assertTrue(roled(source, MarkdownRole.Italic).isEmpty())
  }

  @Test
  fun `links split into text and url`() {
    val source = "see [the tooling notes](/notes/tooling) for more"
    assertEquals(listOf("the tooling notes"), roled(source, MarkdownRole.LinkText))
    assertEquals(listOf("/notes/tooling"), roled(source, MarkdownRole.LinkUrl))
    assertEquals(listOf("[", "](", ")"), roled(source, MarkdownRole.Marker))
  }

  @Test
  fun `emphasis inside a link label is not double claimed`() {
    val source = "[*emph*](/x)"
    assertEquals(listOf("*emph*"), roled(source, MarkdownRole.LinkText))
    assertTrue(roled(source, MarkdownRole.Italic).isEmpty())
  }

  @Test
  fun `several inline runs on one line`() {
    val source = "**a** and *b* and `c`"
    assertEquals(listOf("a"), roled(source, MarkdownRole.Bold))
    assertEquals(listOf("b"), roled(source, MarkdownRole.Italic))
    assertEquals(listOf("c"), roled(source, MarkdownRole.Code))
  }

  @Test
  fun `inline styling works inside headings, quotes and bullets`() {
    assertEquals(listOf("ship"), roled("## On agents that **ship**", MarkdownRole.Bold))
    assertEquals(listOf("hypothesis"), roled("> a *hypothesis*", MarkdownRole.Italic))
    assertEquals(listOf("own"), roled("1. files I already `own`", MarkdownRole.Code))
  }

  // ---- things that must not blow up ----

  @Test
  fun `unclosed markers are left as plain text`() {
    listOf("**unclosed", "*unclosed", "`unclosed", "[text](unclosed", "[text]").forEach { source ->
      val result = spans(source)
      assertTrue("crashed or styled '$source'", result.none { it.role == MarkdownRole.Bold })
    }
  }

  @Test
  fun `empty and whitespace input`() {
    assertTrue(spans("").isEmpty() || spans("").all { it.end <= 0 })
    spans("\n\n   \n")
    spans("   ")
  }

  @Test
  fun `emphasis markers with nothing between them are ignored`() {
    assertTrue(roled("****", MarkdownRole.Bold).isEmpty())
    assertTrue(roled("**", MarkdownRole.Italic).isEmpty())
  }

  @Test
  fun `offsets are correct on later lines`() {
    val source = "line one\nline two **bold**"
    val bold = spans(source).single { it.role == MarkdownRole.Bold }
    assertEquals("bold", textOf(source, bold))
  }

  @Test
  fun `handles the prototype document end to end`() {
    val spans = spans(SAMPLE)
    assertTrue(spans.isNotEmpty())
    assertEquals(listOf("title", "date", "tags", "draft"), roled(SAMPLE, MarkdownRole.FrontmatterKey))
    assertEquals(listOf("generation"), roled(SAMPLE, MarkdownRole.Italic))
    assertEquals(listOf("decides whether any of it matters"), roled(SAMPLE, MarkdownRole.Bold))
    assertEquals(listOf("the tooling notes"), roled(SAMPLE, MarkdownRole.LinkText))
    assertEquals(3, roled(SAMPLE, MarkdownRole.Bullet).size)
  }

  @Test
  fun `handles a TOML-fenced version of the same document end to end`() {
    val spans = spans(TOML_SAMPLE)
    assertTrue(spans.isNotEmpty())
    assertEquals(listOf("title", "date", "tags", "draft"), roled(TOML_SAMPLE, MarkdownRole.FrontmatterKey))
    assertEquals(listOf("generation"), roled(TOML_SAMPLE, MarkdownRole.Italic))
    assertEquals(listOf("decides whether any of it matters"), roled(TOML_SAMPLE, MarkdownRole.Bold))
    assertEquals(listOf("the tooling notes"), roled(TOML_SAMPLE, MarkdownRole.LinkText))
    assertEquals(3, roled(TOML_SAMPLE, MarkdownRole.Bullet).size)
    assertEquals(listOf("+++", "+++"), roled(TOML_SAMPLE, MarkdownRole.Fence))
  }

  private companion object {
    val SAMPLE = """
      ---
      title: On agents that actually ship
      date: 2026-08-17
      tags: [ai, tooling, craft]
      draft: true
      ---

      ## The gap nobody talks about

      Every agent demo ends at the moment of *generation*. Then comes the part that
      **decides whether any of it matters**.

      > A generated diff is a hypothesis. Shipping is the experiment.

      1. They write to files I already own
      2. They show me the diff before it lands
      3. They fail loudly, in one place

      See [the tooling notes](/notes/tooling) for the longer version.
    """.trimIndent()

    val TOML_SAMPLE = """
      +++
      title = "On agents that actually ship"
      date = 2026-08-17
      tags = ["ai", "tooling", "craft"]
      draft = true
      +++

      ## The gap nobody talks about

      Every agent demo ends at the moment of *generation*. Then comes the part that
      **decides whether any of it matters**.

      > A generated diff is a hypothesis. Shipping is the experiment.

      1. They write to files I already own
      2. They show me the diff before it lands
      3. They fail loudly, in one place

      See [the tooling notes](/notes/tooling) for the longer version.
    """.trimIndent()
  }
}
