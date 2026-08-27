package com.rrajath.bloggo.coverart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden plans captured from the prototype's `paint()`. These pin the *order* of
 * random draws, which is the part that silently breaks: reordering two lines
 * still produces a plausible looking cover, just not the same one as before.
 */
class CoverArtPlannerTest {

  private val tolerance = 1e-4

  private fun plan(seed: Int, width: Int, height: Int): CoverArtPlan =
    CoverArtPlanner.plan(MulberryRng(seed), seed, width, height)

  @Test
  fun `seed 4 at hero size matches the prototype`() {
    val plan = plan(4, 480, 270)
    assertEquals(5, plan.shapes.size)

    val disc = plan.shapes[0] as CoverShape.Disc
    assertEquals(3, disc.inkIndex)
    assertEquals(0.407747365f, disc.alpha, 1e-6f)
    assertEquals(116.228302f, disc.centerX, tolerance.toFloat())
    assertEquals(238.899247f, disc.centerY, tolerance.toFloat())
    assertEquals(166.868389f, disc.radius, tolerance.toFloat())

    val wedge = plan.shapes[1] as CoverShape.Wedge
    assertEquals(0, wedge.inkIndex)
    assertEquals(423.172000f, wedge.centerX, tolerance.toFloat())
    assertEquals(134.566910f, wedge.centerY, tolerance.toFloat())
    assertEquals(344.960967f, wedge.radius, tolerance.toFloat())
    assertEquals(3.574053f, wedge.startRadians, tolerance.toFloat())
    assertEquals(1.539192f, wedge.sweepRadians, tolerance.toFloat())

    assertEquals(3, plan.rules.size)
    assertEquals(73.363550f, plan.rules[0].y, tolerance.toFloat())
    assertEquals(102.362092f, plan.rules[1].y, tolerance.toFloat())
    assertEquals(75.503943f, plan.rules[2].y, tolerance.toFloat())
  }

  @Test
  fun `seed 7 at thumbnail size matches the prototype, including the band`() {
    val plan = plan(7, 120, 120)
    assertEquals(5, plan.shapes.size)

    val band = plan.shapes[2] as CoverShape.Band
    assertEquals(3, band.inkIndex)
    assertEquals(0.398348552f, band.alpha, 1e-6f)
    assertEquals(-0.193640f, band.rotationRadians, tolerance.toFloat())
    assertEquals(4.953045f, band.offsetY, tolerance.toFloat())
    assertEquals(18.653151f, band.thickness, tolerance.toFloat())

    val lastDisc = plan.shapes[4] as CoverShape.Disc
    assertEquals(35.800804f, lastDisc.centerX, tolerance.toFloat())
    assertEquals(47.789516f, lastDisc.radius, tolerance.toFloat())
  }

  @Test
  fun `seed 1 at export size matches the prototype`() {
    val plan = plan(1, 1200, 630)
    assertEquals(4, plan.shapes.size)
    assertTrue(plan.shapes[0] is CoverShape.Band)
    assertTrue(plan.shapes[1] is CoverShape.Band)
    assertTrue(plan.shapes[2] is CoverShape.Wedge)
    assertTrue(plan.shapes[3] is CoverShape.Band)

    val first = plan.shapes[0] as CoverShape.Band
    assertEquals(0.583933f, first.rotationRadians, tolerance.toFloat())
    assertEquals(89.276830f, first.offsetY, tolerance.toFloat())
    assertEquals(91.412226f, first.thickness, tolerance.toFloat())
  }

  @Test
  fun `the generator is left positioned for the grain pass`() {
    // The first grain sample must be the very next draw after the register marks.
    val rng = MulberryRng(4)
    CoverArtPlanner.plan(rng, 4, 480, 270)
    val firstGrain = (rng.next() - 0.5) * CoverArtPlanner.GRAIN_AMPLITUDE
    assertEquals(-2.504238628, firstGrain, 1e-6)
  }

  @Test
  fun `same seed plans identically, different seeds do not`() {
    assertEquals(plan(42, 480, 270), plan(42, 480, 270))
    assertNotEquals(plan(42, 480, 270), plan(43, 480, 270))
  }

  @Test
  fun `shape count always lands between three and five`() {
    for (seed in 0 until 3000) {
      val size = plan(seed, 480, 270).shapes.size
      assertTrue("seed $seed produced $size shapes", size in 3..5)
    }
  }

  @Test
  fun `alpha and ink index stay inside the palette contract`() {
    for (seed in 0 until 2000) {
      plan(seed, 480, 270).shapes.forEach { shape ->
        assertTrue("alpha ${shape.alpha}", shape.alpha in 0.18f..0.48f)
        assertTrue("ink ${shape.inkIndex}", shape.inkIndex in 0 until CoverArtPalette.INK_COUNT)
      }
    }
  }

  @Test
  fun `rules stay on the canvas`() {
    for (seed in 0 until 2000) {
      val plan = plan(seed, 480, 270)
      assertEquals(CoverArtPlanner.RULE_COUNT, plan.rules.size)
      plan.rules.forEach {
        assertTrue("rule at ${it.y}", it.y in 0f..270f)
      }
    }
  }

  @Test
  fun `every shape kind is reachable`() {
    val kinds = (0 until 500)
      .flatMap { plan(it, 480, 270).shapes }
      .map { it::class.simpleName }
      .toSet()
    assertEquals(setOf("Disc", "Band", "Wedge"), kinds)
  }

  @Test
  fun `rule width scales with canvas width and never goes hairline-thin`() {
    assertEquals(1f, plan(4, 120, 120).ruleWidth, 1e-6f)
    assertEquals(1200f / 380f, plan(4, 1200, 630).ruleWidth, 1e-6f)
  }

  @Test
  fun `a slug seeded plan is reproducible from the slug alone`() {
    val slug = "on-agents-that-actually-ship"
    val a = CoverArtPlanner.plan(MulberryRng(coverSeedOf(slug)), coverSeedOf(slug), 1200, 630)
    val b = CoverArtPlanner.plan(MulberryRng(coverSeedOf(slug)), coverSeedOf(slug), 1200, 630)
    assertEquals(a, b)
  }
}
