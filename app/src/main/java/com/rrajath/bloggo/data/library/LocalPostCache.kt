package com.rrajath.bloggo.data.library

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Index over the local-only markdown files [LocalPostStore] writes under
 * `context.filesDir` — a post or page that has never been pushed to the repo
 * (see [com.rrajath.bloggo.model.isPushed]), so there is no `blobSha`/`path`
 * to key off the way [PostCacheEntity]/[PageCacheEntity] do. [slug] is the
 * key instead: this app's own internal identifier, unique by construction
 * (`BloggoApp.kt`'s `newBlankPost`/`newBlankPage`/`newPostFromFragment`).
 *
 * Deliberately has no `replaceAll`/`deleteExcept` the way the remote caches
 * do: nothing here is ever "reconciled away" by a sync pass. A row leaves
 * this table only through [LocalPostDao.deleteBySlug], and every call site of
 * that is an explicit user delete — never a side effect of refresh, startup,
 * or publish. That's the whole point of this table existing.
 */
@Entity(tableName = "local_post")
data class LocalPostEntity(
  @PrimaryKey val slug: String,
  /** [com.rrajath.bloggo.model.DocKind] serialized as its `name`, not the
   * ordinal — a Room migration re-numbering it would otherwise silently
   * relabel every existing local page as a post, or vice versa. */
  val kind: String,
  val title: String,
  /** Absolute path to this post's markdown file under `context.filesDir` —
   * see [LocalPostStore]. The body lives on disk, not in this row, so this
   * table stays cheap to query even with a long draft cached in it. */
  val filePath: String,
  val wordCount: Int,
  val date: String?,
  val dateMillis: Long?,
  val updatedAt: Long?,
  val editedAgo: String?,
)

@Dao
interface LocalPostDao {
  @Query("SELECT * FROM local_post")
  suspend fun getAll(): List<LocalPostEntity>

  /** Upserts one row — never a batch replace. A local draft is saved one at a
   * time, on its own edit, so there is never a "here is the true set, drop
   * anything not in it" moment the way a remote tree refresh has. */
  @Upsert
  suspend fun upsert(entity: LocalPostEntity)

  /** The only way a row ever leaves this table. Every caller of this is an
   * explicit user delete action — see this file's own doc comment. */
  @Query("DELETE FROM local_post WHERE slug = :slug")
  suspend fun deleteBySlug(slug: String)
}
