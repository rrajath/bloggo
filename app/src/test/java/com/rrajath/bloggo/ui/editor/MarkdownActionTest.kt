package com.rrajath.bloggo.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownActionTest {

  @Test
  fun `Heading inserts a level-one marker on a line with no heading`() {
    val value = TextFieldValue("Some text", TextRange(4))

    val result = MarkdownAction.Heading.applyTo(value)

    assertEquals("# Some text", result.text)
  }

  @Test
  fun `Heading cycles from level one to level two on repeated taps`() {
    val value = TextFieldValue("# Some text", TextRange(4))

    val result = MarkdownAction.Heading.applyTo(value)

    assertEquals("## Some text", result.text)
  }

  @Test
  fun `Heading cycles through every level and wraps back to level one`() {
    var value = TextFieldValue("Some text", TextRange(0))
    val expectedPrefixes = listOf("#", "##", "###", "####", "#####", "######", "#")

    for (expected in expectedPrefixes) {
      value = MarkdownAction.Heading.applyTo(value)
      assertEquals("$expected Some text", value.text)
    }
  }

  @Test
  fun `Heading only affects the line the caret is on`() {
    val value = TextFieldValue("First line\nSecond line", TextRange(15))

    val result = MarkdownAction.Heading.applyTo(value)

    assertEquals("First line\n# Second line", result.text)
  }

  @Test
  fun `Heading keeps the caret positioned relative to the line after the prefix changes`() {
    val value = TextFieldValue("## Some text", TextRange(6))

    val result = MarkdownAction.Heading.applyTo(value)

    assertEquals("### Some text", result.text)
    assertEquals(TextRange(7), result.selection)
  }

  @Test
  fun `Link wraps the selection and leaves the caret right after https for typing the url`() {
    val value = TextFieldValue("Read the docs", TextRange(9, 13))

    val result = MarkdownAction.Link.applyTo(value)

    assertEquals("Read the [docs](https://)", result.text)
    val caret = result.selection.start
    assertEquals(caret, result.selection.end)
    assertEquals("[docs](https://", result.text.substring(9, caret))
  }

  @Test
  fun `Link with no selection selects the placeholder link text`() {
    val value = TextFieldValue("", TextRange(0))

    val result = MarkdownAction.Link.applyTo(value)

    assertEquals("[link text](https://)", result.text)
    assertEquals("link text", result.text.substring(result.selection.min, result.selection.max))
  }
}
