package com.rrajath.bloggo.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.domain.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostDaoTest {

    private lateinit var database: BloggoDatabase
    private lateinit var dao: PostDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BloggoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.postDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun sampleEntity(
        id: String = "post-1",
        title: String = "Test Post",
        slug: String = "test-post",
        draft: Boolean = true,
        syncState: SyncState = SyncState.LOCAL_ONLY,
    ) = PostDraftEntity(
        localId = id,
        repoPath = null,
        blobSha = null,
        syncState = syncState.name,
        title = title,
        slug = slug,
        draft = draft,
        slugAutoDerive = true,
        rawFrontMatter = "date: 2026-06-21",
        postDate = "2026-06-21",
        body = "Body text.",
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun upsert_andGetById_returnsEntity() = runTest {
        val entity = sampleEntity()
        dao.upsert(entity)

        val retrieved = dao.getById("post-1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.title).isEqualTo("Test Post")
        assertThat(retrieved.slug).isEqualTo("test-post")
    }

    @Test
    fun upsert_updatesExistingEntity() = runTest {
        dao.upsert(sampleEntity(title = "Original"))
        dao.upsert(sampleEntity(title = "Updated"))

        val retrieved = dao.getById("post-1")
        assertThat(retrieved!!.title).isEqualTo("Updated")
    }

    @Test
    fun deleteById_removesEntity() = runTest {
        dao.upsert(sampleEntity())
        dao.deleteById("post-1")

        val retrieved = dao.getById("post-1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun delete_entity_removesEntity() = runTest {
        val entity = sampleEntity()
        dao.upsert(entity)
        dao.delete(entity)

        assertThat(dao.getById("post-1")).isNull()
    }

    @Test
    fun count_returnsNumberOfPosts() = runTest {
        assertThat(dao.count()).isEqualTo(0)

        dao.upsert(sampleEntity(id = "1"))
        dao.upsert(sampleEntity(id = "2"))
        dao.upsert(sampleEntity(id = "3"))

        assertThat(dao.count()).isEqualTo(3)
    }

    @Test
    fun observeAll_returnsAllPosts() = runTest {
        dao.upsert(sampleEntity(id = "1", title = "First"))
        dao.upsert(sampleEntity(id = "2", title = "Second"))

        val all = dao.observeAll().first()
        assertThat(all).hasSize(2)
    }

    @Test
    fun observeById_returnsMatchingPost() = runTest {
        dao.upsert(sampleEntity(id = "1", title = "Match"))
        dao.upsert(sampleEntity(id = "2", title = "Other"))

        val match = dao.observeById("1").first()
        assertThat(match).isNotNull()
        assertThat(match!!.title).isEqualTo("Match")
    }

    @Test
    fun observeById_nonExistent_returnsNull() = runTest {
        dao.upsert(sampleEntity(id = "1"))

        val result = dao.observeById("nonexistent").first()
        assertThat(result).isNull()
    }

    @Test
    fun syncState_storedAndRetrievedCorrectly() = runTest {
        dao.upsert(sampleEntity(syncState = SyncState.SYNCED))
        val retrieved = dao.getById("post-1")
        assertThat(retrieved!!.syncState).isEqualTo(SyncState.SYNCED.name)
    }

    @Test
    fun syncState_modified_storedCorrectly() = runTest {
        dao.upsert(sampleEntity(syncState = SyncState.SYNCED_MODIFIED))
        val retrieved = dao.getById("post-1")
        assertThat(retrieved!!.syncState).isEqualTo(SyncState.SYNCED_MODIFIED.name)
    }
}
