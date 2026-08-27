package com.rrajath.bloggo.data.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable, per-post storage for the readability findings the writer has ignored
 * on the review screen. Without this the ignore set lived only in Compose state,
 * so closing the screen brought every dismissed highlight straight back.
 *
 * The invariant, enforced by [ReadabilityIgnoreDao] having no single-row delete:
 * an ignore is removed only by [clear], and the one call site of [clear] is the
 * writer tapping recompute. Nothing here runs a reconciliation pass.
 */
class ReadabilityIgnoreStore(private val dao: ReadabilityIgnoreDao) {

  /** Every ignore key for [slug], read once when the review screen opens. */
  suspend fun load(slug: String): Set<String> = withContext(Dispatchers.IO) {
    dao.keysFor(slug).toSet()
  }

  /** Records one ignore. Safe to call on every confirm — a single upsert. */
  suspend fun ignore(slug: String, key: String) = withContext(Dispatchers.IO) {
    dao.add(ReadabilityIgnoreEntity(slug = slug, ignoreKey = key))
  }

  /** Drops every ignore for [slug]. Called only from the recompute action. */
  suspend fun clear(slug: String) = withContext(Dispatchers.IO) {
    dao.clearFor(slug)
  }
}
