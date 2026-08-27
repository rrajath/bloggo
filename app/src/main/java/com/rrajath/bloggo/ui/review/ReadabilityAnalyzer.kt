package com.rrajath.bloggo.ui.review

import com.rrajath.bloggo.ui.preview.ArticleBlock
import com.rrajath.bloggo.ui.preview.ArticleParser
import kotlin.math.roundToInt

/**
 * The prototype's five highlight categories plus [Note]. Sentence-level ([Hard],
 * [VeryHard]) and word-level ([Complex], [Adverb], [Passive]) washes stack over
 * the same text, the way Hemingway's editor does. [Note] is the neutral slate
 * wash the screen paints on the spans that triggered a block-level check (see
 * [ReadabilityNote]); it also stacks. Kept Compose-free — the screen maps each
 * to a `--an-*` colour.
 */
enum class FlagCategory { Hard, VeryHard, Complex, Adverb, Passive, Note }

/** A flagged word or phrase, as a character range into a block's plain text. */
data class WordFlag(val start: Int, val end: Int, val category: FlagCategory, val reason: String)

/** One sentence's span in a block's text, with its hardness wash (or none). */
data class SentenceRange(val start: Int, val end: Int, val flag: FlagCategory?)

/** How a prose block is laid out in the review. */
enum class ProseKind { Paragraph, Quote, ListItem }

/** A block ready to render. Headings are kept but never analysed, matching the
 * prototype (`reviewReport` runs only paragraphs, quotes and list items through
 * the scorer). */
sealed interface RenderBlock {
  data class Heading(val level: Int, val text: String) : RenderBlock
  data class Prose(
    val kind: ProseKind,
    val marker: String?,
    val text: String,
    val sentences: List<SentenceRange>,
    val wordFlags: List<WordFlag>,
    /** Spans that a block-level check flagged, painted with the [FlagCategory.Note]
     * wash and carrying the note's message as their tap reason. */
    val noteFlags: List<WordFlag> = emptyList(),
  ) : RenderBlock
}

/** The four extra checks. Surfaced both as an advisory list and, since each
 * [ReadabilityNote] now carries the [NoteSpan]s that triggered it, as a neutral
 * slate wash on those spans in the prose. */
enum class NoteKind { RepeatedWord, SameOpener, LongParagraph, DraftMarker }

/** Where in the rendered prose a note's finding sits. [blockIndex] indexes into
 * [ReadabilityReport.blocks]; [start]/[end] are character offsets into that
 * block's `text`. */
data class NoteSpan(val blockIndex: Int, val start: Int, val end: Int)

data class ReadabilityNote(
  val kind: NoteKind,
  val message: String,
  val spans: List<NoteSpan> = emptyList(),
)

/** Everything the review screen needs for one draft. */
data class ReadabilityReport(
  val grade: Int,
  val wordCount: Int,
  val sentenceCount: Int,
  val adverbCount: Int,
  val counts: Map<FlagCategory, Int>,
  val blocks: List<RenderBlock>,
  val notes: List<ReadabilityNote>,
) {
  companion object {
    val Empty = ReadabilityReport(
      grade = 0,
      wordCount = 0,
      sentenceCount = 0,
      adverbCount = 0,
      counts = FlagCategory.entries.associateWith { 0 },
      blocks = emptyList(),
      notes = emptyList(),
    )
  }
}

/** Which checks are on. Persisted by `SettingsRepository` as a comma-joined list
 * of names; an unset value means every check is on. */
enum class ReadabilityCheck {
  HardSentences,
  PassiveVoice,
  Adverbs,
  WeakQualifiers,
  ComplexWords,
  RepeatedWords,
  SameOpenerSentences,
  LongParagraphs,
  DraftMarkers;

  companion object {
    val All: Set<ReadabilityCheck> = entries.toSet()

    fun fromStored(raw: String?): Set<ReadabilityCheck> {
      if (raw == null) return All
      return raw.split(',')
        .mapNotNullTo(linkedSetOf()) { token -> entries.firstOrNull { it.name == token.trim() } }
    }
  }
}

