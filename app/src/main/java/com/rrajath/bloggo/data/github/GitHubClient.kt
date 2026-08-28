package com.rrajath.bloggo.data.github

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** What ANDROID_TDD.md §7.1 asks for: not just "invalid", but what was found. */
sealed interface ConnectionCheck {
  data class Connected(
    val defaultBranch: String,
    val isPrivate: Boolean,
  ) : ConnectionCheck

  /** 401, or 403 that isn't a rate limit: the token is missing, expired, or wrong. */
  data object Unauthorized : ConnectionCheck

  /** 403 with a rate-limit header. Distinct from [Unauthorized] because the fix is "wait", not "reconnect". */
  data object RateLimited : ConnectionCheck

  /** 404: GitHub returns this for both "doesn't exist" and "no access", to avoid leaking private repos. */
  data object NotFound : ConnectionCheck

  data object NoNetwork : ConnectionCheck
  data class Unknown(val httpCode: Int?) : ConnectionCheck
}

/** The error taxonomy of ANDROID_TDD.md §5.5, shared by every call this client makes. */
sealed interface GitHubApiError {
  data object Unauthorized : GitHubApiError
  data object RateLimited : GitHubApiError
  data object NotFound : GitHubApiError
  data object NoNetwork : GitHubApiError

  /** [GitHubClient.commitFiles] only: the branch moved between reading its head
   * and updating the ref, so the ref update was rejected as a non-fast-forward
   * change rather than silently force-pushed over. The fix is "refetch and
   * retry", not "reconnect" — distinct from a generic [Unknown] for that reason. */
  data object Conflict : GitHubApiError
  data class Unknown(val httpCode: Int?) : GitHubApiError
}

/** One human-readable line per [GitHubApiError] variant — shared so a library
 * refresh failure and a publish failure never describe the same error two
 * different ways. */
fun GitHubApiError.describe(): String = when (this) {
  GitHubApiError.Unauthorized -> "reconnect on the Settings screen"
  GitHubApiError.RateLimited -> "rate limited, try again shortly"
  GitHubApiError.NotFound -> "repo not found"
  GitHubApiError.NoNetwork -> "no network"
  GitHubApiError.Conflict -> "someone else pushed to this branch — refresh and try again"
  is GitHubApiError.Unknown -> "unexpected error"
}

/** A markdown post entry from under `content/posts/`, per ANDROID_TDD.md §5.2. */
data class PostTreeEntry(val path: String, val blobSha: String)

sealed interface PostsTreeResult {
  /** [etag] is the tree response's ETag when GitHub sent one, to hand back as
   * `If-None-Match` on the next refresh. Null for the directory-listing
   * fallback, which is a different resource. */
  data class Success(val entries: List<PostTreeEntry>, val etag: String? = null) : PostsTreeResult

  /** 304: the tree is byte-for-byte what the caller already has. Costs no
   * rate-limit unit and carries no body, so the caller serves its own cache. */
  data object NotModified : PostsTreeResult

  data class Failed(val error: GitHubApiError) : PostsTreeResult
}

/** A markdown page entry from a top-level file under `content/` — not
 * `content/posts/`. The Pages tab's counterpart to [PostTreeEntry]; see
 * docs/PROTOTYPE_NOTES.md's "Pages replaced Media". */
data class PageTreeEntry(val path: String, val blobSha: String)

sealed interface PagesTreeResult {
  data class Success(val entries: List<PageTreeEntry>, val etag: String? = null) : PagesTreeResult
  data object NotModified : PagesTreeResult
  data class Failed(val error: GitHubApiError) : PagesTreeResult
}

/** An image file under the repo's configured image path, real (already
 * committed) — the Media screen's counterpart to [PostTreeEntry]. */
data class ImageTreeEntry(val path: String, val blobSha: String)

sealed interface ImagesTreeResult {
  data class Success(val entries: List<ImageTreeEntry>, val etag: String? = null) : ImagesTreeResult
  data object NotModified : ImagesTreeResult
  data class Failed(val error: GitHubApiError) : ImagesTreeResult
}

/** One file's content for [GitHubClient.commitFiles] — text for markdown,
 * base64 for anything binary (images). GitHub's blob endpoint accepts either
 * directly, so no caller ever base64-encodes text or decodes an image itself. */
sealed interface FileContent {
  data class Text(val value: String) : FileContent
  data class Base64(val bytes: ByteArray) : FileContent
}

