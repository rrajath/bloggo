package com.rrajath.bloggo.coverart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The golden values are the output of the web prototype's own `rng()`, captured
 * from Node. If this file goes red, Android and the web have drifted and every
 * previously generated cover would render differently on one of them.
 */
class MulberryRngTest {

  private val tolerance = 1e-15

  @Test
  fun `matches the javascript sequence for seed 4`() {
    val expected = doubleArrayOf(
      0.77434869250282645,
      0.75915788277052343,
      0.99753003800287843,
      0.20606280444189906,
      0.24214229639619589,
      0.88481202512048185,
      0.29009653651155531,
      0.53719014953821898,
      0.12737242528237402,
      0.74853472621180117,
      0.88160833250731230,
      0.49839596473611891,
    )
    val rng = MulberryRng(4)
    expected.forEachIndexed { index, want ->
      assertEquals("draw $index", want, rng.next(), tolerance)
    }
  }

  @Test
  fun `matches the javascript sequence at the edges of the seed range`() {
    assertEquals(0.54645607573911548, MulberryRng(1).next(), tolerance)
    assertEquals(0.90928475488908589, MulberryRng(Int.MAX_VALUE).next(), tolerance)
    // Seed 0 collapses the initial multiply to zero; it must still advance.
    val zero = MulberryRng(0)
    assertEquals(0.26642920868471265, zero.next(), tolerance)
    assertEquals(0.00032974570058286, zero.next(), tolerance)
  }

  /**
   * The regression that motivated [MulberryRng.seedToState]. Above about two
   * million the JavaScript seed multiply overflows the double mantissa, and an
   * exact Long multiply produces a different generator. Slug derived seeds are
   * always in this range, so these are the values that actually matter.
   */
  @Test
  fun `matches the javascript sequence for large seeds where the multiply loses precision`() {
    val rngMax = MulberryRng(2147483647)
    assertEquals(0.90928475488908589, rngMax.next(), tolerance)
    assertEquals(0.24610298452898860, rngMax.next(), tolerance)
    assertEquals(0.42229271191172302, rngMax.next(), tolerance)

    val rngBig = MulberryRng(1234567890)
    assertEquals(0.31185688381083310, rngBig.next(), tolerance)
    assertEquals(0.31463723885826766, rngBig.next(), tolerance)
    assertEquals(0.11300237057730556, rngBig.next(), tolerance)

    // 2^24, just past where the product stops being exactly representable.
    assertEquals(0.24418238759972155, MulberryRng(16777216).next(), tolerance)
    // 2^21, still exact, so both the naive and the faithful port agree here.
    assertEquals(0.02192378905601799, MulberryRng(2097152).next(), tolerance)
  }

  @Test
  fun `stays inside the unit interval over a long run`() {
    val rng = MulberryRng(20260817)
    repeat(200_000) {
      val value = rng.next()
      assertTrue("out of range: $value", value >= 0.0 && value < 1.0)
    }
  }

  @Test
  fun `mean of a long run is close to one half`() {
    val rng = MulberryRng(99)
    var sum = 0.0
    val n = 200_000
    repeat(n) { sum += rng.next() }
    assertEquals(0.5, sum / n, 0.005)
  }

  @Test
  fun `nextInt covers every bucket and stays in range`() {
    val rng = MulberryRng(7)
    val counts = IntArray(3)
    repeat(30_000) { counts[rng.nextInt(3)]++ }
    counts.forEach { assertTrue("bucket never hit", it > 9000) }
  }

  @Test
  fun `seeds derived from slugs are stable and distinct`() {
    assertEquals(coverSeedOf("on-agents-that-actually-ship"), coverSeedOf("on-agents-that-actually-ship"))
    assertNotEquals(coverSeedOf("a-small-note-on-taste"), coverSeedOf("the-cost-of-a-good-abstraction"))
    // Shuffle walks to a new cover without losing reproducibility.
    assertNotEquals(coverSeedOf("post", 0), coverSeedOf("post", 1))
    assertEquals(coverSeedOf("post", 3), coverSeedOf("post", 3))
  }

  @Test
  fun `slug seeds are never negative so they can be persisted as they are`() {
    listOf("", "a", "post", "ünïcödé-slug", "a".repeat(400)).forEach {
      assertTrue("negative seed for '$it'", coverSeedOf(it) >= 0)
    }
  }
}