/**
 * A Hemingway-style pass over the current draft. Ported from the prototype
 * script (`internal/design/bloggo-prototype.html`): sentence splitting, a
 * vowel-group syllable estimate, a Flesch-Kincaid grade per sentence and for the
 * document, and word/phrase flags against [ReadabilityLexicon]. Pure and
 * Compose-free, the same design rule as `ArticleParser`.
 *
 * Differs from the prototype in one place: [splitSentences] guards against
 * common abbreviations and initials ("Dr. Smith", "U.S.") rather than breaking
 * on every period, so they do not inflate the sentence count (and therefore the
 * grade). It is deliberately a hand-rolled walk rather than `BreakIterator` so
 * that JVM unit tests and the on-device run agree. The word lists and the
 * thresholds below are the parts to review when tuning the feature.
 */
object ReadabilityAnalyzer {

  private const val HARD_WORDS = 11
  private const val HARD_GRADE = 9.0
  private const val VERY_HARD_WORDS = 22
  private const val VERY_HARD_GRADE = 13.0
  private const val LONG_PARAGRAPH_WORDS = 150
  private const val REPEAT_WINDOW = 40
  private const val MIN_REPEAT_LENGTH = 5
  private const val OPENER_RUN = 3
  private const val THERE_OPENER_LIMIT = 3

  private val wordRegex = Regex("[A-Za-z0-9'’]+")
  private val flagWordRegex = Regex("[A-Za-z’']+")
  private val vowelGroupRegex = Regex("[aeiouy]{1,2}")
  private val syllableTrimRegex = Regex("(?:[^laeiouy]es|ed|[^laeiouy]e)$")
  private val leadingYRegex = Regex("^y")
  private val nonAlphaRegex = Regex("[^a-z]")
  private val passiveRegex = Regex(
    "\\b(?:am|is|are|was|were|be|been|being|get|got|gets)\\b(?:\\s+\\w+ly)?\\s+([A-Za-z]+)",
    RegexOption.IGNORE_CASE,
  )
  private val trailingEdRegex = Regex("[a-z]{2}ed$")
  private val draftMarkerRegex = Regex("\\b(TODO|FIXME|TK|XXX)\\b")
  private val bracketPlaceholderRegex = Regex("\\[[^\\]\\n]{1,60}]")

  private val repeatStopWords = setOf(
    "about", "above", "after", "again", "against", "along", "already", "although", "always",
    "another", "around", "because", "been", "before", "being", "below", "between", "beyond",
    "could", "different", "doing", "during", "either", "enough", "every", "everything", "first",
    "further", "have", "having", "here", "howdy", "into", "itself", "makes", "might", "more",
    "most", "much", "never", "often", "other", "over", "really", "same", "should", "since",
    "some", "something", "still", "such", "than", "that", "their", "them", "then", "there",
    "these", "they", "thing", "things", "this", "those", "through", "under", "until", "using",
    "very", "were", "what", "when", "where", "which", "while", "with", "within", "without",
    "would", "your",
  )

