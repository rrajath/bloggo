package com.rrajath.bloggo.ui.preview

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoColors
import com.rrajath.bloggo.designsystem.BloggoFonts
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.LinkBar
import com.rrajath.bloggo.designsystem.component.SegmentedControl
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.StagedMedia
import com.rrajath.bloggo.model.effectiveDate
import com.rrajath.bloggo.model.formatFrontmatterDate
import com.rrajath.bloggo.model.parseTagList
import com.rrajath.bloggo.ui.editor.EditorMode

/**
 * Read mode.
 *
 * Two states, and the difference matters: reached from the editor this is an
 * unpublished draft whose permalink does not resolve yet, reached from a Live row
 * it is the real page. Showing the same chrome for both would make the Open
 * button lie.
 */
@Composable
fun PreviewScreen(
  post: Post,
  published: Boolean,
  siteHost: String,
  markdown: String,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onShare: () -> Unit,
  onOpenLive: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val blocks = remember(markdown) { ArticleParser.parse(markdown) }

  Column(modifier.fillMaxSize()) {
    Row(
      Modifier
        .fillMaxWidth()
        .background(colors.paper)
        .padding(start = 4.dp, end = 10.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      BloggoIconButton(BloggoIcons.ChevronLeft, "Back to editor", onBack)
      Text("Preview", style = BloggoTheme.type.rowTitle, color = colors.ink, modifier = Modifier.weight(1f))
      SegmentedControl(
        options = listOf(EditorMode.Edit, EditorMode.Read),
        selected = EditorMode.Read,
        onSelect = { if (it == EditorMode.Edit) onEdit() },
        modifier = Modifier.width(130.dp),
        label = { it.label },
      )
      BloggoIconButton(BloggoIcons.Share, "Share", onShare)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))

    LinkBar(
      url = post.liveUrl(siteHost),
      published = published,
      onOpen = onOpenLive,
    )

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
      val front = blocks.frontmatter
      Text(
        front["tags"]?.parseTagList()?.takeIf { it.isNotEmpty() }
          ?.joinToString(" · ") { it.uppercase() }
          ?: "NOTES",
        style = BloggoTheme.type.eyebrow,
        color = colors.inkFaint,
        modifier = Modifier.padding(bottom = 10.dp),
      )
      Text(
        if (published) post.title else front["title"]?.trim('"', '\'') ?: post.title,
        style = BloggoTheme.type.articleTitle,
        color = colors.ink,
      )
      Row(
        Modifier.padding(top = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (published) {
          BloggoChip("Live", ChipTone.Live)
        } else {
          BloggoChip("Draft preview", ChipTone.Draft)
        }
        Text(
          "${front.effectiveDate()?.let(::formatFrontmatterDate).orEmpty()} · ${maxOf(1, blocks.wordCount / 200)} min read",
          style = BloggoTheme.type.meta,
          color = colors.inkFaint,
        )
      }
      Box(Modifier.fillMaxWidth().height(1.dp).background(colors.rule))

      ArticleBody(
        blocks = blocks.blocks,
        connection = connection,
        token = token,
        stagedMedia = stagedMedia,
        modifier = Modifier.padding(top = 22.dp, bottom = 40.dp),
      )
    }
  }
}

private const val LinkAnnotationTag = "url"

/**
 * Renders [text] as plain [Text] unless it carries [InlineSpan]s or
 * [InlineLink]s, in which case bold/italic/code spans get the matching
 * character style from [inlineSpanStyle] and link spans additionally get the
 * design system's link colour, an underline, and become tappable, opening the
 * URL via an [Intent.ACTION_VIEW].
 */
@Composable
private fun LinkAwareText(
  text: String,
  links: List<InlineLink>,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  spans: List<InlineSpan> = emptyList(),
) {
  if (links.isEmpty() && spans.isEmpty()) {
    Text(text, style = style, color = color, modifier = modifier)
    return
  }

  val context = LocalContext.current
  val colors = BloggoTheme.colors
  val annotated = remember(text, links, spans, style, colors) {
    buildAnnotatedString {
      append(text)
      spans.forEach { span ->
        addStyle(inlineSpanStyle(span.style, style, colors), span.start, span.end)
      }
      links.forEach { link ->
        addStyle(SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline), link.start, link.end)
        addStringAnnotation(LinkAnnotationTag, link.url, link.start, link.end)
      }
    }
  }

  if (links.isEmpty()) {
    Text(annotated, style = style, color = color, modifier = modifier)
    return
  }

  ClickableText(
    text = annotated,
    style = style.copy(color = color),
    modifier = modifier,
    onClick = { offset ->
      annotated.getStringAnnotations(LinkAnnotationTag, offset, offset).firstOrNull()?.let { annotation ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))) }
      }
    },
  )
}

/**
 * The [SpanStyle] for one [InlineStyle], matching `.read strong`/`.read
 * em`/`.read code` in the prototype: bold is 600 weight (not a heavier 700,
 * so it sits with the rest of the type system's weights), italic borrows
 * whichever italic face is already loaded for [base]'s family, and code
 * switches to mono at 0.84x the surrounding size with the design system's
 * inline-code colour and tint background. A rounded background (the
 * prototype's `border-radius:5px`) isn't representable by [SpanStyle], so
 * code spans render with a plain rectangular highlight instead.
 */
