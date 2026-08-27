package com.rrajath.bloggo.data.library

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * The Pages tab's counterpart to [PostCacheEntity] — same
 * `path -> (blobSha, ...)` shape, narrower: a page has no draft flag and no
 * cover (docs/PROTOTYPE_NOTES.md, "Pages replaced Media"), so there is
 * nothing here to carry for either.
 */
@Entity(tableName = "page_cache")
data class PageCacheEntity(
  @PrimaryKey val path: String,
  val blobSha: String,
  val slug: String,
  val title: String,
  val date: String?,
  val markdown: String,
  val wordCount: Int,
)

@Dao
interface PageCacheDao {
  @Query("SELECT * FROM page_cache")
  suspend fun getAll(): List<PageCacheEntity>

  @Query("SELECT path FROM page_cache")
  suspend fun getAllPaths(): List<String>

  @Upsert
  suspend fun upsertAll(entries: List<PageCacheEntity>)

  @Query("DELETE FROM page_cache")
  suspend fun deleteAll()

  /** Replaces the cache with exactly [entries]: anything else cached is gone from the repo. */
  @Transaction
  suspend fun replaceAll(entries: List<PageCacheEntity>) {
    val keepPaths = entries.map { it.path }
    if (keepPaths.isEmpty()) {
      deleteAll()
    } else {
      // Chunked the same way PostCacheDao.replaceAll is, against the same
      // SQLITE_MAX_VARIABLE_NUMBER trap — a personal blog has a handful of
      // top-level pages, not hundreds, but there's no reason for this copy
      // to be the one that assumes that.
      val survivors = keepPaths.toSet()
      getAllPaths()
        .filterNot { it in survivors }
        .chunked(DELETE_CHUNK)
        .forEach { deletePaths(it) }
    }
    upsertAll(entries)
  }

  @Query("DELETE FROM page_cache WHERE path IN (:paths)")
  suspend fun deletePaths(paths: List<String>)

  companion object {
    const val DELETE_CHUNK = 900
  }
}
