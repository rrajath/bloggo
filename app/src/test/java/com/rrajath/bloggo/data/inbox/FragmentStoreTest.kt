package com.rrajath.bloggo.data.inbox

import com.rrajath.bloggo.model.Fragment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory stand-in for the Room-generated DAO — same shape
 * `PostLibraryRepositoryTest`'s `FakePostCacheDao` uses, no Android runtime
 * needed to exercise [FragmentStore] itself. */
private class FakeFragmentDao : FragmentDao {
  val storage = mutableMapOf<String, FragmentEntity>()

  override suspend fun getAll(): List<FragmentEntity> = storage.values.toList()

  override suspend fun upsert(entity: FragmentEntity) {
    storage[entity.id] = entity
  }

  override suspend fun deleteById(id: String) {
    storage.remove(id)
  }
}

class FragmentStoreTest {
  private lateinit var dao: FakeFragmentDao
  private lateinit var store: FragmentStore

  @Before
  fun setUp() {
    dao = FakeFragmentDao()
    store = FragmentStore(dao)
  }

  @Test
  fun `save then loadAll round-trips a fragment`() = runTest {
    val fragment = Fragment(id = "captured-1", text = "A caught thought", capturedAtMillis = 1_700_000_000_000L)

    store.save(fragment)
    val loaded = store.loadAll()

    assertEquals(listOf(fragment), loaded)
  }

  @Test
  fun `save upserts rather than duplicating an existing id`() = runTest {
    val original = Fragment(id = "captured-1", text = "First draft of the thought", capturedAtMillis = 1_700_000_000_000L)
    val edited = original.copy(text = "Edited version of the same thought")

    store.save(original)
    store.save(edited)

    val loaded = store.loadAll()
    assertEquals(1, loaded.size)
    assertEquals("Edited version of the same thought", loaded.single().text)
  }

  @Test
  fun `delete removes exactly the given id and nothing else`() = runTest {
    val keep = Fragment(id = "captured-1", text = "Keep this one", capturedAtMillis = 1_700_000_000_000L)
    val remove = Fragment(id = "captured-2", text = "Remove this one", capturedAtMillis = 1_700_000_000_000L)
    store.save(keep)
    store.save(remove)

    store.delete(remove.id)

    val loaded = store.loadAll()
    assertEquals(listOf(keep), loaded)
  }

  @Test
  fun `loadAll on an empty store returns an empty list, never a crash`() = runTest {
    assertTrue(store.loadAll().isEmpty())
  }
}
