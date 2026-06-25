package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrontMatterRoundTripTest {

    @Test
    fun roundTrip_preservesTitleSlugDraft() {
        val original = PostDraft(
            localId = "1",
            title = "Round Trip Post",
            slug = "round-trip-post",
            draft = false,
            rawFrontMatter = "date: 2026-06-21\ntags: [test, round-trip]",
            body = "This is the body.\n\n## Section\n\nMore text.",
        )

        val assembled = FrontMatter.assemble(original)
        assertThat(assembled.warnings).isEmpty()

        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.title).isEqualTo("Round Trip Post")
        assertThat(parsed.slug).isEqualTo("round-trip-post")
        assertThat(parsed.draft).isFalse()
    }

    @Test
    fun roundTrip_preservesBody() {
        val original = PostDraft(
            localId = "1",
            title = "Body Test",
            slug = "body-test",
            draft = true,
            body = "First paragraph.\n\nSecond paragraph.\n\n## Heading\n\n- List item 1\n- List item 2",
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.body).contains("First paragraph.")
        assertThat(parsed.body).contains("Second paragraph.")
        assertThat(parsed.body).contains("## Heading")
        assertThat(parsed.body).contains("- List item 1")
    }

    @Test
    fun roundTrip_preservesRawFrontMatterKeys() {
        val original = PostDraft(
            localId = "1",
            title = "FM Test",
            slug = "fm-test",
            draft = true,
            rawFrontMatter = "date: 2026-06-21\ntags: [a, b, c]\nsummary: \"My summary\"",
            body = "Body.",
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.rawFrontMatter).contains("date:")
        assertThat(parsed.rawFrontMatter).contains("tags:")
        assertThat(parsed.rawFrontMatter).contains("summary:")
    }

    @Test
    fun roundTrip_draftTrueStaysTrue() {
        val original = PostDraft(
            localId = "1",
            title = "Draft",
            slug = "draft",
            draft = true,
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.draft).isTrue()
    }

    @Test
    fun roundTrip_draftFalseStaysFalse() {
        val original = PostDraft(
            localId = "1",
            title = "Published",
            slug = "published",
            draft = false,
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.draft).isFalse()
    }

    @Test
    fun roundTrip_emptyRawFrontMatter() {
        val original = PostDraft(
            localId = "1",
            title = "No FM",
            slug = "no-fm",
            draft = true,
            rawFrontMatter = "",
            body = "Body only.",
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)

        assertThat(parsed.title).isEqualTo("No FM")
        assertThat(parsed.rawFrontMatter).isEmpty()
    }

    @Test
    fun roundTrip_toPostDraft_preservesAllFields() {
        val original = PostDraft(
            localId = "original-id",
            title = "Full Cycle",
            slug = "full-cycle",
            draft = false,
            rawFrontMatter = "date: 2026-06-21\ntags: [test]",
            body = "Body text.",
        )

        val assembled = FrontMatter.assemble(original)
        val parsed = FrontMatter.parse(assembled.content)
        val restored = parsed.toPostDraft(
            localId = "restored-id",
            syncState = SyncState.SYNCED,
        )

        assertThat(restored.title).isEqualTo("Full Cycle")
        assertThat(restored.slug).isEqualTo("full-cycle")
        assertThat(restored.draft).isFalse()
        assertThat(restored.slugAutoDerive).isFalse()
        assertThat(restored.body).contains("Body text.")
        assertThat(restored.syncState).isEqualTo(SyncState.SYNCED)
    }
}
