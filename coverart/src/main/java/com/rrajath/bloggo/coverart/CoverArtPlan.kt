package com.rrajath.bloggo.coverart

import kotlin.math.PI

/**
 * A resolved cover, described in plain data before anything is rasterised.
 *
 * Keeping the plan free of `android.graphics` is what lets the interesting part
 * (determinism, shape distribution, the exact order of random draws) be unit
 * tested on the JVM without a device.
 */
data class CoverArtPlan(
  val seed: Int,
  val width: Int,
  val height: Int,
  val shapes: List<CoverShape>,
  val rules: List<Rule>,
  val ruleWidth: Float,
) {
  /** A hairline register mark, the printing artefact that sells the riso look. */
  data class Rule(val y: Float)
}

sealed interface CoverShape {
  /** Index into [CoverArtPalette.inks]. */
  val inkIndex: Int
  val alpha: Float

  data class Disc(
    override val inkIndex: Int,
    override val alpha: Float,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
  ) : CoverShape

  /** A rotated bar sweeping the full width, drawn about the canvas centre. */
  data class Band(
    override val inkIndex: Int,
    override val alpha: Float,
    val rotationRadians: Float,
    val offsetY: Float,
    val thickness: Float,
  ) : CoverShape

  data class Wedge(
    override val inkIndex: Int,
    override val alpha: Float,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val startRadians: Float,
    val sweepRadians: Float,
  ) : CoverShape
}

object CoverArtPlanner {

  /**
   * Draws the plan.
   *
   * The order of [MulberryRng.next] calls is load bearing: it mirrors `paint()`
   * in the prototype exactly, so the same seed yields the same picture on both
   * platforms. Reordering these lines silently changes every existing cover.
   *
   * The generator is left positioned immediately after the register marks, which
   * is where the renderer picks it up for per pixel grain.
   */
  fun plan(rng: MulberryRng, seed: Int, width: Int, height: Int): CoverArtPlan {
    val w = width.toFloat()
    val h = height.toFloat()

    val formCount = 3 + rng.nextInt(3)
    val shapes = ArrayList<CoverShape>(formCount)

    repeat(formCount) {
      val alpha = rng.nextBetween(0.18, 0.48).toFloat()
      val ink = rng.nextInt(CoverArtPalette.INK_COUNT)
      when (rng.nextInt(3)) {
        0 -> shapes += CoverShape.Disc(
          inkIndex = ink,
          alpha = alpha,
          centerX = (rng.next() * w).toFloat(),
          centerY = (rng.next() * h).toFloat(),
          radius = (rng.nextBetween(0.22, 0.66) * w).toFloat(),
        )

        1 -> shapes += CoverShape.Band(
          inkIndex = ink,
          alpha = alpha,
          rotationRadians = ((rng.next() - 0.5) * 1.5).toFloat(),
          offsetY = ((rng.next() - 0.5) * h * 0.7).toFloat(),
          thickness = (rng.nextBetween(0.06, 0.26) * h).toFloat(),
        )

        else -> {
          val cx = (rng.next() * w).toFloat()
          val cy = (rng.next() * h).toFloat()
          val radius = (rng.nextBetween(0.4, 1.0) * w).toFloat()
          val start = (rng.next() * PI * 2).toFloat()
          shapes += CoverShape.Wedge(
            inkIndex = ink,
            alpha = alpha,
            centerX = cx,
            centerY = cy,
            radius = radius,
            startRadians = start,
            sweepRadians = rng.nextBetween(0.6, 1.7).toFloat(),
          )
        }
      }
    }

    val rules = List(RULE_COUNT) { CoverArtPlan.Rule(y = (h * rng.nextBetween(0.2, 0.85)).toFloat()) }

    return CoverArtPlan(
      seed = seed,
      width = width,
      height = height,
      shapes = shapes,
      rules = rules,
      ruleWidth = maxOf(1f, w / 380f),
    )
  }

  const val RULE_COUNT = 3

  /** Amplitude of the per pixel grain, matching `(r() - 0.5) * 26` in the prototype. */
  const val GRAIN_AMPLITUDE = 26.0
}