/** [path] is repo-relative, e.g. `content/posts/on-agents.md`. */
data class CommitFile(val path: String, val content: FileContent)

sealed interface CommitResult {
  data class Success(val commitSha: String) : CommitResult
  data class Failed(val error: GitHubApiError) : CommitResult
}

private fun classifyHttpError(httpCode: Int, rateLimitRemaining: String?): GitHubApiError = when {
  httpCode == 401 -> GitHubApiError.Unauthorized
  httpCode == 403 && rateLimitRemaining == "0" -> GitHubApiError.RateLimited
  httpCode == 403 -> GitHubApiError.Unauthorized
  httpCode == 404 -> GitHubApiError.NotFound
  else -> GitHubApiError.Unknown(httpCode)
}

private fun parseOwnerRepo(repository: String): Pair<String, String>? {
  val parts = repository.split("/", limit = 2)
  if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
  return parts[0] to parts[1]
}

private const val POSTS_PATH = "content/posts/"
private const val CONTENT_PATH = "content/"

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "svg")

/**
 * The GitHub half of ANDROID_TDD.md §5. Deliberately small: one call to
 * validate the token and repo (§7.1), one recursive tree call for the library
 * (§5.2).
 *
 * The tree call is conditional — pass the previous response's ETag and an
 * unchanged repo answers 304 for free. The caller owns that ETag, since it also
 * owns the cache the 304 is an assertion about.
 */
class GitHubClient(baseUrl: String = "https://api.github.com/") {
  // encodeDefaults = true: without it, kotlinx.serialization drops any field
  // still at its default value — TreeEntryInputDto.mode and .type never made
  // it into the request body, and GitHub's git/trees endpoint 422'd every
  // single commit with "Must supply a valid tree.mode".
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  private val okHttp = OkHttpClient.Builder()
    .withDebugLogging()
    .build()

  private val api = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(okHttp)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(GitHubApi::class.java)

  suspend fun checkConnection(repository: String, branch: String, token: String?): ConnectionCheck =
    withContext(Dispatchers.IO) {
      val (owner, repo) = parseOwnerRepo(repository) ?: return@withContext ConnectionCheck.NotFound
      val authorization = authorizationHeader(token)

      try {
        val response = api.getRepo(authorization, owner, repo)
        if (response.isSuccessful) {
          val body = response.body() ?: return@withContext ConnectionCheck.Unknown(response.code())
          return@withContext ConnectionCheck.Connected(
            defaultBranch = body.defaultBranch,
            isPrivate = body.private,
          )
        }
        when (classifyHttpError(response.code(), response.headers()["x-ratelimit-remaining"])) {
          GitHubApiError.Unauthorized -> ConnectionCheck.Unauthorized
          GitHubApiError.RateLimited -> ConnectionCheck.RateLimited
          GitHubApiError.NotFound -> ConnectionCheck.NotFound
          GitHubApiError.NoNetwork -> ConnectionCheck.NoNetwork
          // classifyHttpError never actually produces this — Conflict is only ever
          // constructed directly, at commitFiles' ref-update call site — but it's
          // part of the same sealed GitHubApiError, so this branch exists for
          // exhaustiveness rather than a real code path.
          GitHubApiError.Conflict -> ConnectionCheck.Unknown(response.code())
          is GitHubApiError.Unknown -> ConnectionCheck.Unknown(response.code())
        }
      } catch (e: IOException) {
        ConnectionCheck.NoNetwork
      }
    }

  /**
   * ANDROID_TDD.md §5.2: one recursive tree call lists every path and blob SHA,
   * filtered to markdown files under `content/posts/`. Falls back to a plain directory listing
   * when the tree response is `truncated` — the trap the doc calls out by name.
   */
  suspend fun getPostsTree(
    repository: String,
    branch: String,
    token: String?,
    etag: String? = null,
  ): PostsTreeResult = withContext(Dispatchers.IO) {
    val (owner, repo) = parseOwnerRepo(repository)
      ?: return@withContext PostsTreeResult.Failed(GitHubApiError.NotFound)
    val authorization = authorizationHeader(token)

    when (
      val result = fetchTree(authorization, owner, repo, branch, etag, POSTS_PATH) { it.path.endsWith(".md") }
    ) {
      is RawTreeFetch.Success ->
        PostsTreeResult.Success(result.entries.map { PostTreeEntry(it.path, it.sha) }, result.etag)
      RawTreeFetch.NotModified -> PostsTreeResult.NotModified
      is RawTreeFetch.Failed -> PostsTreeResult.Failed(result.error)
    }
  }

