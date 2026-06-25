package com.rrajath.bloggo.ui.home

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import org.junit.Test

class DateFormatterTest {

    @Test
    fun formatPostDate_isoTimestamp() {
        val fm = "date: 2026-06-21T08:14:00+00:00"
        assertThat(formatPostDate(fm)).isEqualTo("Jun 21, 2026")
    }

    @Test
    fun formatPostDate_isoDateOnly() {
        val fm = "date: 2026-06-21"
        assertThat(formatPostDate(fm)).isEqualTo("Jun 21, 2026")
    }

    @Test
    fun formatPostDate_quotedDate() {
        val fm = "date: \"2026-06-21\""
        assertThat(formatPostDate(fm)).isEqualTo("Jun 21, 2026")
    }

    @Test
    fun formatPostDate_emptyFrontMatter_returnsEmpty() {
        assertThat(formatPostDate("")).isEmpty()
    }

    @Test
    fun formatPostDate_noDateField_returnsEmpty() {
        val fm = "tags: [a, b]"
        assertThat(formatPostDate(fm)).isEmpty()
    }

    @Test
    fun formatPostDate_invalidDate_returnsRawValue() {
        val fm = "date: not-a-date"
        assertThat(formatPostDate(fm)).isEqualTo("not-a-date")
    }

    @Test
    fun buildMetaText_localOnly_includesNotPushed() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.LOCAL_ONLY,
            rawFrontMatter = "date: 2026-06-21T08:14:00+00:00",
        )
        val meta = buildMetaText(post)
        assertThat(meta).contains("Jun 21, 2026")
        assertThat(meta).contains("not pushed")
    }

    @Test
    fun buildMetaText_synced_doesNotIncludeNotPushed() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.SYNCED,
            rawFrontMatter = "date: 2026-06-21T08:14:00+00:00",
        )
        val meta = buildMetaText(post)
        assertThat(meta).contains("Jun 21, 2026")
        assertThat(meta).doesNotContain("not pushed")
    }

    @Test
    fun buildMetaText_syncedModified_doesNotIncludeNotPushed() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.SYNCED_MODIFIED,
            rawFrontMatter = "date: 2026-06-21T08:14:00+00:00",
        )
        val meta = buildMetaText(post)
        assertThat(meta).doesNotContain("not pushed")
    }

    @Test
    fun buildMetaText_emptyDate_returnsEmptyWithNotPushed() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.LOCAL_ONLY,
            rawFrontMatter = "",
        )
        val meta = buildMetaText(post)
        assertThat(meta).isEqualTo("  ·  not pushed")
    }
}
