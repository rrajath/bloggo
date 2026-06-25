package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrontMatterValidateTest {

    @Test
    fun validate_validYamlNoOwnedKeys_noWarnings() {
        val raw = "date: 2026-06-21\ntags: [a, b]"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).isEmpty()
    }

    @Test
    fun validate_ownedKeyTitle_returnsWarning() {
        val raw = "title: Fake\ndate: 2026-06-21"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0].message).contains("title")
    }

    @Test
    fun validate_ownedKeySlug_returnsWarning() {
        val raw = "slug: fake-slug\ndate: 2026-06-21"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0].message).contains("slug")
    }

    @Test
    fun validate_ownedKeyDraft_returnsWarning() {
        val raw = "draft: false\ndate: 2026-06-21"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0].message).contains("draft")
    }

    @Test
    fun validate_allOwnedKeys_returnsThreeWarnings() {
        val raw = "title: T\nslug: s\ndraft: true\ndate: 2026-06-21"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).hasSize(3)
    }

    @Test
    fun validate_malformedYaml_returnsWarning() {
        val raw = "title: [unclosed"
        val warnings = FrontMatter.validate(raw)

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0].message).contains("Malformed")
    }

    @Test
    fun validate_emptyString_noWarnings() {
        val warnings = FrontMatter.validate("")

        assertThat(warnings).isEmpty()
    }

    @Test
    fun validate_blankString_noWarnings() {
        val warnings = FrontMatter.validate("   \n  \n")

        assertThat(warnings).isEmpty()
    }
}