  fun analyze(markdown: String, enabled: Set<ReadabilityCheck> = ReadabilityCheck.All): ReadabilityReport {
    val article = ArticleParser.parse(markdown)

    var totalWords = 0
    var totalSyllables = 0
    var totalSentences = 0
    val counts = FlagCategory.entries.associateWithTo(mutableMapOf()) { 0 }

    val renderBlocks = mutableListOf<RenderBlock>()
    val documentWords = mutableListOf<WordOccurrence>()
    val sentenceOpeners = mutableListOf<OpenerOccurrence>()
    val notes = mutableListOf<ReadabilityNote>()

    for (block in article.blocks) {
      when (block) {
        is ArticleBlock.Heading -> renderBlocks += RenderBlock.Heading(block.level, block.text)

        is ArticleBlock.Paragraph, is ArticleBlock.Callout,
        is ArticleBlock.Quote, is ArticleBlock.ListItem -> {
          val (text, kind, marker) = when (block) {
            is ArticleBlock.Quote -> Triple(block.text, ProseKind.Quote, null)
            is ArticleBlock.ListItem -> Triple(block.text, ProseKind.ListItem, block.marker)
            is ArticleBlock.Callout -> Triple(block.text, ProseKind.Paragraph, null)
            else -> Triple((block as ArticleBlock.Paragraph).text, ProseKind.Paragraph, null)
          }

          val blockIndex = renderBlocks.size
          val prose = analyzeProse(text, enabled, counts, blockIndex)
          totalWords += prose.words
          totalSyllables += prose.syllables
          totalSentences += prose.sentences.size
          documentWords += prose.occurrences
          // List items naturally start alike ("They ship...", "They fail...");
          // opener monotony is only interesting in running prose.
          if (kind != ProseKind.ListItem) sentenceOpeners += prose.openers

          if (kind == ProseKind.Paragraph &&
            ReadabilityCheck.LongParagraphs in enabled &&
            prose.words > LONG_PARAGRAPH_WORDS
          ) {
            // Wash the first sentence as the anchor; a wash over 150+ words
            // would be a wall of colour.
            val anchor = prose.sentences.firstOrNull()
              ?.let { listOf(NoteSpan(blockIndex, it.start, it.end)) }
              .orEmpty()
            notes += ReadabilityNote(
              NoteKind.LongParagraph,
              "One paragraph runs to ${prose.words} words. A break would help the reader.",
              anchor,
            )
          }
          if (ReadabilityCheck.DraftMarkers in enabled) notes += draftMarkerNotes(text, blockIndex)

          renderBlocks += RenderBlock.Prose(kind, marker, text, prose.sentences, prose.wordFlags)
        }

        // Figures, videos, code blocks and rules are dropped, like the prototype.
        else -> Unit
      }
    }

    if (ReadabilityCheck.RepeatedWords in enabled) notes += repeatedWordNotes(documentWords)
    if (ReadabilityCheck.SameOpenerSentences in enabled) notes += openerMonotonyNotes(sentenceOpeners)

    val documentGrade = grade(totalWords, totalSyllables, totalSentences)
    val sortedNotes = notes.sortedBy { it.kind.ordinal }
    attachNoteWashes(sortedNotes, renderBlocks)

    return ReadabilityReport(
      grade = if (totalSentences > 0) maxOf(1, documentGrade.roundToInt()) else 0,
      wordCount = totalWords,
      sentenceCount = totalSentences,
      adverbCount = counts.getValue(FlagCategory.Adverb),
      counts = counts,
      blocks = renderBlocks,
      notes = sortedNotes,
    )
  }

  /** Fold every note's [NoteSpan]s back onto their prose block as
   * [FlagCategory.Note] flags, so the screen can paint and label them. */
  private fun attachNoteWashes(notes: List<ReadabilityNote>, blocks: MutableList<RenderBlock>) {
    val byBlock = HashMap<Int, MutableList<WordFlag>>()
    for (note in notes) {
      for (span in note.spans) {
        byBlock.getOrPut(span.blockIndex) { mutableListOf() } +=
          WordFlag(span.start, span.end, FlagCategory.Note, note.message)
      }
    }
    for ((index, flags) in byBlock) {
      val block = blocks.getOrNull(index)
      if (block is RenderBlock.Prose) {
        blocks[index] = block.copy(noteFlags = flags.sortedBy { it.start })
      }
    }
  }

  /** One word in the document, lowercased, with where it sits in the prose. */
  private data class WordOccurrence(val lower: String, val blockIndex: Int, val start: Int, val end: Int)

  /** The first word of one sentence, lowercased, with where it sits. */
  private data class OpenerOccurrence(val lower: String, val blockIndex: Int, val start: Int, val end: Int)

  private class ProseAnalysis(
    val words: Int,
    val syllables: Int,
    val sentences: List<SentenceRange>,
    val wordFlags: List<WordFlag>,
    val occurrences: List<WordOccurrence>,
    val openers: List<OpenerOccurrence>,
  )