private fun inlineSpanStyle(kind: InlineStyle, base: TextStyle, colors: BloggoColors): SpanStyle =
  when (kind) {
    InlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.SemiBold)
    InlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    InlineStyle.Code -> SpanStyle(
      fontFamily = BloggoFonts.Mono,
      fontSize = base.fontSize * 0.84f,
      color = colors.accentBright,
      background = colors.accentTint,
    )
  }

@Composable
private fun ArticleBody(
  blocks: List<ArticleBlock>,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val type = BloggoTheme.type
  var viewingImage by remember { mutableStateOf<String?>(null) }

  Column(modifier) {
    blocks.forEach { block ->
      when (block) {
        is ArticleBlock.Heading -> LinkAwareText(
          text = block.text,
          links = block.links,
          spans = block.spans,
          style = if (block.level <= 2) type.articleHeading else type.articleSubheading,
          color = colors.ink,
          modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
        )

        is ArticleBlock.Paragraph -> LinkAwareText(
          text = block.text,
          links = block.links,
          spans = block.spans,
          style = type.articleBody,
          color = colors.ink,
          modifier = Modifier.padding(bottom = 15.dp),
        )

        is ArticleBlock.Quote -> Row(Modifier.padding(vertical = 20.dp)) {
          Box(Modifier.width(2.dp).height(56.dp).background(colors.mark))
          LinkAwareText(
            text = block.text,
            links = block.links,
            spans = block.spans,
            style = type.articleQuote,
            color = colors.inkMuted,
            modifier = Modifier.padding(start = 16.dp),
          )
        }

        is ArticleBlock.ListItem -> Row(Modifier.padding(bottom = 8.dp)) {
          Text(
            block.marker,
            style = type.meta.copy(fontSize = type.monoField.fontSize),
            color = colors.accent,
            modifier = Modifier.width(30.dp),
          )
          LinkAwareText(
            text = block.text,
            links = block.links,
            spans = block.spans,
            style = type.articleBody,
            color = colors.ink,
          )
        }

        is ArticleBlock.Callout -> Box(
          Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp)
            .background(colors.amberTint, BloggoTheme.shapes.medium)
            .padding(16.dp)
        ) {
          LinkAwareText(
            text = block.text,
            links = block.links,
            spans = block.spans,
            style = type.articleBody,
            color = colors.ink,
          )
        }

        is ArticleBlock.Figure -> Column(Modifier.padding(vertical = 22.dp)) {
          RepoFigureImage(
            sitePath = block.src,
            connection = connection,
            token = token,
            stagedMedia = stagedMedia,
            contentDescription = block.caption.ifEmpty { null },
            onClick = { viewingImage = block.src },
          )
          if (block.caption.isNotEmpty()) {
            Text(
              block.caption,
              style = type.cellSubtitle,
              color = colors.inkFaint,
              modifier = Modifier.padding(top = 8.dp),
            )
          }
        }

        is ArticleBlock.CodeBlock -> Box(
          Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(BloggoTheme.shapes.medium)
            .background(colors.paperSunk)
            .horizontalScroll(rememberScrollState())
            .padding(14.dp)
        ) {
          Text(block.code, style = type.editorSource, color = colors.ink)
        }

        is ArticleBlock.Video -> if (block.src.startsWith("http://") || block.src.startsWith("https://")) {
          AndroidView(
            factory = { context ->
              VideoView(context).apply {
                setVideoURI(Uri.parse(block.src))
                setMediaController(MediaController(context).also { it.setAnchorView(this) })
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 22.dp)
              .height(200.dp)
              .clip(BloggoTheme.shapes.medium)
              .background(colors.ink),
          )
        } else {
          Box(
            Modifier
              .fillMaxWidth()
              .padding(vertical = 22.dp)
              .height(180.dp)
              .background(colors.paperSunk, BloggoTheme.shapes.medium),
            contentAlignment = Alignment.Center,
          ) {
            Text("▶ ${block.src.substringAfterLast('/')}", style = type.meta, color = colors.inkFaint)
          }
        }

        ArticleBlock.Rule -> Box(
          Modifier.fillMaxWidth().height(1.dp).background(colors.rule).padding(vertical = 26.dp)
        )
      }
    }
  }

  viewingImage?.let { sitePath ->
    RepoImageViewerDialog(
      sitePath = sitePath,
      connection = connection,
      token = token,
      stagedMedia = stagedMedia,
      onDismiss = { viewingImage = null },
    )
  }
}

@Preview(heightDp = 900)
@Composable
private fun PreviewScreenPreview() {
  BloggoTheme {
    PreviewScreen(
      post = SampleData.draft,
      published = false,
      siteHost = "rrajath.dev",
      markdown = SampleData.draftMarkdown,
      connection = RepoConnection(),
      token = null,
      stagedMedia = emptyList(),
      onBack = {},
      onEdit = {},
      onShare = {},
      onOpenLive = {},
    )
  }
}
