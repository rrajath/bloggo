package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlugReconciliationTest {

    // ── onTitleCommitted ─────────────────────────────────────────────────────

    @Test
    fun onTitleCommitted_localOnlyWithAutoDerive_derivesSlug() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.LOCAL_ONLY,
            slugAutoDerive = true,
        )
        val updated = post.onTitleCommitted("My New Post")
        assertThat(updated.title).isEqualTo("My New Post")
        assertThat(updated.slug).isEqualTo("my-new-post")
    }

    @Test
    fun onTitleCommitted_synced_doesNotDeriveSlug() {
        val post = PostDraft(
            localId = "1",
            slug = "frozen-slug",
            syncState = SyncState.SYNCED,
            slugAutoDerive = true,
        )
        val updated = post.onTitleCommitted("New Title")
        assertThat(updated.title).isEqualTo("New Title")
        assertThat(updated.slug).isEqualTo("frozen-slug")
    }

    @Test
    fun onTitleCommitted_syncedModified_doesNotDeriveSlug() {
        val post = PostDraft(
            localId = "1",
            slug = "frozen-slug",
            syncState = SyncState.SYNCED_MODIFIED,
            slugAutoDerive = true,
        )
        val updated = post.onTitleCommitted("New Title")
        assertThat(updated.slug).isEqualTo("frozen-slug")
    }

    @Test
    fun onTitleCommitted_autoDeriveFalse_doesNotDeriveSlug() {
        val post = PostDraft(
            localId = "1",
            slug = "manual-slug",
            syncState = SyncState.LOCAL_ONLY,
            slugAutoDerive = false,
        )
        val updated = post.onTitleCommitted("New Title")
        assertThat(updated.title).isEqualTo("New Title")
        assertThat(updated.slug).isEqualTo("manual-slug")
    }

    @Test
    fun onTitleCommitted_updatesTimestamp() {
        val post = PostDraft(
            localId = "1",
            syncState = SyncState.LOCAL_ONLY,
            slugAutoDerive = true,
            updatedAt = 1000L,
        )
        val updated = post.onTitleCommitted("Title")
        assertThat(updated.updatedAt).isGreaterThan(1000L)
    }

    // ── onSlugManuallyEdited ─────────────────────────────────────────────────

    @Test
    fun onSlugManuallyEdited_setsSlug() {
        val post = PostDraft(localId = "1", slugAutoDerive = true)
        val updated = post.onSlugManuallyEdited("custom-slug")
        assertThat(updated.slug).isEqualTo("custom-slug")
    }

    @Test
    fun onSlugManuallyEdited_freezesAutoDerive() {
        val post = PostDraft(
            localId = "1",
            slugAutoDerive = true,
            syncState = SyncState.LOCAL_ONLY,
        )
        val updated = post.onSlugManuallyEdited("custom-slug")
        assertThat(updated.slugAutoDerive).isFalse()
    }

    @Test
    fun onSlugManuallyEdited_thenTitleCommit_doesNotDerive() {
        val post = PostDraft(
            localId = "1",
            slugAutoDerive = true,
            syncState = SyncState.LOCAL_ONLY,
        )
        val afterSlugEdit = post.onSlugManuallyEdited("custom-slug")
        val afterTitleCommit = afterSlugEdit.onTitleCommitted("New Title")
        assertThat(afterTitleCommit.slug).isEqualTo("custom-slug")
    }

    @Test
    fun onSlugManuallyEdited_updatesTimestamp() {
        val post = PostDraft(localId = "1", slugAutoDerive = true, updatedAt = 1000L)
        val updated = post.onSlugManuallyEdited("custom-slug")
        assertThat(updated.updatedAt).isGreaterThan(1000L)
    }

    // ── Full lifecycle ───────────────────────────────────────────────────────

    @Test
    fun lifecycle_newPostDerivesSlugThenFreezesOnPublish() {
        var post = PostDraft(localId = "1", syncState = SyncState.LOCAL_ONLY, slugAutoDerive = true)

        post = post.onTitleCommitted("My First Post")
        assertThat(post.slug).isEqualTo("my-first-post")

        post = post.withSyncedState()
        assertThat(post.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(post.slugAutoDerive).isFalse()

        post = post.onTitleCommitted("My Renamed Post")
        assertThat(post.slug).isEqualTo("my-first-post")
    }
}
