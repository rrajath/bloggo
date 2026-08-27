package com.rrajath.bloggo.coverart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws the cover for [seed].
 *
 * The bitmap comes from [CoverArtCache], which outlives the composable — lazy
 * layouts dispose off-screen rows, so a `remember` here re-rasterised every row
 * that scrolled back into view. A cache miss renders on [Dispatchers.Default]
 * and shows flat paper for the frame or two until it lands, so composition never
 * rasterises: `render` allocates the bitmap, draws it, and then walks every
 * pixel again to apply grain. [CoverArtSize.Export] is a background job and is
 * never called from here at all.
 */
@Composable
fun CoverArt(
  seed: Int,
  modifier: Modifier = Modifier,
  size: CoverArtSize = CoverArtSize.Hero,
  palette: CoverArtPalette = CoverArtPalette.of(isSystemInDarkTheme()),
  contentDescription: String? = null,
) {
  val key = remember(seed, size, palette) {
    CoverArtCache.Key(seed, size.width, size.height, palette)
  }
  // A hit is synchronous, so a cover that has been drawn once never flashes its
  // placeholder again — including on the first composition after a scroll back.
  val image by produceState(initialValue = CoverArtCache.peek(key), key) {
    if (value == null) value = withContext(Dispatchers.Default) { CoverArtCache.getOrRender(key) }
  }

  val rendered = image
  if (rendered != null) {
    Image(
      bitmap = rendered,
      contentDescription = contentDescription,
      modifier = modifier,
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(modifier.background(Color(palette.paper)))
  }
}

/** Convenience for the common case of seeding from a post slug. */
@Composable
fun CoverArt(
  slug: String,
  variant: Int = 0,
  modifier: Modifier = Modifier,
  size: CoverArtSize = CoverArtSize.Hero,
  palette: CoverArtPalette = CoverArtPalette.of(isSystemInDarkTheme()),
  contentDescription: String? = null,
) = CoverArt(
  seed = coverSeedOf(slug, variant),
  modifier = modifier,
  size = size,
  palette = palette,
  contentDescription = contentDescription,
)

@Preview(widthDp = 200, heightDp = 340)
@Composable
private fun CoverArtPreview() {
  androidx.compose.foundation.layout.Column {
    CoverArt("on-agents-that-actually-ship", size = CoverArtSize.Hero, modifier = Modifier.size(180.dp, 101.dp))
    CoverArt("why-i-left-obsidian", size = CoverArtSize.Thumbnail, modifier = Modifier.size(52.dp))
    CoverArt("a-small-note-on-taste", size = CoverArtSize.Thumbnail, modifier = Modifier.size(52.dp))
    CoverArt("the-cost-of-a-good-abstraction", size = CoverArtSize.Thumbnail, modifier = Modifier.size(52.dp))
  }
}
