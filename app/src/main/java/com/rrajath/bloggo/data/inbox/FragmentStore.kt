package com.rrajath.bloggo.data.inbox

import com.rrajath.bloggo.model.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable storage for Inbox fragments — the fix for the same data-loss shape
 * [com.rrajath.bloggo.data.library.LocalPostStore] fixed for local posts/pages:
 * `fragments` (`BloggoApp.kt`) is pure in-memory Compose state, so without this a
 * captured thought survived only as long as the process did.
 *
 * Unlike [com.rrajath.bloggo.data.library.LocalPostStore], there is no file to
 * write alongside the Room row: a fragment never has its own git-file shape to
 * mirror — it only gets one once it's promoted to a [com.rrajath.bloggo.model.Post],
 * at which point it becomes that store's concern instead (see `BloggoApp.kt`'s
 * "Promote to Post" handler, which both persists the new post there and deletes
 * the fragment here in the same action).
 *
 * The same hard invariant as that store: a row is never removed except by
 * [delete], and every call to [delete] in this codebase is a direct result of an
 * explicit user action (promotion). Nothing here runs a reconciliation pass.
 */
class FragmentStore(private val fragmentDao: FragmentDao) {

  /** Every captured-but-unpromoted fragment, ready to merge into `BloggoApp`'s
   * in-memory list. Called once, at cold start; never removes a row. */
  suspend fun loadAll(): List<Fragment> = withContext(Dispatchers.IO) {
    fragmentDao.getAll().map { it.toFragment() }
  }

  /** Upserts [fragment]'s row. Safe to call on every capture — a single write,
   * not a scan of anything else stored here. */
  suspend fun save(fragment: Fragment) = withContext(Dispatchers.IO) {
    fragmentDao.upsert(
      FragmentEntity(
        id = fragment.id,
        text = fragment.text,
        capturedAtMillis = fragment.capturedAtMillis,
        tag = fragment.tag,
      )
    )
  }

  /** Removes [id]'s row for good. Every call site of this must be a direct,
   * explicit user action — see this class's own doc comment. */
  suspend fun delete(id: String) = withContext(Dispatchers.IO) {
    fragmentDao.deleteById(id)
  }
}

private fun FragmentEntity.toFragment(): Fragment = Fragment(
  id = id,
  text = text,
  capturedAtMillis = capturedAtMillis,
  tag = tag,
)
