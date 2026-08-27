package com.rrajath.bloggo.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadabilityAnalyzerTest {

  private fun analyze(markdown: String, enabled: Set<ReadabilityCheck> = ReadabilityCheck.All) =
    ReadabilityAnalyzer.analyze(markdown, enabled)

  private fun ReadabilityReport.wordFlags(): List<WordFlag> =
    blocks.filterIsInstance<RenderBlock.Prose>().flatMap { it.wordFlags }

  @Test
  fun `an empty draft scores nothing`() {
    val report = analyze("")
    assertEquals(0, report.grade)
    assertEquals(0, report.wordCount)
    assertEquals(0, report.sentenceCount)
    assertTrue(report.blocks.isEmpty())
    assertTrue(report.notes.isEmpty())
  }

  @Test
  fun `frontmatter and fenced code are not analysed`() {
    val markdown = """
      ---
      title: ignored words that should not count at all
      ---

      One two three.

      ```
      the config was generated automatically
      ```
    """.trimIndent()

    val report = analyze(markdown)

    assertEquals(3, report.wordCount)
    assertEquals(1, report.sentenceCount)
    assertEquals(0, report.counts.getValue(FlagCategory.Passive))
  }

  @Test
  fun `BreakIterator does not split on an abbreviation`() {
    val report = analyze("Dr. Smith went home. He left early.")
    assertEquals(2, report.sentenceCount)
  }

  @Test
  fun `passive voice is flagged, an active be-verb is not`() {
    val flagged = analyze("The report was written overnight.").wordFlags()
    assertEquals(1, flagged.count { it.category == FlagCategory.Passive })
    assertEquals("Passive voice. Name who does it.", flagged.first { it.category == FlagCategory.Passive }.reason)

    val clean = analyze("It was helpful and clear.").wordFlags()
    assertTrue(clean.none { it.category == FlagCategory.Passive })
  }

  @Test
  fun `an irregular participle still reads as passive`() {
    val flagged = analyze("The anthem was sung before the match.").wordFlags()
    assertTrue(flagged.any { it.category == FlagCategory.Passive })
  }

  @Test
  fun `adverbs are flagged but the -ly stop list is not`() {
    val flagged = analyze("She quickly closed the early reply.").wordFlags()
    assertTrue(flagged.any { it.category == FlagCategory.Adverb })
    // "early" and "reply" are on the stop list, "the" is too short.
    assertEquals(1, flagged.count { it.category == FlagCategory.Adverb })
  }

  @Test
  fun `weak qualifiers and complex words carry a suggestion`() {
    val weak = analyze("This is very important.").wordFlags()
    assertEquals("Weak qualifier. Cut it or commit.", weak.first { it.category == FlagCategory.Adverb }.reason)

    val complex = analyze("We should utilize the platform.").wordFlags().first { it.category == FlagCategory.Complex }
    assertEquals("Complex word. Try “use”.", complex.reason)
  }

  @Test
  fun `a wordy phrase is flagged as one range`() {
    val flagged = analyze("He trained hard in order to qualify.").wordFlags()
    val phrase = flagged.first { it.category == FlagCategory.Complex }
    assertEquals("Wordy. Try “to”.", phrase.reason)
  }

  @Test
  fun `a long dense sentence is a hard sentence`() {
    val markdown =
      "The comprehensive documentation describes numerous configuration parameters " +
        "that fundamentally determine how the distributed system coordinates replication " +
        "across geographically separated availability zones."
    val report = analyze(markdown)
    assertTrue(
      "expected a hard or very-hard sentence",
      report.counts.getValue(FlagCategory.Hard) + report.counts.getValue(FlagCategory.VeryHard) >= 1,
    )
  }

  @Test
  fun `disabling a check removes its flags and its count`() {
    val markdown = "The report was written overnight by the team."
    val without = analyze(markdown, ReadabilityCheck.All - ReadabilityCheck.PassiveVoice)

    assertEquals(0, without.counts.getValue(FlagCategory.Passive))
    assertTrue(without.wordFlags().none { it.category == FlagCategory.Passive })
  }

  @Test
  fun `headings are kept in the block list but never analysed`() {
    val report = analyze("# A Heading That Is Never Scored\n\nA short sentence.")
    assertTrue(report.blocks.first() is RenderBlock.Heading)
    assertEquals(1, report.sentenceCount)
  }

  @Test
  fun `a repeated uncommon word is noted once`() {
    val report = analyze("The architecture is clean. The architecture also scales well.")
    val notes = report.notes.filter { it.kind == NoteKind.RepeatedWord }
    assertEquals(1, notes.size)
    assertTrue(notes.first().message.contains("architecture"))
  }

  @Test
  fun `three sentences with the same opener are noted`() {
    val report = analyze("The cat slept. The dog barked. The bird sang.")
    assertTrue(report.notes.any { it.kind == NoteKind.SameOpener })
  }

  @Test
  fun `an overlong paragraph is noted`() {
    val report = analyze(("filler ".repeat(170)).trim() + ".")
    assertTrue(report.notes.any { it.kind == NoteKind.LongParagraph })
  }

  @Test
  fun `leftover draft markers are noted`() {
    val report = analyze("This section still needs a TODO and a [citation needed] before it ships.")
    val markers = report.notes.filter { it.kind == NoteKind.DraftMarker }
    assertTrue(markers.any { it.message.contains("TODO") })
    assertTrue(markers.any { it.message.contains("citation needed") })
  }

  @Test
  fun `disabling the extra checks silences their notes`() {
    val markdown = "The plan is solid. The plan works. The plan ships. TODO fix later."
    val report = analyze(markdown, emptySet())
    assertTrue(report.notes.isEmpty())
  }

  @Test
  fun `word and sentence flag offsets stay inside the block text`() {
    val markdown =
      "The comprehensive documentation describes numerous configuration parameters " +
        "that were written to determine replication behaviour across zones."
    val prose = analyze(markdown).blocks.filterIsInstance<RenderBlock.Prose>().first()
    prose.wordFlags.forEach {
      assertTrue(it.start in 0..prose.text.length)
      assertTrue(it.end in it.start..prose.text.length)
    }
    prose.sentences.forEach {
      assertTrue(it.start in 0..prose.text.length)
      assertTrue(it.end in it.start..prose.text.length)
    }
  }

  @Test
  fun `fromStored round-trips and defaults to every check`() {
    assertEquals(ReadabilityCheck.All, ReadabilityCheck.fromStored(null))
    assertEquals(emptySet<ReadabilityCheck>(), ReadabilityCheck.fromStored(""))
    assertEquals(
      setOf(ReadabilityCheck.PassiveVoice, ReadabilityCheck.Adverbs),
      ReadabilityCheck.fromStored("PassiveVoice,Adverbs"),
    )
    assertFalse(ReadabilityCheck.HardSentences in ReadabilityCheck.fromStored("PassiveVoice"))
  }
}