  private fun analyzeProse(
    text: String,
    enabled: Set<ReadabilityCheck>,
    counts: MutableMap<FlagCategory, Int>,
    blockIndex: Int,
  ): ProseAnalysis {
    var words = 0
    var syllables = 0
    val sentenceRanges = mutableListOf<SentenceRange>()
    val wordFlags = mutableListOf<WordFlag>()
    val occurrences = mutableListOf<WordOccurrence>()
    val openers = mutableListOf<OpenerOccurrence>()

    for ((start, end) in splitSentences(text)) {
      val core = text.substring(start, end)
      val coreMatches = wordRegex.findAll(core).toList()
      if (coreMatches.isEmpty()) continue
      val coreWords = coreMatches.map { it.value }

      val wordCount = coreWords.size
      val syllableCount = coreWords.sumOf(::countSyllables)
      words += wordCount
      syllables += syllableCount
      coreMatches.mapTo(occurrences) {
        WordOccurrence(it.value.lowercase(), blockIndex, it.range.first + start, it.range.last + 1 + start)
      }
      coreMatches.first().let {
        openers += OpenerOccurrence(
          it.value.lowercase(), blockIndex, it.range.first + start, it.range.last + 1 + start,
        )
      }

      val sentenceGrade = grade(wordCount, syllableCount, 1)
      var flag: FlagCategory? = null
      if (ReadabilityCheck.HardSentences in enabled) {
        if (wordCount >= VERY_HARD_WORDS && sentenceGrade >= VERY_HARD_GRADE) {
          flag = FlagCategory.VeryHard
          counts.merge(FlagCategory.VeryHard, 1, Int::plus)
        } else if (wordCount >= HARD_WORDS && sentenceGrade >= HARD_GRADE) {
          flag = FlagCategory.Hard
          counts.merge(FlagCategory.Hard, 1, Int::plus)
        }
      }
      sentenceRanges += SentenceRange(start, end, flag)

      for (wordFlag in flagWords(core, enabled, counts)) {
        wordFlags += wordFlag.copy(start = wordFlag.start + start, end = wordFlag.end + start)
      }
    }

    return ProseAnalysis(words, syllables, sentenceRanges, wordFlags, occurrences, openers)
  }

  private val abbreviations = setOf(
    "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc", "fig", "al", "inc", "ltd", "co",
    "dept", "est", "gen", "gov", "hon", "capt", "lt", "sgt", "cf", "approx", "no", "vol", "pp",
    "eg", "ie", "ca", "cca", "ph", "rev", "esq",
  )

  /** A deterministic sentence walk: a sentence ends at `.` / `!` / `?` (plus any
   * run of terminals and a closing quote or bracket) when what follows is
   * whitespace and a plausible new-sentence start. A `.` is not a boundary after
   * a known abbreviation, an initial, or an acronym like "U.S." Returns trimmed
   * (start, end) offsets into [text]. */
  private fun splitSentences(text: String): List<Pair<Int, Int>> {
    if (text.isBlank()) return emptyList()
    val out = mutableListOf<Pair<Int, Int>>()
    var sentenceStart = 0
    var i = 0
    while (i < text.length) {
      val c = text[i]
      if (c != '.' && c != '!' && c != '?') {
        i++
        continue
      }

      var end = i + 1
      while (end < text.length && (text[end] == '.' || text[end] == '!' || text[end] == '?')) end++
      while (end < text.length && text[end] in "\"'”’)]") end++

      val followedByBreak = end >= text.length || text[end].isWhitespace()
      if (!followedByBreak) {
        i++
        continue
      }

      var nextStart = end
      while (nextStart < text.length && text[nextStart].isWhitespace()) nextStart++
      val opensNewSentence = nextStart >= text.length ||
        text[nextStart].isUpperCase() || text[nextStart].isDigit() || text[nextStart] in "\"'“‘("

      if (c == '.' && (!opensNewSentence || endsWithAbbreviation(text, i))) {
        i = end
        continue
      }

      addTrimmed(text, sentenceStart, end, out)
      sentenceStart = nextStart
      i = maxOf(nextStart, i + 1)
    }
    if (sentenceStart < text.length) addTrimmed(text, sentenceStart, text.length, out)
    return out
  }

