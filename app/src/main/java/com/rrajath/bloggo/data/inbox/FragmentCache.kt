package com.rrajath.bloggo.data.inbox

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Index over every captured-but-not-yet-promoted [com.rrajath.bloggo.model.Fragment] —
 * the Inbox's counterpart to [com.rrajath.bloggo.data.library.LocalPostEntity]. A
 * fragment is plain captured text with no frontmatter and no eventual repo file of
 * its own (unlike a local post/page, which mirrors the file shape it will one day be
 * committed as), so a Room row alone is sufficient storage — there is no markdown
 * file on disk to keep in step with this table the way [com.rrajath.bloggo.data.library.LocalPostStore]
 * has to.
 *
 * Deliberately has no bulk-delete/replaceAll/deleteExcept method — not just by
 * convention, but because [FragmentDao] simply has no such method to call. A row
 * leaves this table only through [FragmentDao.deleteById], and every call site of
 * that in this codebase is a direct result of the writer promoting that exact
 * fragment to a post (see `BloggoApp.kt`'s "Promote to Post" handler). Nothing here
 * runs a reconciliation pass, and startup's own [FragmentDao.getAll] never deletes.
 */
@Entity(tableName = "fragment")
data class FragmentEntity(
  @PrimaryKey val id: String,
  val text: String,
  val capturedAtMillis: Long,
  val tag: String?,
)

@Dao
interface FragmentDao {
  @Query("SELECT * FROM fragment")
  suspend fun getAll(): List<FragmentEntity>

  /** Upserts one row — never a batch replace. A thought is captured one at a time,
   * on its own explicit Save, so there is never a "here is the true set" moment the
   * way a remote tree refresh has. */
  @Upsert
  suspend fun upsert(entity: FragmentEntity)

  /** The only way a row ever leaves this table. Every caller of this is a direct,
   * explicit user action — see this file's own doc comment. */
  @Query("DELETE FROM fragment WHERE id = :id")
  suspend fun deleteById(id: String)
}
