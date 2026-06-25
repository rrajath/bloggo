package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeSectionTest {

    @Test
    fun draftPost_sectionIsDraft() {
        val post = PostDraft(localId = "1", draft = true)
        assertThat(post.section()).isEqualTo(HomeSection.DRAFT)
    }

    @Test
    fun publishedPost_sectionIsPublished() {
        val post = PostDraft(localId = "1", draft = false)
        assertThat(post.section()).isEqualTo(HomeSection.PUBLISHED)
    }

    @Test
    fun draftAndSynced_sectionIsStillDraft() {
        val post = PostDraft(
            localId = "1",
            draft = true,
            syncState = SyncState.SYNCED,
        )
        assertThat(post.section()).isEqualTo(HomeSection.DRAFT)
    }

    @Test
    fun publishedAndLocalOnly_sectionIsStillPublished() {
        val post = PostDraft(
            localId = "1",
            draft = false,
            syncState = SyncState.LOCAL_ONLY,
        )
        assertThat(post.section()).isEqualTo(HomeSection.PUBLISHED)
    }
}
