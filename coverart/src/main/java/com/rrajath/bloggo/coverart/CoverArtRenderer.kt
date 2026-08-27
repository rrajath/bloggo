package com.rrajath.bloggo.coverart

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.OutputStream
import kotlin.math.roundToInt

/** Where a cover is going, which is the only thing that changes between uses. */
enum class CoverArtSize(val width: Int, val height: Int) {
  /** Row thumbnail in the library. */
  Thumbnail(120, 120),

  /** Hero card at the top of the library. */
  Hero(480, 270),

  /** What gets written to the repo and used as the Open Graph image. */
  Export(1200, 630),
}

object CoverArtRenderer {

  /**
   * Renders the cover for [seed] into a new bitmap.
   *
   * Deliberately one rendering path for every use. An earlier sketch drew
   * thumbnails with cheap Compose primitives and only rasterised on export,
   * which meant the picture you approved was not quite the picture you shipped.
   */
  fun render(
    seed: Int,
    width: Int,
    height: Int,
    palette: CoverArtPalette,
    grain: Boolean = true,
  ): Bitmap {
    require(width > 0 && height > 0) { "cover must have a positive size, got ${width}x$height" }

    val rng = MulberryRng(seed)
    val plan = CoverArtPlanner.plan(rng, seed, width, height)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(palette.paper)

    drawShapes(canvas, plan, palette)
    drawRules(canvas, plan, palette)
    // The generator is now exactly where the prototype's grain loop picks it up.
    if (grain) applyGrain(bitmap, rng)

    return bitmap
  }

  fun render(
    seed: Int,
    size: CoverArtSize,
    palette: CoverArtPalette,
    grain: Boolean = true,
  ): Bitmap = render(seed, size.width, size.height, palette, grain)

  /**
   * Renders at [CoverArtSize.Export] and writes a PNG, which is what the commit
   * flow attaches to the post. PNG rather than WebP so the bytes are identical
   * on every device and the diff stays stable when nothing has changed.
   */
  fun writePng(
    seed: Int,
    palette: CoverArtPalette,
    out: OutputStream,
    size: CoverArtSize = CoverArtSize.Export,
  ) {
    val bitmap = render(seed, size, palette)
    try {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } finally {
      bitmap.recycle()
    }
  }

  private fun drawShapes(canvas: Canvas, plan: CoverArtPlan, palette: CoverArtPalette) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      blendMode = when (palette.blend) {
        CoverBlend.Multiply -> BlendMode.MULTIPLY
        CoverBlend.Screen -> BlendMode.SCREEN
      }
    }
    val width = plan.width.toFloat()
    val height = plan.height.toFloat()

    for (shape in plan.shapes) {
      paint.color = palette.inks[shape.inkIndex]
      paint.alpha = (shape.alpha * 255f).roundToInt().coerceIn(0, 255)

      when (shape) {
        is CoverShape.Disc ->
          canvas.drawCircle(shape.centerX, shape.centerY, shape.radius, paint)

        is CoverShape.Band -> {
          canvas.save()
          canvas.translate(width / 2f, height / 2f)
          canvas.rotate(Math.toDegrees(shape.rotationRadians.toDouble()).toFloat())
          canvas.drawRect(
            -width,
            shape.offsetY,
            width,
            shape.offsetY + shape.thickness,
            paint,
          )
          canvas.restore()
        }

        is CoverShape.Wedge -> {
          val oval = RectF(
            shape.centerX - shape.radius,
            shape.centerY - shape.radius,
            shape.centerX + shape.radius,
            shape.centerY + shape.radius,
          )
          val path = Path().apply {
            moveTo(shape.centerX, shape.centerY)
            arcTo(
              oval,
              Math.toDegrees(shape.startRadians.toDouble()).toFloat(),
              Math.toDegrees(shape.sweepRadians.toDouble()).toFloat(),
            )
            close()
          }
          canvas.drawPath(path, paint)
        }
      }
    }
  }

  private fun drawRules(canvas: Canvas, plan: CoverArtPlan, palette: CoverArtPalette) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      // Register marks sit on top rather than blending, as in the prototype.
      blendMode = BlendMode.SRC_OVER
      color = palette.rule
      alpha = (palette.ruleAlpha * 255f).roundToInt().coerceIn(0, 255)
      strokeWidth = plan.ruleWidth
    }
    for (rule in plan.rules) {
      canvas.drawLine(0f, rule.y, plan.width.toFloat(), rule.y, paint)
    }
  }

  /**
   * Adds the same signed offset to R, G and B per pixel, which is what makes the
   * result read as paper grain rather than colour noise.
   */
  private fun applyGrain(bitmap: Bitmap, rng: MulberryRng) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    // Unpacked and repacked by hand rather than through Color.argb/red/green/blue.
    // This is eight static calls per pixel on a loop that runs 129,600 times for
    // a hero and 756,000 for an export. The arithmetic is what those helpers do,
    // so the bytes are identical — which matters, because the cover for a given
    // slug has to stay stable (see MulberryRng's notes on draw order).
    for (i in pixels.indices) {
      val noise = ((rng.next() - 0.5) * CoverArtPlanner.GRAIN_AMPLITUDE).roundToInt()
      val pixel = pixels[i]
      val red = (((pixel shr 16) and 0xFF) + noise).coerceIn(0, 255)
      val green = (((pixel shr 8) and 0xFF) + noise).coerceIn(0, 255)
      val blue = ((pixel and 0xFF) + noise).coerceIn(0, 255)
      pixels[i] = (pixel and 0xFF000000.toInt()) or (red shl 16) or (green shl 8) or blue
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
  }
}