  /**
   * The Pages tab's counterpart to [getPostsTree] — same one-recursive-call
   * shape, filtered to top-level `.md` files directly under `content/`: direct children of
   * `content/`, which excludes everything under `content/posts/` (or any
   * other subdirectory) by construction rather than by name-matching
   * "posts" — see docs/PROTOTYPE_NOTES.md's "Pages replaced Media".
   */
  suspend fun getPagesTree(
    repository: String,
    branch: String,
    token: String?,
    etag: String? = null,
  ): PagesTreeResult = withContext(Dispatchers.IO) {
    val (owner, repo) = parseOwnerRepo(repository)
      ?: return@withContext PagesTreeResult.Failed(GitHubApiError.NotFound)
    val authorization = authorizationHeader(token)

    when (
      val result = fetchTree(authorization, owner, repo, branch, etag, CONTENT_PATH) { entry ->
        entry.path.endsWith(".md") && !entry.path.removePrefix(CONTENT_PATH).contains("/")
      }
    ) {
      is RawTreeFetch.Success ->
        PagesTreeResult.Success(result.entries.map { PageTreeEntry(it.path, it.sha) }, result.etag)
      RawTreeFetch.NotModified -> PagesTreeResult.NotModified
      is RawTreeFetch.Failed -> PagesTreeResult.Failed(result.error)
    }
  }

  /** The Media screen's counterpart to [getPostsTree] — same one-recursive-call
   * shape, filtered to image extensions under [imagePath] instead of markdown
   * under `content/posts/`. Shares [fetchTree] rather than a second, less-proven
   * way of asking GitHub the same kind of question. */
  suspend fun getImagesTree(
    repository: String,
    branch: String,
    imagePath: String,
    token: String?,
    etag: String? = null,
  ): ImagesTreeResult = withContext(Dispatchers.IO) {
    val (owner, repo) = parseOwnerRepo(repository)
      ?: return@withContext ImagesTreeResult.Failed(GitHubApiError.NotFound)
    val authorization = authorizationHeader(token)
    val prefix = imagePath.trim().trimStart('/').let { if (it.endsWith('/')) it else "$it/" }

    when (
      val result = fetchTree(authorization, owner, repo, branch, etag, prefix) { entry ->
        entry.path.substringAfterLast('.', missingDelimiterValue = "").lowercase() in IMAGE_EXTENSIONS
      }
    ) {
      is RawTreeFetch.Success ->
        ImagesTreeResult.Success(result.entries.map { ImageTreeEntry(it.path, it.sha) }, result.etag)
      RawTreeFetch.NotModified -> ImagesTreeResult.NotModified
      is RawTreeFetch.Failed -> ImagesTreeResult.Failed(result.error)
    }
  }

  private sealed interface RawTreeFetch {
    data class Success(val entries: List<GitTreeEntryDto>, val etag: String? = null) : RawTreeFetch
    data object NotModified : RawTreeFetch
    data class Failed(val error: GitHubApiError) : RawTreeFetch
  }

  /** One recursive tree call, filtered to blobs under [pathPrefix] and passing
   * [extraFilter], falling back to a plain listing of [pathPrefix] itself when
   * the tree response is `truncated` — the shape §5.2 validated for posts,
   * shared here so images ask GitHub the same kind of question the same way. */
  private suspend fun fetchTree(
    authorization: String?,
    owner: String,
    repo: String,
    branch: String,
    etag: String?,
    pathPrefix: String,
    extraFilter: (GitTreeEntryDto) -> Boolean,
  ): RawTreeFetch = try {
    val response = api.getTree(authorization, owner, repo, branch, recursive = 1, ifNoneMatch = etag)
    if (response.code() == 304) return RawTreeFetch.NotModified
    if (!response.isSuccessful) {
      return RawTreeFetch.Failed(classifyHttpError(response.code(), response.headers()["x-ratelimit-remaining"]))
    }
    val body = response.body() ?: return RawTreeFetch.Failed(GitHubApiError.Unknown(response.code()))
    if (body.truncated) {
      return fetchDirectory(authorization, owner, repo, branch, pathPrefix, extraFilter)
    }
    val entries = body.tree.filter { it.type == "blob" && it.path.startsWith(pathPrefix) && extraFilter(it) }
    RawTreeFetch.Success(entries, etag = response.headers()["etag"])
  } catch (e: IOException) {
    RawTreeFetch.Failed(GitHubApiError.NoNetwork)
  }

