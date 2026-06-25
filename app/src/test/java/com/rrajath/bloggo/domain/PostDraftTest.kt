package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostDraftTest {

    @Test
    fun isSynced_falseForLocalOnly() {
        val post = PostDraft(localId = "1", syncState = SyncState.LOCAL_ONLY)
        assertThat(post.isSynced).isFalse()
    }

    @Test
    fun isSynced_trueForSynced() {
        val post = PostDraft(localId = "1", syncState = SyncState.SYNCED)
        assertThat(post.isSynced).isTrue()
    }

    @Test
    fun isSynced_trueForSyncedModified() {
        val post = PostDraft(localId = "1", syncState = SyncState.SYNCED_MODIFIED)
        assertThat(post.isSynced).isTrue()
    }

    @Test
    fun targetFilename_isSlugMd() {
        val post = PostDraft(localId = "1", slug = "my-post")
        assertThat(post.targetFilename).isEqualTo("my-post.md")
    }

    @Test
    fun targetPath_combinesContentPathAndSlug() {
        val post = PostDraft(localId = "1", slug = "my-post")
        assertThat(post.targetPath("content/posts")).isEqualTo("content/posts/my-post.md")
    }

    @Test
    fun targetPath_handlesTrailingSlashInContentPath() {
        val post = PostDraft(localId = "1", slug = "my-post")
        assertThat(post.targetPath("content/posts/")).isEqualTo("content/posts//my-post.md")
    }

    @Test
    fun defaultValues_areCorrect() {
        val post = PostDraft(localId = "1")
        assertThat(post.repoPath).isNull()
        assertThat(post.blobSha).isNull()
        assertThat(post.syncState).isEqualTo(SyncState.LOCAL_ONLY)
        assertThat(post.title).isEmpty()
        assertThat(post.slug).isEmpty()
        assertThat(post.draft).isTrue()
        assertThat(post.slugAutoDerive).isTrue()
        assertThat(post.rawFrontMatter).isEmpty()
        assertThat(post.body).isEmpty()
    }

    @Test
    fun withSyncedState_setsSyncedAndFreezesSlug() {
        val post = PostDraft(localId = "1", slugAutoDerive = true, syncState = SyncState.LOCAL_ONLY)
        val synced = post.withSyncedState()
        assertThat(synced.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(synced.slugAutoDerive).isFalse()
    }

    @Test
    fun markAsModified_setsSyncedModified() {
        val post = PostDraft(localId = "1", syncState = SyncState.SYNCED)
        val modified = post.markAsModified()
        assertThat(modified.syncState).isEqualTo(SyncState.SYNCED_MODIFIED)
    }
}
