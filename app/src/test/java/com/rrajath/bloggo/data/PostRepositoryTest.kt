package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.db.AutosaveDao
import com.rrajath.bloggo.data.db.AutosaveEntity
import com.rrajath.bloggo.data.db.PostDao
import com.rrajath.bloggo.data.db.PostDraftEntity
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PostRepositoryTest {

    private val postDao = mockk<PostDao>(relaxed = true)
    private val autosaveDao = mockk<AutosaveDao>(relaxed = true)
    private val repository = PostRepository(postDao, autosaveDao)

    private fun samplePost(
        id: String = "post-1",
        syncState: SyncState = SyncState.LOCAL_ONLY,
    ) = PostDraft(
        localId = id,
        syncState = syncState,
        title = "Test Post",
        slug = "test-post",
        draft = true,
        rawFrontMatter = "date: 2026-06-21",
        body = "Body.",
        updatedAt = 1000L,
    )

    private fun sampleEntity(
        id: String = "post-1",
        syncState: SyncState = SyncState.LOCAL_ONLY,
    ) = PostDraftEntity(
        localId = id,
        repoPath = null,
        blobSha = null,
        syncState = syncState.name,
        title = "Test Post",
        slug = "test-post",
        draft = true,
        slugAutoDerive = true,
        rawFrontMatter = "date: 2026-06-21",
        body = "Body.",
        updatedAt = 1000L,
    )

    @Test
    fun observeAllPosts_mapsEntitiesToDomain() = runTest {
        coEvery { postDao.observeAll() } returns flowOf(
            listOf(sampleEntity(id = "1"), sampleEntity(id = "2")),
        )

        val posts = repository.observeAllPosts().first()

        assertThat(posts).hasSize(2)
        assertThat(posts[0].localId).isEqualTo("1")
        assertThat(posts[0].title).isEqualTo("Test Post")
        assertThat(posts[1].localId).isEqualTo("2")
    }

    @Test
    fun observeAllPosts_emptyList() = runTest {
        coEvery { postDao.observeAll() } returns flowOf(emptyList())

        val posts = repository.observeAllPosts().first()

        assertThat(posts).isEmpty()
    }

    @Test
    fun observePost_mapsEntityToDomain() = runTest {
        coEvery { postDao.observeById("post-1") } returns flowOf(sampleEntity())

        val post = repository.observePost("post-1").first()

        assertThat(post).isNotNull()
        assertThat(post!!.title).isEqualTo("Test Post")
    }

    @Test
    fun observePost_nonExistent_returnsNull() = runTest {
        coEvery { postDao.observeById("nonexistent") } returns flowOf(null)

        val post = repository.observePost("nonexistent").first()

        assertThat(post).isNull()
    }

    @Test
    fun getPost_returnsDomainObject() = runTest {
        coEvery { postDao.getById("post-1") } returns sampleEntity()

        val post = repository.getPost("post-1")

        assertThat(post).isNotNull()
        assertThat(post!!.slug).isEqualTo("test-post")
    }

    @Test
    fun getPost_nonExistent_returnsNull() = runTest {
        coEvery { postDao.getById("nonexistent") } returns null

        val post = repository.getPost("nonexistent")

        assertThat(post).isNull()
    }

    @Test
    fun savePost_delegatesToDaoUpsert() = runTest {
        val post = samplePost()

        repository.savePost(post)

        coVerify { postDao.upsert(any()) }
    }

    @Test
    fun savePost_mapsDomainToEntity() = runTest {
        val post = samplePost(syncState = SyncState.SYNCED)

        repository.savePost(post)

        coVerify {
            postDao.upsert(match {
                it.localId == "post-1" &&
                    it.syncState == "SYNCED" &&
                    it.title == "Test Post"
            })
        }
    }

    @Test
    fun deletePost_delegatesToDaoDeleteById() = runTest {
        repository.deletePost("post-1")

        coVerify { postDao.deleteById("post-1") }
    }

    @Test
    fun count_delegatesToDao() = runTest {
        coEvery { postDao.count() } returns 5

        assertThat(repository.count()).isEqualTo(5)
    }

    @Test
    fun saveAutosave_delegatesToAutosaveDao() = runTest {
        val post = samplePost()

        repository.saveAutosave(post)

        coVerify { autosaveDao.save(any()) }
    }

    @Test
    fun saveAutosave_mapsDomainToAutosaveEntity() = runTest {
        val post = samplePost()

        repository.saveAutosave(post)

        coVerify {
            autosaveDao.save(match {
                it.localId == "post-1" &&
                    it.title == "Test Post" &&
                    it.body == "Body."
            })
        }
    }

    @Test
    fun getAutosave_returnsDomainObject() = runTest {
        val autosave = AutosaveEntity(
            localId = "post-1",
            title = "Recovered",
            slug = "recovered",
            draft = true,
            slugAutoDerive = false,
            rawFrontMatter = "",
            body = "Recovered body.",
            updatedAt = 2000L,
        )
        coEvery { autosaveDao.get("post-1") } returns autosave

        val post = repository.getAutosave("post-1")

        assertThat(post).isNotNull()
        assertThat(post!!.title).isEqualTo("Recovered")
        assertThat(post!!.body).isEqualTo("Recovered body.")
    }

    @Test
    fun getAutosave_nonExistent_returnsNull() = runTest {
        coEvery { autosaveDao.get("nonexistent") } returns null

        assertThat(repository.getAutosave("nonexistent")).isNull()
    }

    @Test
    fun deleteAutosave_delegatesToDao() = runTest {
        repository.deleteAutosave("post-1")

        coVerify { autosaveDao.delete("post-1") }
    }
}
