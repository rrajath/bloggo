package com.rrajath.bloggo.data.review

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/**
 * One readability finding the writer has chosen to ignore on the review screen,
 * scoped to the post it was found in. [ignoreKey] is the opaque
 * `category + normalised text` string built by
 * [com.rrajath.bloggo.ui.review.readabilityIgnoreKey]: identity is the flagged
 * text and its check category, so an unrelated edit elsewhere in the draft does
 * not disturb it.
 *
 * A row is written when the writer confirms the ignore popup and removed only in
 * bulk, per slug, when they tap recompute. There is no single-row delete and no
 * reconciliation pass: leaving the screen or killing the app must not bring an
 * ignored highlight back.
 */
@Entity(tableName = "readability_ignore", primaryKeys = ["slug", "ignoreKey"])
data class ReadabilityIgnoreEntity(
  val slug: String,
  val ignoreKey: String,
)

@Dao
interface ReadabilityIgnoreDao {
  @Query("SELECT ignoreKey FROM readability_ignore WHERE slug = :slug")
  suspend fun keysFor(slug: String): List<String>

  @Upsert
  suspend fun add(entity: ReadabilityIgnoreEntity)

  /** The only way rows leave this table: the review screen's recompute action,
   * which drops every ignore for the post so the analysis starts clean. */
  @Query("DELETE FROM readability_ignore WHERE slug = :slug")
  suspend fun clearFor(slug: String)
}