  private fun addTrimmed(text: String, start: Int, end: Int, out: MutableList<Pair<Int, Int>>) {
    var s = start
    var e = end
    while (s < e && text[s].isWhitespace()) s++
    while (e > s && text[e - 1].isWhitespace()) e--
    if (e > s) out += s to e
  }

  private fun endsWithAbbreviation(text: String, periodIndex: Int): Boolean {
    var start = periodIndex
    while (start > 0 && (text[start - 1].isLetter() || text[start - 1] == '.')) start--
    val token = text.substring(start, periodIndex).lowercase()
    if (token.isEmpty()) return false
    val letters = token.replace(".", "")
    return letters.length <= 2 || token.trimEnd('.') in abbreviations
  }

  /** Ported from the prototype's `anSyllables`: strip non-letters, one syllable
   * for short words, trim a silent ending, then count vowel groups. */
  private fun countSyllables(word: String): Int {
    var w = word.lowercase().replace(nonAlphaRegex, "")
    if (w.length <= 3) return if (w.isEmpty()) 0 else 1
    w = w.replace(syllableTrimRegex, "").replace(leadingYRegex, "")
    val groups = vowelGroupRegex.findAll(w).count()
    return if (groups > 0) groups else 1
  }

  /** Flesch-Kincaid grade. `0` when there is nothing to score, matching the
   * prototype's `anGrade`. */
  private fun grade(words: Int, syllables: Int, sentences: Int): Double {
    if (words == 0 || sentences == 0) return 0.0
    return 0.39 * (words.toDouble() / sentences) +
      11.8 * (syllables.toDouble() / words) -
      15.59
  }

  private class Candidate(val start: Int, val end: Int, val category: FlagCategory, val reason: String)