  private suspend fun fetchDirectory(
    authorization: String?,
    owner: String,
    repo: String,
    branch: String,
    path: String,
    extraFilter: (GitTreeEntryDto) -> Boolean,
  ): RawTreeFetch = try {
    val response = api.listDirectory(authorization, owner, repo, path.trimEnd('/'), branch)
    if (!response.isSuccessful) {
      return RawTreeFetch.Failed(classifyHttpError(response.code(), response.headers()["x-ratelimit-remaining"]))
    }
    val entries = response.body().orEmpty()
      .filter { it.type == "file" }
      .map { GitTreeEntryDto(path = it.path, mode = "100644", type = "blob", sha = it.sha) }
      .filter(extraFilter)
    RawTreeFetch.Success(entries)
  } catch (e: IOException) {
    RawTreeFetch.Failed(GitHubApiError.NoNetwork)
  }

  /** A post's raw markdown, or null on any failure — the caller falls back to its cache. */
  suspend fun getFileContent(repository: String, path: String, branch: String, token: String?): String? =
    withContext(Dispatchers.IO) {
      val (owner, repo) = parseOwnerRepo(repository) ?: return@withContext null
      val authorization = authorizationHeader(token)
      try {
        val response = api.getRawFile(authorization, owner, repo, path, branch)
        if (response.isSuccessful) response.body()?.string() else null
      } catch (e: IOException) {
        null
      }
    }

