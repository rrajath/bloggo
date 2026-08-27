package com.rrajath.bloggo.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoColors
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.component.MetaText
import com.rrajath.bloggo.designsystem.component.StatLine
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/**
 * A Hemingway-style pass over the current draft, reached from an accent button
 * in the editor toolbar. Read-only: fixing a flagged sentence means going back
 * to Edit. Structure mirrors the prototype's `review` screen — a pinned summary
 * and legend over the scrolling prose, with the `--an-*` washes stacked on
 * flagged sentences and words. Tapping a highlight shows the reason as a toast.
 */
@Composable
fun ReviewScreen(
  markdown: String,
  enabledChecks: Set<ReadabilityCheck>,
  onBack: () -> Unit,
  onToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val report = remember(markdown, enabledChecks) {
    ReadabilityAnalyzer.analyze(markdown, enabledChecks)
  }

  Column(modifier.fillMaxSize().background(colors.paper)) {
    ReviewHeader(onBack = onBack)

    Summary(report)
    Legend(report, enabledChecks)

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp)
    ) {
      if (report.blocks.isEmpty()) {
        MetaText(
          "Nothing to analyze yet. Write a few sentences and come back.",
          color = colors.inkFaint,
          maxLines = 3,
        )
      } else {
        report.blocks.forEach { block -> ReviewBlock(block, onToast) }
      }

      if (report.notes.isNotEmpty()) {
        Eyebrow("Notes")
        report.notes.forEach { note ->
          Row(Modifier.padding(bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("·", style = BloggoTheme.type.body, color = colors.inkFaint)
            Text(note.message, style = BloggoTheme.type.body, color = colors.inkMuted)
          }
        }
      }
    }
  }
}

