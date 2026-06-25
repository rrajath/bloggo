package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrontMatterAssembleTest {

    @Test
    fun assemble_basicStructuredFields() {
        val draft = PostDraft(
            localId = "1",
            title = "My Post",
            slug = "my-post",
            draft = false,
            body = "Hello world.",
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.warnings).isEmpty()
        assertThat(result.content).contains("---")
        assertThat(result.content).contains("title: My Post")
        assertThat(result.content).contains("slug: my-post")
        assertThat(result.content).contains("draft: false")
        assertThat(result.content).contains("Hello world.")
    }

    @Test
    fun assemble_withRawFrontMatter_mergesKeys() {
        val draft = PostDraft(
            localId = "1",
            title = "My Post",
            slug = "my-post",
            draft = true,
            rawFrontMatter = "date: 2026-06-21\ntags: [nginx, devops]\nsummary: \"Test\"",
            body = "Body.",
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.warnings).isEmpty()
        assertThat(result.content).contains("title:")
        assertThat(result.content).contains("slug:")
        assertThat(result.content).contains("draft: true")
        assertThat(result.content).contains("date:")
        assertThat(result.content).contains("tags:")
        assertThat(result.content).contains("summary:")
        assertThat(result.content).contains("Body.")
    }

    @Test
    fun assemble_ownedKeyInRaw_stripsAndWarns() {
        val draft = PostDraft(
            localId = "1",
            title = "Structured Title",
            slug = "structured-slug",
            draft = false,
            rawFrontMatter = "title: Raw Title\ndate: 2026-06-21",
            body = "Body.",
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.warnings).hasSize(1)
        assertThat(result.warnings[0].message).contains("title")
        assertThat(result.content).contains("Structured Title")
        assertThat(result.content).doesNotContain("Raw Title")
    }

    @Test
    fun assemble_multipleOwnedKeysInRaw_allStrippedAndWarned() {
        val draft = PostDraft(
            localId = "1",
            title = "Real Title",
            slug = "real-slug",
            draft = true,
            rawFrontMatter = "title: Fake\ndraft: false\nslug: fake-slug\ndate: 2026-01-01",
            body = "",
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.warnings).hasSize(3)
        assertThat(result.content).contains("Real Title")
        assertThat(result.content).contains("real-slug")
        assertThat(result.content).contains("draft: true")
        assertThat(result.content).contains("date:")
    }

    @Test
    fun assemble_emptyTitle_usesUntitled() {
        val draft = PostDraft(
            localId = "1",
            title = "",
            slug = "some-slug",
            draft = true,
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.content).contains("Untitled")
    }

    @Test
    fun assemble_emptySlug_derivesFromTitle() {
        val draft = PostDraft(
            localId = "1",
            title = "Hello World",
            slug = "",
            draft = true,
        )
        val result = FrontMatter.assemble(draft)

        assertThat(result.content).contains("hello-world")
    }

    @Test
    fun assemble_emptyBody_producesFrontMatterOnly() {
        val draft = PostDraft(
            localId = "1",
            title = "Test",
            slug = "test",
            draft = true,
            body = "",
        )
        val result = FrontMatter.assemble(draft)

        val lines = result.content.trim().lines()
        assertThat(lines.last()).isEqualTo("---")
    }

    @Test
    fun assemble_bodyWithLeadingNewlines_trimmed() {
        val draft = PostDraft(
            localId = "1",
            title = "Test",
            slug = "test",
            draft = true,
            body = "\n\n\nHello world.",
        )
        val result = FrontMatter.assemble(draft)

        val bodyPart = result.content.substringAfterLast("---\n")
        assertThat(bodyPart).startsWith("\nHello world.")
        assertThat(bodyPart).doesNotContain("\n\n\nHello")
    }

    @Test
    fun assemble_startsWithFrontMatterFence() {
        val draft = PostDraft(localId = "1", title = "T", slug = "t", draft = true)
        val result = FrontMatter.assemble(draft)

        assertThat(result.content.startsWith("---\n")).isTrue()
    }

    @Test
    fun assemble_hasClosingFrontMatterFence() {
        val draft = PostDraft(localId = "1", title = "T", slug = "t", draft = true, body = "Body")
        val result = FrontMatter.assemble(draft)

        assertThat(result.content).contains("---\n\nBody")
    }
}
