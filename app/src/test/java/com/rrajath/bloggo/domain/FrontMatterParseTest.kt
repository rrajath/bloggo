package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrontMatterParseTest {

    @Test
    fun parse_basicFile_extractsAllFields() {
        val content = """
            ---
            title: "My Post"
            slug: "my-post"
            draft: false
            date: 2026-06-21T08:14:00+00:00
            tags: [nginx, devops]
            summary: "One domain, many services."
            ---

            Body text here.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.title).isEqualTo("My Post")
        assertThat(result.slug).isEqualTo("my-post")
        assertThat(result.draft).isFalse()
        assertThat(result.body).contains("Body text here.")
    }

    @Test
    fun parse_noFrontMatter_returnsBodyOnly() {
        val content = "Just some body text without front matter."

        val result = FrontMatter.parse(content)

        assertThat(result.title).isNull()
        assertThat(result.slug).isNull()
        assertThat(result.draft).isFalse()
        assertThat(result.rawFrontMatter).isEmpty()
        assertThat(result.body).isEqualTo("Just some body text without front matter.")
    }

    @Test
    fun parse_missingDraft_defaultsToFalse() {
        val content = """
            ---
            title: "No Draft Field"
            slug: "no-draft"
            ---

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isFalse()
    }

    @Test
    fun parse_draftTrue_boolean() {
        val content = "---\ntitle: T\nslug: t\ndraft: true\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isTrue()
    }

    @Test
    fun parse_draftFalse_boolean() {
        val content = "---\ntitle: T\nslug: t\ndraft: false\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isFalse()
    }

    @Test
    fun parse_draftAsString_true() {
        val content = "---\ntitle: T\nslug: t\ndraft: \"true\"\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isTrue()
    }

    @Test
    fun parse_draftAsString_false() {
        val content = "---\ntitle: T\nslug: t\ndraft: \"false\"\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isFalse()
    }

    @Test
    fun parse_rawFrontMatter_preservesNonOwnedKeys() {
        val content = """
            ---
            title: "T"
            slug: "t"
            draft: true
            date: 2026-06-21
            tags: [a, b]
            ---

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.rawFrontMatter).contains("date:")
        assertThat(result.rawFrontMatter).contains("tags:")
    }

    @Test
    fun parse_rawFrontMatter_excludesOwnedKeys() {
        val content = "---\ntitle: T\nslug: t\ndraft: true\ndate: 2026-06-21\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.rawFrontMatter).doesNotContain("title:")
        assertThat(result.rawFrontMatter).doesNotContain("slug:")
        assertThat(result.rawFrontMatter).doesNotContain("draft:")
    }

    @Test
    fun parse_emptyBody() {
        val content = "---\ntitle: T\nslug: t\ndraft: true\n---\n"

        val result = FrontMatter.parse(content)

        assertThat(result.body).isEmpty()
    }

    @Test
    fun parse_quotedTitle_removesQuotes() {
        val content = "---\ntitle: \"My Quoted Title\"\nslug: t\ndraft: true\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.title).isEqualTo("My Quoted Title")
    }

    @Test
    fun parse_titleWithColon() {
        val content = "---\ntitle: \"A: B\"\nslug: t\ndraft: true\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.title).isEqualTo("A: B")
    }

    @Test
    fun parse_onlyOwnedKeys_emptyRawFrontMatter() {
        val content = "---\ntitle: T\nslug: t\ndraft: true\n---\n\nBody."

        val result = FrontMatter.parse(content)

        assertThat(result.rawFrontMatter).isEmpty()
    }

    @Test
    fun parse_tomlFrontMatter_extractsAllFields() {
        val content = """
            +++
            title = "My TOML Post"
            slug = "my-toml-post"
            draft = false
            date = 2026-06-21
            tags = ["nginx", "devops"]
            summary = "One domain, many services."
            +++

            Body text here.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.title).isEqualTo("My TOML Post")
        assertThat(result.slug).isEqualTo("my-toml-post")
        assertThat(result.draft).isFalse()
        assertThat(result.body).contains("Body text here.")
    }

    @Test
    fun parse_tomlFrontMatter_missingDraft_defaultsToFalse() {
        val content = """
            +++
            title = "No Draft TOML"
            slug = "no-draft-toml"
            +++

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isFalse()
    }

    @Test
    fun parse_tomlFrontMatter_draftTrue() {
        val content = """
            +++
            title = "Draft Post"
            slug = "draft-post"
            draft = true
            +++

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.draft).isTrue()
    }

    @Test
    fun parse_tomlFrontMatter_preservesNonOwnedKeys() {
        val content = """
            +++
            title = "T"
            slug = "t"
            draft = true
            date = 2026-06-21
            tags = ["a", "b"]
            +++

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.rawFrontMatter).contains("date:")
        assertThat(result.rawFrontMatter).contains("tags:")
    }

    @Test
    fun parse_tomlFrontMatter_excludesOwnedKeys() {
        val content = """
            +++
            title = "T"
            slug = "t"
            draft = true
            date = 2026-06-21
            +++

            Body.
        """.trimIndent()

        val result = FrontMatter.parse(content)

        assertThat(result.rawFrontMatter).doesNotContain("title:")
        assertThat(result.rawFrontMatter).doesNotContain("slug:")
        assertThat(result.rawFrontMatter).doesNotContain("draft:")
    }
}
