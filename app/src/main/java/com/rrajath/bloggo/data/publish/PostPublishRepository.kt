package com.rrajath.bloggo.data.publish

import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.github.CommitFile
import com.rrajath.bloggo.data.github.CommitResult
import com.rrajath.bloggo.data.github.FileContent
import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.data.resolvePagePath
import com.rrajath.bloggo.data.resolvePostPath
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.StagedMedia
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PublishResult {
  data class Success(val repoPath: String, val commitSha: String) : PublishResult
  data class Failed(val error: GitHubApiError) : PublishResult
}

/**
 * Turns a [Post] plus whatever [StagedMedia] it claims into one real GitHub
 * commit, via [GitHubClient.commitFiles]. No Room cache of its own — unlike
 * `PostLibraryRepository`, there is nothing here worth caching between calls;
 * this is purely "resolve the path, build the file list, make the call."
 */
class PostPublishRepository(private val gitHubClient: GitHubClient) {
  suspend fun publish(
    post: Post,
    connection: RepoConnection,
    token: String?,
    message: String,
    stagedMediaForThisPost: List<StagedMedia>,
  ): PublishResult = withContext(Dispatchers.IO) {
    // post.slug is this app's own internal identifier (timestamp-suffixed for
    // uniqueness before a post has real frontmatter to key off — see
    // Post.publicSlug's doc comment); the file actually committed to the repo
    // must be named after the clean, public-facing slug instead, the same one
    // Post.liveUrl() already builds permalinks from.
    val path = post.repoPath ?: if (post.kind == DocKind.Page) {
      resolvePagePath(post.publicSlug)
    } else {
      resolvePostPath(connection.postPath, post.publicSlug)
    }

    val files = buildList {
      add(CommitFile(path, FileContent.Text(post.markdown)))
      stagedMediaForThisPost.forEach { media ->
        add(CommitFile(media.repoPath, FileContent.Base64(File(media.localPath).readBytes())))
      }
    }

    when (
      val result = gitHubClient.commitFiles(
        repository = connection.repository,
        branch = connection.branch,
        token = token,
        message = message,
        files = files,
      )
    ) {
      is CommitResult.Success -> PublishResult.Success(repoPath = path, commitSha = result.commitSha)
      is CommitResult.Failed -> PublishResult.Failed(result.error)
    }
  }
}
