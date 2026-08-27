package com.rrajath.bloggo.data.library

import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.data.github.PostTreeEntry
import com.rrajath.bloggo.data.github.PostsTreeResult
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.effectiveDate
import com.rrajath.bloggo.model.formatShortFrontmatterDate
import com.rrajath.bloggo.model.markdownWordCount
import com.rrajath.bloggo.model.parseFrontmatter
import com.rrajath.bloggo.model.parseFrontmatterDateEpochMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface LibraryRefreshResult {
  data class Success(val posts: List<Post>) : LibraryRefreshResult
  data class Failed(val error: GitHubApiError) : LibraryRefreshResult
}

/**
 * ANDROID_TDD.md §5.2: one recursive tree call lists every post's path and blob
 * SHA. Titles, dates and the draft flag live in frontmatter, so a post is only
 * re-fetched when its blob SHA no longer matches the cache — an unchanged SHA is
 * content-addressed proof the frontmatter hasn't changed either.
 */
class PostLibraryRepository(
  private val gitHubClient: GitHubClient,
  private val postCacheDao: PostCacheDao,
) {
  /** The last tree response's ETag, replayed as `If-None-Match`. Process-lifetime
   * only: a 304 saves a full tree download and a rate-limit unit on every refresh
   * after the first, and losing it across a restart costs exactly one tree call. */
  private var treeEtag: String? = null

  suspend fun refresh(repository: String, branch: String, token: String?): LibraryRefreshResult {
    val cached = postCacheDao.getAll().associateBy { it.path }

    val entries = when (val treeResult = gitHubClient.getPostsTree(repository, branch, token, treeEtag)) {
      is PostsTreeResult.Failed -> return LibraryRefreshResult.Failed(treeResult.error)
      // Nothing in the repo moved. The cache is the answer, and it cost one 304.
      PostsTreeResult.NotModified -> return LibraryRefreshResult.Success(cached.values.map { it.toPost() })
      is PostsTreeResult.Success -> {
        treeEtag = treeResult.etag
        treeResult.entries
      }
    }

    // Fetched concurrently, with a gate. Serially, first sync of an N-post blog
    // was N round trips end to end — a minute of empty library on a real blog at
    // a real RTT. The permit count matches OkHttp's own default
    // maxRequestsPerHost, so the dispatcher is the thing doing the queueing
    // rather than a queue behind a queue.
    val updated = coroutineScope {
      val gate = Semaphore(MAX_CONCURRENT_FETCHES)
      entries
        .map { entry -> async { gate.withPermit { fetch(repository, branch, token, entry, cached[entry.path]) } } }
        .awaitAll()
        .filterNotNull()
    }

    postCacheDao.replaceAll(updated)
    return LibraryRefreshResult.Success(updated.map { it.toPost() })
  }

  /** The last synced snapshot, straight from the Room cache with no network
   * call — read at cold start so Library shows real posts immediately instead
   * of the SampleData placeholders sitting on screen for however long
   * [refresh]'s tree-plus-per-file network round trip takes. Empty until the
   * very first successful [refresh] this device has ever done. */
  suspend fun loadCached(): List<Post> = postCacheDao.getAll().map { it.toPost() }

  /** Evicts [path]'s row from the cache — the local-state half of deleting an
   * already-pushed post (`BloggoApp.kt`'s `onDeletePost`), called only after
   * the remote delete itself has already succeeded. Without this, a stale
   * cache row would resurrect the just-deleted post the next time this
   * repository serves a 304 [refresh] rather than a fresh tree. */
  suspend fun evict(path: String) = postCacheDao.deletePaths(listOf(path))

  private suspend fun fetch(
    repository: String,
    branch: String,
    token: String?,
    entry: PostTreeEntry,
    existing: PostCacheEntity?,
  ): PostCacheEntity? {
    if (existing != null && existing.blobSha == entry.blobSha) {
      // An unchanged blob SHA is content-addressed proof the word count is
      // unchanged too — except on the first refresh after the cache gained the
      // column, where it is recovered from the markdown already in hand.
      return if (existing.wordCount == PostCacheEntity.UNKNOWN_WORD_COUNT) {
        existing.copy(wordCount = existing.markdown.markdownWordCount())
      } else {
        existing
      }
    }
    // A per-file fetch failure keeps the stale cached entry rather than
    // dropping the post from the library; a brand new file that fails to
    // load is simply retried on the next refresh.
    return gitHubClient.getFileContent(repository, entry.path, branch, token)
      ?.let { entry.toCacheEntity(it) }
      ?: existing
  }

  private companion object {
    const val MAX_CONCURRENT_FETCHES = 5
  }
}

private fun PostTreeEntry.toCacheEntity(markdown: String): PostCacheEntity {
  val frontmatter = markdown.parseFrontmatter()
  val slug = path.substringAfterLast('/').removeSuffix(".md")
  return PostCacheEntity(
    path = path,
    blobSha = blobSha,
    slug = slug,
    title = frontmatter["title"]?.takeIf { it.isNotBlank() } ?: slug,
    date = frontmatter.effectiveDate(),
    draft = frontmatter["draft"]?.equals("true", ignoreCase = true) ?: false,
    cover = frontmatter["cover"]?.takeIf { it.isNotBlank() },
    markdown = markdown,
    wordCount = markdown.markdownWordCount(),
  )
}

private fun PostCacheEntity.toPost(): Post = Post(
  slug = slug,
  title = title,
  state = if (draft) PostState.Draft else PostState.Published,
  markdown = markdown,
  wordCount = wordCount.coerceAtLeast(0),
  date = date?.let(::formatShortFrontmatterDate),
  // Parsed here, next to the display string it comes from, so sorting the
  // library never has to reach back into the markdown for it.
  dateMillis = date?.let(::parseFrontmatterDateEpochMillis),
  cover = cover,
  repoPath = path,
)
