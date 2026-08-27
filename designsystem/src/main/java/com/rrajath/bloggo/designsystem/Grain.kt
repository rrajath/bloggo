package com.rrajath.bloggo.designsystem

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.rrajath.bloggo.coverart.MulberryRng
import kotlin.math.roundToInt

/**
 * The paper texture, matching the prototype's `feTurbulence` overlay.
 *
 * A pre-baked tiling bitmap rather than an AGSL shader. minSdk 34 would allow
 * `RuntimeShader`, but a 140px tile repeated across the screen is
 * indistinguishable at this opacity and costs one texture upload instead of a
 * fragment program per frame.
 *
 * Note this is a different mechanism from the grain inside a generated cover,
 * which is applied per pixel at render time so that an exported cover carries
 * its own noise.
 */
private const val TILE = 140

private fun buildNoiseTile(dark: Boolean): ImageBitmap {
  val rng = MulberryRng(if (dark) 2 else 1)
  val pixels = IntArray(TILE * TILE)
  for (i in pixels.indices) {
    // Centred on mid grey so multiply darkens and screen lightens symmetrically.
    // Both the alpha and the spread are deliberately low: this is a hint of tooth
    // in the paper, and anything stronger reads as a dirty screen rather than as
    // texture. It should be invisible until you look for it.
    val value = (128 + (rng.next() - 0.5) * 46).roundToInt().coerceIn(0, 255)
    pixels[i] = AndroidColor.argb(30, value, value, value)
  }
  return Bitmap.createBitmap(pixels, TILE, TILE, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * The two tiles the app can ever need, built at most once each for the process.
 *
 * [buildNoiseTile] runs a 19,600-iteration loop and allocates a 78KB bitmap, and
 * it used to do that during first composition and again on every light/dark
 * flip. Neither tile depends on anything but its own seed, so neither has any
 * reason to be rebuilt.
 */
private val lightGrainBrush: Brush by lazy { grainBrushFor(dark = false) }
private val darkGrainBrush: Brush by lazy { grainBrushFor(dark = true) }

private fun grainBrushFor(dark: Boolean): Brush =
  ShaderBrush(ImageShader(buildNoiseTile(dark), TileMode.Repeated, TileMode.Repeated))

/**
 * Overlays paper grain on whatever is drawn beneath.
 *
 * Apply once, high in the tree, on the surface that represents paper. Applying
 * it per component stacks the texture and reads as dirt.
 *
 * A [DrawModifierNode] rather than `composed {}`: the latter is deprecated, and
 * it opts the modifier out of Compose's modifier reuse and skipping. Reading the
 * theme still has to happen in composition, so this stays `@Composable` and
 * hands the node a plain boolean.
 */
@Composable
fun Modifier.paperGrain(): Modifier = this then PaperGrainElement(BloggoTheme.colors.isDark)

private data class PaperGrainElement(val dark: Boolean) : ModifierNodeElement<PaperGrainNode>() {
  override fun create(): PaperGrainNode = PaperGrainNode(dark)

  override fun update(node: PaperGrainNode) {
    node.dark = dark
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "paperGrain"
    properties["dark"] = dark
  }
}

private class PaperGrainNode(dark: Boolean) : Modifier.Node(), DrawModifierNode {
  var dark: Boolean = dark
    set(value) {
      if (field != value) {
        field = value
        invalidateDraw()
      }
    }

  override fun ContentDrawScope.draw() {
    drawContent()
    drawRect(
      brush = if (dark) darkGrainBrush else lightGrainBrush,
      blendMode = if (dark) BlendMode.Screen else BlendMode.Multiply,
    )
  }
}