  /** Ported from the prototype's `anFlag`: wordy phrases, `be`/`get` + participle
   * passives, complex words, weak qualifiers and `-ly` adverbs, with the same
   * longest-wins overlap resolution. Offsets are relative to [sentence]. */
  private fun flagWords(
    sentence: String,
    enabled: Set<ReadabilityCheck>,
    counts: MutableMap<FlagCategory, Int>,
  ): List<WordFlag> {
    val lower = sentence.lowercase()
    val candidates = mutableListOf<Candidate>()

    if (ReadabilityCheck.ComplexWords in enabled) {
      for ((phrase, alternative) in ReadabilityLexicon.COMPLEX_PHRASES) {
        var index = lower.indexOf(phrase)
        while (index != -1) {
          val boundaryBefore = index == 0 || !isWordChar(lower[index - 1])
          val afterIndex = index + phrase.length
          val boundaryAfter = afterIndex >= lower.length || !isWordChar(lower[afterIndex])
          if (boundaryBefore && boundaryAfter) {
            candidates += Candidate(
              index, afterIndex, FlagCategory.Complex,
              if (alternative.isNotEmpty()) "Wordy. Try “$alternative”." else "Wordy phrase. Cut it.",
            )
          }
          index = lower.indexOf(phrase, afterIndex)
        }
      }
    }

    if (ReadabilityCheck.PassiveVoice in enabled) {
      for (match in passiveRegex.findAll(sentence)) {
        val tail = match.groupValues[1].lowercase()
        val looksParticiple = trailingEdRegex.containsMatchIn(tail) && tail !in ReadabilityLexicon.WEAK_WORDS
        if (looksParticiple || tail in ReadabilityLexicon.PARTICIPLES) {
          candidates += Candidate(
            match.range.first, match.range.last + 1, FlagCategory.Passive,
            "Passive voice. Name who does it.",
          )
        }
      }
    }

    for (match in flagWordRegex.findAll(sentence)) {
      val word = match.value.lowercase().replace("’", "").replace("'", "")
      val start = match.range.first
      val end = match.range.last + 1
      if (candidates.any { start < it.end && end > it.start }) continue
      when {
        ReadabilityCheck.ComplexWords in enabled && ReadabilityLexicon.COMPLEX.containsKey(word) -> {
          val alternative = ReadabilityLexicon.COMPLEX.getValue(word)
          candidates += Candidate(
            start, end, FlagCategory.Complex,
            if (alternative.isNotEmpty()) "Complex word. Try “$alternative”." else "Complex word. Simplify it.",
          )
        }

        ReadabilityCheck.WeakQualifiers in enabled && word in ReadabilityLexicon.WEAK_WORDS ->
          candidates += Candidate(start, end, FlagCategory.Adverb, "Weak qualifier. Cut it or commit.")

        ReadabilityCheck.Adverbs in enabled &&
          word.endsWith("ly") && word.length > 4 && word !in ReadabilityLexicon.LY_STOP ->
          candidates += Candidate(
            start, end, FlagCategory.Adverb, "Adverb. A stronger verb often does more.",
          )
      }
    }

    candidates.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))
    val kept = mutableListOf<Candidate>()
    for (candidate in candidates) {
      if (kept.none { candidate.start < it.end && candidate.end > it.start }) kept += candidate
    }
    kept.sortBy { it.start }
    return kept.map {
      counts.merge(it.category, 1, Int::plus)
      WordFlag(it.start, it.end, it.category, it.reason)
    }
  }

  private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

  private fun draftMarkerNotes(text: String, blockIndex: Int): List<ReadabilityNote> {
    val out = mutableListOf<ReadabilityNote>()
    for (match in draftMarkerRegex.findAll(text)) {
      out += ReadabilityNote(
        NoteKind.DraftMarker,
        "A draft marker is still in the text: “${match.value}”.",
        listOf(NoteSpan(blockIndex, match.range.first, match.range.last + 1)),
      )
    }
    for (match in bracketPlaceholderRegex.findAll(text)) {
      out += ReadabilityNote(
        NoteKind.DraftMarker,
        "A bracketed placeholder is still in the text: “${match.value}”.",
        listOf(NoteSpan(blockIndex, match.range.first, match.range.last + 1)),
      )
    }
    return out
  }

  private fun repeatedWordNotes(words: List<WordOccurrence>): List<ReadabilityNote> {
    val lastSeenAt = HashMap<String, Int>()
    val spansByWord = LinkedHashMap<String, LinkedHashSet<NoteSpan>>()
    words.forEachIndexed { position, occ ->
      if (occ.lower.length >= MIN_REPEAT_LENGTH && occ.lower !in repeatStopWords) {
        val previous = lastSeenAt[occ.lower]
        if (previous != null && position - previous <= REPEAT_WINDOW) {
          val spans = spansByWord.getOrPut(occ.lower) { LinkedHashSet() }
          val prior = words[previous]
          spans += NoteSpan(prior.blockIndex, prior.start, prior.end)
          spans += NoteSpan(occ.blockIndex, occ.start, occ.end)
        }
        lastSeenAt[occ.lower] = position
      }
    }
    return spansByWord.map { (word, spans) ->
      ReadabilityNote(
        NoteKind.RepeatedWord,
        "“$word” repeats close to itself. Vary it or cut one.",
        spans.toList(),
      )
    }
  }

  private fun openerMonotonyNotes(openers: List<OpenerOccurrence>): List<ReadabilityNote> {
    val out = mutableListOf<ReadabilityNote>()
    var runStart = 0
    while (runStart < openers.size) {
      var runEnd = runStart
      while (runEnd + 1 < openers.size && openers[runEnd + 1].lower == openers[runStart].lower) runEnd++
      val length = runEnd - runStart + 1
      if (length >= OPENER_RUN) {
        out += ReadabilityNote(
          NoteKind.SameOpener,
          "$length sentences in a row open with “${openers[runStart].lower}”.",
          (runStart..runEnd).map { NoteSpan(openers[it].blockIndex, openers[it].start, openers[it].end) },
        )
      }
      runStart = runEnd + 1
    }
    val thereOpeners = openers.filter { it.lower == "there" }
    if (thereOpeners.size >= THERE_OPENER_LIMIT) {
      out += ReadabilityNote(
        NoteKind.SameOpener,
        "${thereOpeners.size} sentences open with “there”. A direct subject reads stronger.",
        thereOpeners.map { NoteSpan(it.blockIndex, it.start, it.end) },
      )
    }
    return out
  }
}
