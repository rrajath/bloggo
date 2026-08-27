package com.rrajath.bloggo.ui.inbox

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureListContinuationTest {

  @Test
  fun `Enter on a non-empty bullet line continues the list with the same marker`() {
    val old = TextFieldValue("- item", TextRange(6))
    val new = TextFieldValue("- item\n", TextRange(7))

    val result = continueListOnEnter(old, new)

    assertEquals("- item\n- ", result.text)
    assertEquals(TextRange(9), result.selection)
  }

  @Test
  fun `Enter on an empty bullet line removes the marker instead of continuing`() {
    val old = TextFieldValue("Some text\n- ", TextRange(12))
    val new = TextFieldValue("Some text\n- \n", TextRange(13))

    val result = continueListOnEnter(old, new)

    assertEquals("Some text\n", result.text)
    assertEquals(TextRange(10), result.selection)
  }

  @Test
  fun `Enter on a non-empty numbered line increments the number`() {
    val old = TextFieldValue("1. first", TextRange(8))
    val new = TextFieldValue("1. first\n", TextRange(9))

    val result = continueListOnEnter(old, new)

    assertEquals("1. first\n2. ", result.text)
    assertEquals(TextRange(12), result.selection)
  }

  @Test
  fun `Enter continues a numbered list from whatever number the line actually has`() {
    val old = TextFieldValue("1. first\n2. second", TextRange(19))
    val new = TextFieldValue("1. first\n2. second\n", TextRange(20))

    val result = continueListOnEnter(old, new)

    assertEquals("1. first\n2. second\n3. ", result.text)
  }

  @Test
  fun `Enter on an empty numbered line removes the marker instead of continuing`() {
    val old = TextFieldValue("1. ", TextRange(3))
    val new = TextFieldValue("1. \n", TextRange(4))

    val result = continueListOnEnter(old, new)

    assertEquals("", result.text)
    assertEquals(TextRange(0), result.selection)
  }

  @Test
  fun `Enter on a plain prose line is left completely untouched`() {
    val old = TextFieldValue("Just a thought", TextRange(14))
    val new = TextFieldValue("Just a thought\n", TextRange(15))

    val result = continueListOnEnter(old, new)

    assertEquals(new.text, result.text)
    assertEquals(new.selection, result.selection)
  }

  @Test
  fun `indented bullets keep their indent on continuation`() {
    val old = TextFieldValue("  * nested", TextRange(10))
    val new = TextFieldValue("  * nested\n", TextRange(11))

    val result = continueListOnEnter(old, new)

    assertEquals("  * nested\n  * ", result.text)
  }

  @Test
  fun `a multi-character insertion, such as a paste, is never treated as a single Enter`() {
    val old = TextFieldValue("- item", TextRange(6))
    val new = TextFieldValue("- item\nmore\n", TextRange(12))

    val result = continueListOnEnter(old, new)

    assertEquals(new.text, result.text)
    assertEquals(new.selection, result.selection)
  }

  @Test
  fun `a deletion is never treated as an Enter insertion`() {
    val old = TextFieldValue("- item", TextRange(6))
    val new = TextFieldValue("- ite", TextRange(5))

    val result = continueListOnEnter(old, new)

    assertEquals(new.text, result.text)
    assertEquals(new.selection, result.selection)
  }
}
