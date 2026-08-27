package com.rrajath.bloggo.data.library

import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.data.github.PageTreeEntry
import com.rrajath.bloggo.data.github.PagesTreeResult
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.formatShortFrontmatterDate
import com.rrajath.bloggo.model.markdownWordCount
import com.rrajath.bloggo.model.parseFrontmatter
import com.rrajath.bloggo.model.parseFrontmatterDateEpochMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface PageLibraryRefreshResult {
  data class Success(val pages: List<Post>) : PageLibraryRefreshResult
  data class Failed(val error: GitHubApiError) : PageLibraryRefreshResult
}

/**
 * The Pages tab's counterpart to [PostLibraryRepository] — the same one-tree-
 * call, cache-by-blob-SHA shape, over the top-level `.md` files directly under `content/`
 * [GitHubClient.getPagesTree] lists instead of `content/posts/`. Every page it
 * returns has already been fetched from the repo, so [PostState.Published]
 * here doubles as "already pushed" — see [com.rrajath.bloggo.model.Post.kind]'s
 * caller in `PagesScreen.kt` for why that's the signal that routes a tap to
 * Preview instead of Editor.
 */
class PageLibraryRepository(
  private val gitHubClient: GitHubClient,
  private val pageCacheDao: PageCacheDao,
) {
  private var treeEtag: String? = null

  suspend fun refresh(repository: String, branch: String, token: String?): PageLibraryRefreshResult {
    val cached = pageCacheDao.getAll().associateBy { it.path }

    val entries = when (val treeResult = gitHubClient.getPagesTree(repository, branch, token, treeEtag)) {
      is PagesTreeResult.Failed -> return PageLibraryRefreshResult.Failed(treeResult.error)
      PagesTreeResult.NotModified -> return PageLibraryRefreshResult.Success(cached.values.map { it.toPage() })
      is PagesTreeResult.Success -> {
        treeEtag = treeResult.etag
        treeResult.entries
      }
    }

    val updated = coroutineScope {
      val gate = Semaphore(MAX_CONCURRENT_FETCHES)
      entries
        .map { entry -> async { gate.withPermit { fetch(repository, branch, token, entry, cached[entry.path]) } } }
        .awaitAll()
        .filterNotNull()
    }

    pageCacheDao.replaceAll(updated)
    return PageLibraryRefreshResult.Success(updated.map { it.toPage() })
  }

  /** [PostLibraryRepository.loadCached]'s counterpart for pages. */
  suspend fun loadCached(): List<Post> = pageCacheDao.getAll().map { it.toPage() }

  /** [PostLibraryRepository.evict]'s counterpart for a page. */
  suspend fun evict(path: String) = pageCacheDao.deletePaths(listOf(path))

  private suspend fun fetch(
    repository: String,
    branch: String,
    token: String?,
    entry: PageTreeEntry,
    existing: PageCacheEntity?,
  ): PageCacheEntity? {
    if (existing != null && existing.blobSha == entry.blobSha) return existing
    // A per-file fetch failure keeps the stale cached entry, same as
    // PostLibraryRepository — a brand new file that fails to load is simply
    // retried on the next refresh.
    return gitHubClient.getFileContent(repository, entry.path, branch, token)
      ?.let { entry.toCacheEntity(it) }
      ?: existing
  }

  private companion object {
    const val MAX_CONCURRENT_FETCHES = 5
  }
}

private fun PageTreeEntry.toCacheEntity(markdown: String): PageCacheEntity {
  val frontmatter = markdown.parseFrontmatter()
  val slug = path.substringAfterLast('/').removeSuffix(".md")
  return PageCacheEntity(
    path = path,
    blobSha = blobSha,
    slug = slug,
    title = frontmatter["title"]?.takeIf { it.isNotBlank() } ?: slug,
    date = frontmatter["date"]?.takeIf { it.isNotBlank() },
    markdown = markdown,
    wordCount = markdown.markdownWordCount(),
  )
}

private fun PageCacheEntity.toPage(): Post = Post(
  slug = slug,
  title = title,
  state = PostState.Published,
  kind = DocKind.Page,
  markdown = markdown,
  wordCount = wordCount.coerceAtLeast(0),
  date = date?.let(::formatShortFrontmatterDate),
  dateMillis = date?.let(::parseFrontmatterDateEpochMillis),
  repoPath = path,
)
