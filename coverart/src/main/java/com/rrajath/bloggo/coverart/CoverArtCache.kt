package com.rrajath.bloggo.coverart

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Process-wide cache of rendered covers.
 *
 * [androidx.compose.runtime.remember] is not a cache for this: it is scoped to a
 * composable instance, and a `LazyColumn` disposes rows that scroll off-screen.
 * Every row that scrolled back into view was re-rasterising a bitmap from
 * scratch — allocating it, drawing it, then walking every pixel again to apply
 * grain — for a picture that is a pure function of its key.
 *
 * Sized in bytes rather than entries because the sizes in play differ by an
 * order of magnitude: a [CoverArtSize.Thumbnail] is 14,400 pixels and a
 * [CoverArtSize.Hero] is 129,600.
 */
object CoverArtCache {

  /** Everything [CoverArtRenderer.render] reads. Equal keys, identical bitmap. */
  data class Key(
    val seed: Int,
    val width: Int,
    val height: Int,
    val palette: CoverArtPalette,
  )

  /** Roughly sixteen heroes, or a library's worth of thumbnails and then some. */
  private const val MAX_BYTES = 8 * 1024 * 1024

  private val cache = object : LruCache<Key, ImageBitmap>(MAX_BYTES) {
    override fun sizeOf(key: Key, value: ImageBitmap): Int = key.width * key.height * 4
  }

  /** The cached bitmap for [key], or null if it has not been rendered yet. */
  fun peek(key: Key): ImageBitmap? = cache.get(key)

  /**
   * The bitmap for [key], rendering it if it is not already cached.
   *
   * Call this off the main thread: a miss rasterises. [peek] is the
   * composition-safe half.
   */
  fun getOrRender(key: Key): ImageBitmap = cache.get(key) ?: run {
    val rendered = CoverArtRenderer.render(key.seed, key.width, key.height, key.palette).asImageBitmap()
    cache.put(key, rendered)
    rendered
  }

  /** For tests, and for a palette change that invalidates everything at once. */
  fun clear() = cache.evictAll()
}
