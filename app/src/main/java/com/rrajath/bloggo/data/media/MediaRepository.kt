package com.rrajath.bloggo.data.media

import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.data.github.ImageTreeEntry
import com.rrajath.bloggo.data.github.ImagesTreeResult
import com.rrajath.bloggo.data.repoPathToSitePath
import com.rrajath.bloggo.model.MediaFile

sealed interface MediaRefreshResult {
  data class Success(val files: List<MediaFile>) : MediaRefreshResult
  data class Failed(val error: GitHubApiError) : MediaRefreshResult
}

/** The Media screen's real half — everything already committed under the
 * repo's configured image path. No caching of its own: unlike posts, there's
 * no per-file content to fetch and diff against, just a listing, so there's
 * nothing here worth a Room table. Staged-but-uncommitted images are a
 * separate, purely local concern (`BloggoApp`'s `stagedMedia` registry) merged
 * in by the caller, not this repository. */
class MediaRepository(private val gitHubClient: GitHubClient) {
  suspend fun refresh(repository: String, branch: String, imagePath: String, token: String?): MediaRefreshResult =
    when (val result = gitHubClient.getImagesTree(repository, branch, imagePath, token)) {
      is ImagesTreeResult.Success -> MediaRefreshResult.Success(result.entries.map { it.toMediaFile() })
      // Never actually reached: this repository never passes an etag through,
      // so GitHub has nothing to answer 304 against. Kept for exhaustiveness
      // against ImagesTreeResult, not a real code path.
      ImagesTreeResult.NotModified -> MediaRefreshResult.Success(emptyList())
      is ImagesTreeResult.Failed -> MediaRefreshResult.Failed(result.error)
    }
}

private fun ImageTreeEntry.toMediaFile(): MediaFile = MediaFile(
  name = path.substringAfterLast('/'),
  seed = path,
  sitePath = repoPathToSitePath(path),
  repoPath = path,
)
