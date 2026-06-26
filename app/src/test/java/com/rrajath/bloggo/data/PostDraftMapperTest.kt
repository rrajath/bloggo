package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.db.AutosaveEntity
import com.rrajath.bloggo.data.db.PostDraftEntity
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import org.junit.Test

class PostDraftMapperTest {

    @Test
    fun entity_toDomain_preservesAllFields() {
        val entity = PostDraftEntity(
            localId = "id-1",
            repoPath = "content/posts/my-post.md",
            blobSha = "abc123",
            syncState = SyncState.SYNCED.name,
            title = "My Post",
            slug = "my-post",
            draft = false,
            slugAutoDerive = false,
            rawFrontMatter = "date: 2026-06-21",
            postDate = "2026-06-21",
            body = "Body text.",
            updatedAt = 1000L,
        )

        val domain = entity.toDomain()

        assertThat(domain.localId).isEqualTo("id-1")
        assertThat(domain.repoPath).isEqualTo("content/posts/my-post.md")
        assertThat(domain.blobSha).isEqualTo("abc123")
        assertThat(domain.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(domain.title).isEqualTo("My Post")
        assertThat(domain.slug).isEqualTo("my-post")
        assertThat(domain.draft).isFalse()
        assertThat(domain.slugAutoDerive).isFalse()
        assertThat(domain.rawFrontMatter).isEqualTo("date: 2026-06-21")
        assertThat(domain.body).isEqualTo("Body text.")
        assertThat(domain.updatedAt).isEqualTo(1000L)
    }

    @Test
    fun domain_toEntity_preservesAllFields() {
        val domain = PostDraft(
            localId = "id-1",
            repoPath = "content/posts/my-post.md",
            blobSha = "abc123",
            syncState = SyncState.SYNCED_MODIFIED,
            title = "My Post",
            slug = "my-post",
            draft = false,
            slugAutoDerive = false,
            rawFrontMatter = "date: 2026-06-21",
            body = "Body text.",
            updatedAt = 1000L,
        )

        val entity = domain.toEntity()

        assertThat(entity.localId).isEqualTo("id-1")
        assertThat(entity.repoPath).isEqualTo("content/posts/my-post.md")
        assertThat(entity.blobSha).isEqualTo("abc123")
        assertThat(entity.syncState).isEqualTo(SyncState.SYNCED_MODIFIED.name)
        assertThat(entity.title).isEqualTo("My Post")
        assertThat(entity.slug).isEqualTo("my-post")
        assertThat(entity.draft).isFalse()
        assertThat(entity.slugAutoDerive).isFalse()
        assertThat(entity.rawFrontMatter).isEqualTo("date: 2026-06-21")
        assertThat(entity.body).isEqualTo("Body text.")
        assertThat(entity.updatedAt).isEqualTo(1000L)
    }

    @Test
    fun roundTrip_entityToDomainToEntity_preservesAllFields() {
        val original = PostDraftEntity(
            localId = "id-1",
            repoPath = null,
            blobSha = null,
            syncState = SyncState.LOCAL_ONLY.name,
            title = "Test",
            slug = "test",
            draft = true,
            slugAutoDerive = true,
            rawFrontMatter = "",
            postDate = null,
            body = "",
            updatedAt = 500L,
        )

        val restored = original.toDomain().toEntity()

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun domain_toAutosaveEntity_preservesEditorFields() {
        val domain = PostDraft(
            localId = "id-1",
            title = "Autosave Test",
            slug = "autosave-test",
            draft = true,
            slugAutoDerive = true,
            rawFrontMatter = "date: 2026-06-21",
            body = "In progress.",
            updatedAt = 2000L,
        )

        val autosave = domain.toAutosaveEntity()

        assertThat(autosave.localId).isEqualTo("id-1")
        assertThat(autosave.title).isEqualTo("Autosave Test")
        assertThat(autosave.slug).isEqualTo("autosave-test")
        assertThat(autosave.draft).isTrue()
        assertThat(autosave.slugAutoDerive).isTrue()
        assertThat(autosave.rawFrontMatter).isEqualTo("date: 2026-06-21")
        assertThat(autosave.body).isEqualTo("In progress.")
        assertThat(autosave.updatedAt).isEqualTo(2000L)
    }

    @Test
    fun autosaveEntity_toDomain_setsCorrectFields() {
        val autosave = AutosaveEntity(
            localId = "id-1",
            title = "Recovered",
            slug = "recovered",
            draft = true,
            slugAutoDerive = false,
            rawFrontMatter = "date: 2026-06-21",
            body = "Recovered body.",
            updatedAt = 3000L,
        )

        val domain = autosave.toDomain("id-1")

        assertThat(domain.localId).isEqualTo("id-1")
        assertThat(domain.title).isEqualTo("Recovered")
        assertThat(domain.slug).isEqualTo("recovered")
        assertThat(domain.draft).isTrue()
        assertThat(domain.slugAutoDerive).isFalse()
        assertThat(domain.syncState).isEqualTo(SyncState.LOCAL_ONLY)
        assertThat(domain.repoPath).isNull()
        assertThat(domain.blobSha).isNull()
    }
}
