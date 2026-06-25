package com.rrajath.bloggo.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutosaveDaoTest {

    private lateinit var database: BloggoDatabase
    private lateinit var dao: AutosaveDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, BloggoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.autosaveDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun sampleAutosave(id: String = "post-1", title: String = "Draft Title") = AutosaveEntity(
        localId = id,
        title = title,
        slug = "draft-title",
        draft = true,
        slugAutoDerive = true,
        rawFrontMatter = "date: 2026-06-21",
        body = "In-progress body.",
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun save_andGet_returnsEntity() = runTest {
        val entity = sampleAutosave()
        dao.save(entity)

        val retrieved = dao.get("post-1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.title).isEqualTo("Draft Title")
        assertThat(retrieved.body).isEqualTo("In-progress body.")
    }

    @Test
    fun save_updatesExistingAutosave() = runTest {
        dao.save(sampleAutosave(title = "Original"))
        dao.save(sampleAutosave(title = "Updated"))

        val retrieved = dao.get("post-1")
        assertThat(retrieved!!.title).isEqualTo("Updated")
    }

    @Test
    fun delete_removesAutosave() = runTest {
        dao.save(sampleAutosave())
        dao.delete("post-1")

        assertThat(dao.get("post-1")).isNull()
    }

    @Test
    fun get_nonExistent_returnsNull() = runTest {
        assertThat(dao.get("nonexistent")).isNull()
    }
}
