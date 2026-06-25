package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostDraftFactoryTest {

    @Test
    fun seedFrontMatter_replacesDateToken() {
        val template = "date: {date}\ntags: []"
        val seeded = seedFrontMatter(template)

        assertThat(seeded).doesNotContain("{date}")
        assertThat(seeded).contains("date: ")
        assertThat(seeded).contains("tags: []")
    }

    @Test
    fun seedFrontMatter_noDateToken_unchanged() {
        val template = "tags: []\nsummary: \"Test\""
        val seeded = seedFrontMatter(template)

        assertThat(seeded).isEqualTo(template)
    }

    @Test
    fun seedFrontMatter_producesIsoTimestamp() {
        val seeded = seedFrontMatter("date: {date}")
        val dateLine = seeded.lines().first()

        assertThat(dateLine).matches("date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
    }

    @Test
    fun currentIsoTimestamp_matchesIsoFormat() {
        val ts = currentIsoTimestamp()

        assertThat(ts).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
    }

    @Test
    fun toPostDraft_fromParseResult_preservesAllFields() {
        val parsed = ParseResult(
            title = "Test Post",
            slug = "test-post",
            draft = false,
            rawFrontMatter = "date: 2026-06-21\ntags: [a]",
            body = "Body text.",
            warnings = emptyList(),
        )

        val draft = parsed.toPostDraft(localId = "id-1", syncState = SyncState.SYNCED)

        assertThat(draft.localId).isEqualTo("id-1")
        assertThat(draft.title).isEqualTo("Test Post")
        assertThat(draft.slug).isEqualTo("test-post")
        assertThat(draft.draft).isFalse()
        assertThat(draft.slugAutoDerive).isFalse()
        assertThat(draft.rawFrontMatter).isEqualTo("date: 2026-06-21\ntags: [a]")
        assertThat(draft.body).isEqualTo("Body text.")
        assertThat(draft.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun toPostDraft_defaultSyncStateIsSynced() {
        val parsed = ParseResult(
            title = "T",
            slug = "t",
            draft = true,
            rawFrontMatter = "",
            body = "",
            warnings = emptyList(),
        )

        val draft = parsed.toPostDraft(localId = "1")

        assertThat(draft.syncState).isEqualTo(SyncState.SYNCED)
    }

    @Test
    fun toPostDraft_slugAutoDeriveIsAlwaysFalse() {
        val parsed = ParseResult(
            title = "T",
            slug = "t",
            draft = true,
            rawFrontMatter = "",
            body = "",
            warnings = emptyList(),
        )

        val draft = parsed.toPostDraft(localId = "1")

        assertThat(draft.slugAutoDerive).isFalse()
    }

    @Test
    fun toPostDraft_nullTitle_becomesEmptyString() {
        val parsed = ParseResult(
            title = null,
            slug = null,
            draft = true,
            rawFrontMatter = "",
            body = "",
            warnings = emptyList(),
        )

        val draft = parsed.toPostDraft(localId = "1")

        assertThat(draft.title).isEmpty()
        assertThat(draft.slug).isEmpty()
    }
}
