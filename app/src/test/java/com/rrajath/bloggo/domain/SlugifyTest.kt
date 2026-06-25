package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlugifyTest {

    // ── Basic cases ──────────────────────────────────────────────────────────

    @Test
    fun simpleTitle_lowercasesAndDashesSpaces() {
        assertThat(slugify("Hello World")).isEqualTo("hello-world")
    }

    @Test
    fun singleWord_lowercases() {
        assertThat(slugify("Hello")).isEqualTo("hello")
    }

    @Test
    fun emptyString_returnsEmpty() {
        assertThat(slugify("")).isEmpty()
    }

    @Test
    fun blankString_returnsEmpty() {
        assertThat(slugify("   ")).isEmpty()
    }

    // ── Apostrophes and quotes ───────────────────────────────────────────────

    @Test
    fun apostrophe_isDropped() {
        assertThat(slugify("Don't Stop")).isEqualTo("dont-stop")
    }

    @Test
    fun unicodeApostrophe_isDropped() {
        assertThat(slugify("Don\u2019t Stop")).isEqualTo("dont-stop")
    }

    @Test
    fun doubleQuotes_areDropped() {
        assertThat(slugify("Say \"Hi\"")).isEqualTo("say-hi")
    }

    @Test
    fun singleQuotes_areDropped() {
        assertThat(slugify("Say 'Hi'")).isEqualTo("say-hi")
    }

    // ── Non-alphanumeric runs ────────────────────────────────────────────────

    @Test
    fun multipleSpaces_collapseToSingleDash() {
        assertThat(slugify("A  B")).isEqualTo("a-b")
    }

    @Test
    fun specialChars_becomeDashes() {
        assertThat(slugify("C++ Guide")).isEqualTo("c-guide")
    }

    @Test
    fun mixedPunctuation_collapseToSingleDash() {
        assertThat(slugify("Hello, World! How are you?")).isEqualTo("hello-world-how-are-you")
    }

    @Test
    fun unicodeNonAlnum_becomeDashes() {
        assertThat(slugify("Café Résumé")).isEqualTo("caf-r-sum")
    }

    // ── Leading / trailing dashes ────────────────────────────────────────────

    @Test
    fun leadingSpaces_trimmed() {
        assertThat(slugify("  Hello")).isEqualTo("hello")
    }

    @Test
    fun trailingSpaces_trimmed() {
        assertThat(slugify("Hello  ")).isEqualTo("hello")
    }

    @Test
    fun leadingTrailingSpecialChars_trimmed() {
        assertThat(slugify("---Hello---")).isEqualTo("hello")
    }

    @Test
    fun onlySpecialChars_returnsEmpty() {
        assertThat(slugify("!!! ??? ---")).isEmpty()
    }

    // ── Already-dashed strings ───────────────────────────────────────────────

    @Test
    fun existingDashes_preserved() {
        assertThat(slugify("my-post")).isEqualTo("my-post")
    }

    @Test
    fun multipleDashes_collapseToOne() {
        assertThat(slugify("a---b")).isEqualTo("a-b")
    }

    @Test
    fun dashAndSpace_collapseToOneDash() {
        assertThat(slugify("a - b")).isEqualTo("a-b")
    }

    // ── Numbers ──────────────────────────────────────────────────────────────

    @Test
    fun numbers_preserved() {
        assertThat(slugify("Post 42")).isEqualTo("post-42")
    }

    @Test
    fun onlyNumbers_preserved() {
        assertThat(slugify("2026")).isEqualTo("2026")
    }

    @Test
    fun mixedAlphanumeric_preserved() {
        assertThat(slugify("Hello2World")).isEqualTo("hello2world")
    }
}