  /**
   * The Git Data API's own four-call shape for a real commit, one blob per
   * [files] entry so a post and its images land in a single commit rather
   * than one per file: [getBranch] for the parent commit and base tree, one
   * [createBlob] per file (concurrent, gated like [getPostsTree]'s per-post
   * fetches), [createTree] layered on the base tree so every file the commit
   * doesn't touch carries forward unchanged, [createCommit], then
   * [updateRef]. The ref update is never forced — a rejected non-fast-forward
   * means [branch] moved since the first call, and that must come back as
   * [GitHubApiError.Conflict], not a silent overwrite.
   */
  suspend fun commitFiles(
    repository: String,
    branch: String,
    token: String?,
    message: String,
    files: List<CommitFile>,
  ): CommitResult = withContext(Dispatchers.IO) {
    val (owner, repo) = parseOwnerRepo(repository)
      ?: return@withContext CommitResult.Failed(GitHubApiError.NotFound)
    val authorization = authorizationHeader(token)

    try {
      val branchResponse = api.getBranch(authorization, owner, repo, branch)
      if (!branchResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(branchResponse.code(), branchResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val head = branchResponse.body() ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(branchResponse.code()))

      val blobShas = coroutineScope {
        val gate = Semaphore(MAX_CONCURRENT_BLOB_CREATES)
        files
          .map { file -> async { gate.withPermit { createBlob(authorization, owner, repo, file) } } }
          .awaitAll()
      }
      blobShas.filterIsInstance<BlobOutcome.Failed>().firstOrNull()?.let {
        return@withContext CommitResult.Failed(it.error)
      }
      val entries = files.zip(blobShas).map { (file, outcome) ->
        TreeEntryInputDto(path = file.path, sha = (outcome as BlobOutcome.Success).sha)
      }

      val treeResponse = api.createTree(
        authorization, owner, repo,
        CreateTreeRequestDto(baseTree = head.commit.commit.tree.sha, tree = entries),
      )
      if (!treeResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(treeResponse.code(), treeResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val newTreeSha = treeResponse.body()?.sha
        ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(treeResponse.code()))

      val commitResponse = api.createCommit(
        authorization, owner, repo,
        CreateCommitRequestDto(message = message, tree = newTreeSha, parents = listOf(head.commit.sha)),
      )
      if (!commitResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(commitResponse.code(), commitResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val newCommitSha = commitResponse.body()?.sha
        ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(commitResponse.code()))

      val refResponse = api.updateRef(
        authorization, owner, repo, branch,
        UpdateRefRequestDto(sha = newCommitSha, force = false),
      )
      if (!refResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          if (refResponse.code() == 422) GitHubApiError.Conflict
          else classifyHttpError(refResponse.code(), refResponse.headers()["x-ratelimit-remaining"])
        )
      }

      CommitResult.Success(newCommitSha)
    } catch (e: IOException) {
      CommitResult.Failed(GitHubApiError.NoNetwork)
    }
  }

  /**
   * Removes [path] from the repo in its own commit — the delete-a-pushed-post
   * counterpart to [commitFiles], sharing its Git Data API shape ([getBranch]
   * for the parent commit and base tree, [createTree], [createCommit],
   * [updateRef]) rather than a second, less-proven way of asking GitHub the
   * same kind of question. No blob is created: the single [TreeEntryInputDto]
   * sent to [GitHubApi.createTree] carries a `null` sha, which is the Git
   * Trees API's own documented way to drop a path from the tree it's layered
   * on to, leaving every other file the commit doesn't touch unchanged. The
   * ref update is never forced, for the same non-fast-forward-must-surface-as-
   * [GitHubApiError.Conflict] reason [commitFiles] never forces it either.
   */
  suspend fun deleteFile(
    repository: String,
    branch: String,
    token: String?,
    message: String,
    path: String,
  ): CommitResult = withContext(Dispatchers.IO) {
    val (owner, repo) = parseOwnerRepo(repository)
      ?: return@withContext CommitResult.Failed(GitHubApiError.NotFound)
    val authorization = authorizationHeader(token)

    try {
      val branchResponse = api.getBranch(authorization, owner, repo, branch)
      if (!branchResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(branchResponse.code(), branchResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val head = branchResponse.body() ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(branchResponse.code()))

      val treeResponse = api.createTree(
        authorization, owner, repo,
        CreateTreeRequestDto(baseTree = head.commit.commit.tree.sha, tree = listOf(TreeEntryInputDto(path = path, sha = null))),
      )
      if (!treeResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(treeResponse.code(), treeResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val newTreeSha = treeResponse.body()?.sha
        ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(treeResponse.code()))

      val commitResponse = api.createCommit(
        authorization, owner, repo,
        CreateCommitRequestDto(message = message, tree = newTreeSha, parents = listOf(head.commit.sha)),
      )
      if (!commitResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          classifyHttpError(commitResponse.code(), commitResponse.headers()["x-ratelimit-remaining"])
        )
      }
      val newCommitSha = commitResponse.body()?.sha
        ?: return@withContext CommitResult.Failed(GitHubApiError.Unknown(commitResponse.code()))

      val refResponse = api.updateRef(
        authorization, owner, repo, branch,
        UpdateRefRequestDto(sha = newCommitSha, force = false),
      )
      if (!refResponse.isSuccessful) {
        return@withContext CommitResult.Failed(
          if (refResponse.code() == 422) GitHubApiError.Conflict
          else classifyHttpError(refResponse.code(), refResponse.headers()["x-ratelimit-remaining"])
        )
      }

      CommitResult.Success(newCommitSha)
    } catch (e: IOException) {
      CommitResult.Failed(GitHubApiError.NoNetwork)
    }
  }

  private sealed interface BlobOutcome {
    data class Success(val sha: String) : BlobOutcome
    data class Failed(val error: GitHubApiError) : BlobOutcome
  }

  private suspend fun createBlob(
    authorization: String?,
    owner: String,
    repo: String,
    file: CommitFile,
  ): BlobOutcome {
    val request = when (val content = file.content) {
      is FileContent.Text -> CreateBlobRequestDto(content = content.value, encoding = "utf-8")
      is FileContent.Base64 -> CreateBlobRequestDto(
        content = Base64.getEncoder().encodeToString(content.bytes),
        encoding = "base64",
      )
    }
    val response = api.createBlob(authorization, owner, repo, request)
    if (!response.isSuccessful) {
      return BlobOutcome.Failed(classifyHttpError(response.code(), response.headers()["x-ratelimit-remaining"]))
    }
    val sha = response.body()?.sha ?: return BlobOutcome.Failed(GitHubApiError.Unknown(response.code()))
    return BlobOutcome.Success(sha)
  }

  private fun authorizationHeader(token: String?): String? =
    token?.trim()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

  private companion object {
    const val MAX_CONCURRENT_BLOB_CREATES = 5
  }
}
