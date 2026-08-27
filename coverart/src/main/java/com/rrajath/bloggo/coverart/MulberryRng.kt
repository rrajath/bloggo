package com.rrajath.bloggo.coverart

/**
 * The mulberry32 generator used by the web prototype's `rng()`, ported so that a
 * given seed produces the identical sequence on both platforms.
 *
 * The JavaScript original relies on `Math.imul` and `>>>`, which are 32 bit
 * operations. Kotlin's [Int] arithmetic already wraps at 32 bits, so the only
 * care needed is the initial multiply, which is done in [Long] before being
 * truncated the way `>>> 0` truncates in JavaScript.
 */
class MulberryRng(seed: Int) {

  private var state: Int = seedToState(seed)

  /** Next value in `[0, 1)`. */
  fun next(): Double {
    state += 0x6D2B79F5
    var t = (state xor (state ushr 15)) * (1 or state)
    t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
    return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
  }

  /** Next value in `[min, max)`. */
  fun nextBetween(min: Double, max: Double): Double = min + next() * (max - min)

  /** Next value in `0 until bound`, matching `Math.floor(r() * bound)`. */
  fun nextInt(bound: Int): Int = (next() * bound).toInt()

  private companion object {
    /**
     * `seed * 2654435761 >>> 0`, reproduced including its precision loss.
     *
     * The product exceeds 2^53 for any seed above roughly two million, so the
     * JavaScript original is computing it in a double that has already been
     * rounded before `>>> 0` truncates. Doing this multiply exactly in [Long]
     * would be more correct in the abstract and would give a *different*
     * generator, so every cover already published from the web prototype would
     * come out differently on Android. Slug derived seeds are always large, so
     * this is the common path, not an edge case.
     *
     * Double multiplication and `fmod` are both exactly specified by IEEE 754,
     * so this reproduces the JavaScript result bit for bit on any platform.
     */
    fun seedToState(seed: Int): Int {
      val product = seed.toDouble() * 2654435761.0
      var truncated = kotlin.math.truncate(product) % 4294967296.0
      if (truncated < 0) truncated += 4294967296.0
      return truncated.toLong().toInt()
    }
  }
}

/**
 * FNV-1a, so a post slug maps to the same seed on Android, on the web, and in any
 * build script that pre-renders covers. [String.hashCode] would have been simpler
 * but its value is not stable across languages.
 *
 * @param variant bumped by the Shuffle control to walk to a different cover for
 *   the same post while staying reproducible.
 */
fun coverSeedOf(slug: String, variant: Int = 0): Int {
  var hash = -0x7EE3623B // 2166136261 as a signed Int
  val source = if (variant == 0) slug else "$slug#$variant"
  for (char in source) {
    hash = hash xor char.code
    hash *= 16777619
  }
  return hash and 0x7FFFFFFF
}