@Composable
private fun ReviewHeader(onBack: () -> Unit) {
  val colors = BloggoTheme.colors
  Column {
    Row(
      Modifier
        .fillMaxWidth()
        .background(colors.paper)
        .padding(start = 4.dp, end = 10.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      BloggoIconButton(BloggoIcons.ChevronLeft, "Back to editor", onBack)
      Column(Modifier.weight(1f)) {
        Text("Review", style = BloggoTheme.type.rowTitle, color = colors.ink)
        MetaText("tap a highlight to see why", color = colors.inkFaint)
      }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
  }
}

@Composable
private fun Summary(report: ReadabilityReport) {
  val colors = BloggoTheme.colors
  val hard = report.counts.getValue(FlagCategory.Hard) + report.counts.getValue(FlagCategory.VeryHard)
  Column(Modifier.background(colors.paper).padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
    StatLine(
      listOf(
        (if (report.grade > 0) report.grade.toString() else "-") to "Grade level",
        hard.toString() to "Hard sentences",
        report.counts.getValue(FlagCategory.Passive).toString() to "Passive voice",
      )
    )
    Text(
      buildString {
        append("%,d".format(report.wordCount)).append(pluralize(report.wordCount, " word"))
        append(" · ")
        append(report.sentenceCount).append(pluralize(report.sentenceCount, " sentence"))
        append(" · ")
        append(report.adverbCount).append(pluralize(report.adverbCount, " adverb"))
      },
      style = BloggoTheme.type.meta,
      color = colors.inkFaint,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
    )
  }
}

private fun pluralize(count: Int, singular: String): String =
  if (count == 1) singular else singular + "s"

@Composable
private fun Legend(report: ReadabilityReport, enabled: Set<ReadabilityCheck>) {
  val colors = BloggoTheme.colors
  val rows = buildList {
    if (ReadabilityCheck.HardSentences in enabled) {
      add(LegendRow(FlagCategory.Hard, "Hard to read", report.counts.getValue(FlagCategory.Hard)))
      add(LegendRow(FlagCategory.VeryHard, "Very hard", report.counts.getValue(FlagCategory.VeryHard)))
    }
    if (ReadabilityCheck.ComplexWords in enabled) {
      add(LegendRow(FlagCategory.Complex, "Complex word", report.counts.getValue(FlagCategory.Complex)))
    }
    if (ReadabilityCheck.Adverbs in enabled || ReadabilityCheck.WeakQualifiers in enabled) {
      add(LegendRow(FlagCategory.Adverb, "Adverb / weak word", report.counts.getValue(FlagCategory.Adverb)))
    }
    if (ReadabilityCheck.PassiveVoice in enabled) {
      add(LegendRow(FlagCategory.Passive, "Passive voice", report.counts.getValue(FlagCategory.Passive)))
    }
  }
  if (rows.isEmpty()) return

  Column {
    FlowRow(
      Modifier
        .fillMaxWidth()
        .background(colors.paper)
        .padding(start = 18.dp, end = 18.dp, top = 13.dp, bottom = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      rows.forEach { row ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
          Box(
            Modifier
              .size(12.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(swatchColor(row.category, colors))
          )
          Text(row.label, style = BloggoTheme.type.cellSubtitle, color = colors.inkMuted)
          Text(
            row.count.toString(),
            style = BloggoTheme.type.metaSmall,
            color = colors.inkFaint,
          )
        }
      }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
  }
}

private data class LegendRow(val category: FlagCategory, val label: String, val count: Int)

@Composable
private fun ReviewBlock(block: RenderBlock, onToast: (String) -> Unit) {
  val colors = BloggoTheme.colors
  val type = BloggoTheme.type
  when (block) {
    is RenderBlock.Heading -> Text(
      block.text,
      style = if (block.level <= 2) type.articleHeading else type.articleSubheading,
      color = colors.ink,
      modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )

    is RenderBlock.Prose -> when (block.kind) {
      ProseKind.Paragraph -> WashText(
        block, type.articleBody, colors.ink, onToast,
        modifier = Modifier.padding(bottom = 15.dp),
      )

      ProseKind.Quote -> Row(Modifier.padding(vertical = 18.dp)) {
        Box(Modifier.width(2.dp).height(48.dp).background(colors.rule))
        WashText(
          block, type.articleBody, colors.inkMuted, onToast,
          modifier = Modifier.padding(start = 15.dp),
        )
      }

      ProseKind.ListItem -> Row(Modifier.padding(bottom = 7.dp)) {
        Text(
          block.marker.orEmpty(),
          style = type.meta.copy(fontSize = type.monoField.fontSize),
          color = colors.accent,
          modifier = Modifier.width(24.dp),
        )
        WashText(block, type.articleBody, colors.ink, onToast)
      }
    }
  }
}

private const val ReasonTag = "why"

@Composable
private fun WashText(
  block: RenderBlock.Prose,
  style: TextStyle,
  color: Color,
  onReason: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val hasHighlights = block.wordFlags.isNotEmpty() || block.sentences.any { it.flag != null }
  if (!hasHighlights) {
    Text(block.text, style = style, color = color, modifier = modifier)
    return
  }

  val annotated = remember(block, colors) {
    buildAnnotatedString {
      append(block.text)
      block.sentences.forEach { sentence ->
        sentence.flag?.let { addStyle(SpanStyle(background = washColor(it, colors)), sentence.start, sentence.end) }
      }
      block.wordFlags.forEach { flag ->
        addStyle(SpanStyle(background = washColor(flag.category, colors)), flag.start, flag.end)
      }
      block.wordFlags.forEach { flag ->
        addStringAnnotation(ReasonTag, flag.reason, flag.start, flag.end)
      }
      block.sentences.forEach { sentence ->
        sentence.flag?.let { addStringAnnotation(ReasonTag, sentenceReason(it), sentence.start, sentence.end) }
      }
    }
  }

  ClickableText(
    text = annotated,
    style = style.copy(color = color),
    modifier = modifier,
    onClick = { offset ->
      annotated.getStringAnnotations(ReasonTag, offset, offset)
        .minByOrNull { it.end - it.start }
        ?.let { onReason(it.item) }
    },
  )
}

private fun sentenceReason(flag: FlagCategory): String = when (flag) {
  FlagCategory.VeryHard -> "Very hard to read. Split this sentence."
  else -> "Hard to read. Shorten or split it."
}

private fun washColor(category: FlagCategory, colors: BloggoColors): Color = when (category) {
  FlagCategory.Hard -> colors.analysisHard
  FlagCategory.VeryHard -> colors.analysisVeryHard
  FlagCategory.Complex -> colors.analysisComplex
  FlagCategory.Adverb -> colors.analysisAdverb
  FlagCategory.Passive -> colors.analysisPassive
}

private fun swatchColor(category: FlagCategory, colors: BloggoColors): Color = when (category) {
  FlagCategory.Hard -> colors.analysisHardInk
  FlagCategory.VeryHard -> colors.analysisVeryHardInk
  FlagCategory.Complex -> colors.analysisComplexInk
  FlagCategory.Adverb -> colors.analysisAdverbInk
  FlagCategory.Passive -> colors.analysisPassiveInk
}

@Preview(heightDp = 900)
@Composable
private fun ReviewScreenPreview() {
  BloggoTheme {
    ReviewScreen(
      markdown = SampleData.draftMarkdown,
      enabledChecks = ReadabilityCheck.All,
      onBack = {},
      onToast = {},
    )
  }
}
