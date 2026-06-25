package com.rrajath.bloggo.data

import com.rrajath.bloggo.data.network.ContentRequest
import com.rrajath.bloggo.data.network.ContentResponse
import com.rrajath.bloggo.data.network.ContentUpdateResponse
import com.rrajath.bloggo.data.network.GitHubService
import com.rrajath.bloggo.data.network.TreeItem
import com.rrajath.bloggo.domain.FrontMatter
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import com.rrajath.bloggo.domain.toPostDraft
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val gitHubService: GitHubService,
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
) {
    data class RefreshResult(
        val remotePosts: Int = 0,
        val mergedPosts: Int = 0,
        val error: String? = null,
    )

    data class PublishResult(
        val post: PostDraft? = null,
        val error: String? = null,
        val staleSha: Boolean = false,
    )

    suspend fun refresh(): RefreshResult {
        if (!networkMonitor.checkConnectivity()) {
            return RefreshResult(error = "Offline — cannot refresh from GitHub.")
        }

        val settings = settingsRepository.settings.first()
        val (owner, repo) = settings.ownerRepo ?: return RefreshResult(error = "Repository not configured.")
        val branch = settings.branch
        val contentPath = settings.contentPath

        val treeResponse = try {
            gitHubService.getTree(owner, repo, branch)
        } catch (e: Exception) {
            return RefreshResult(error = "Failed to fetch tree: ${e.message}")
        }

        if (!treeResponse.isSuccessful) {
            return RefreshResult(error = "GitHub API error: ${treeResponse.code()}")
        }

        val tree = treeResponse.body()?.tree ?: emptyList()
        val mdFiles = tree.filter { it.type == "blob" && it.path.endsWith(".md") && it.path.startsWith(contentPath) }

        val remotePosts = mutableListOf<PostDraft>()
        for (item in mdFiles) {
            val contentResponse = try {
                gitHubService.getContent(owner, repo, item.path, branch)
            } catch (e: Exception) {
                continue
            }

            if (!contentResponse.isSuccessful) continue

            val content = contentResponse.body()?.let { decodeContent(it) } ?: continue
            val parsed = FrontMatter.parse(content)
            val post = parsed.toPostDraft(
                localId = UUID.randomUUID().toString(),
                syncState = SyncState.SYNCED,
            ).copy(
                repoPath = item.path,
                blobSha = item.sha,
            )
            remotePosts.add(post)
        }

        mergeRemotePosts(remotePosts)

        return RefreshResult(
            remotePosts = remotePosts.size,
            mergedPosts = postRepository.count(),
        )
    }

    private suspend fun mergeRemotePosts(remotePosts: List<PostDraft>) {
        val localPosts = postRepository.observeAllPosts().first()

        val remoteByPath = remotePosts.associateBy { it.repoPath }
        val localByPath = localPosts.filter { it.repoPath != null }.associateBy { it.repoPath }

        for (remote in remotePosts) {
            val local = remoteByPath[remote.repoPath]?.let { null } ?: localByPath[remote.repoPath]
            if (local != null) {
                if (local.syncState == SyncState.SYNCED_MODIFIED) {
                    continue
                }
                postRepository.savePost(remote.copy(localId = local.localId))
            } else {
                postRepository.savePost(remote)
            }
        }

        val remotePaths = remotePosts.map { it.repoPath }.toSet()
        for (local in localPosts) {
            if (local.syncState == SyncState.SYNCED && local.repoPath != null && local.repoPath !in remotePaths) {
                postRepository.deletePost(local.localId)
            }
        }
    }

    suspend fun publish(post: PostDraft): PublishResult {
        if (!networkMonitor.checkConnectivity()) {
            return PublishResult(error = "Offline — publish is disabled.")
        }

        val settings = settingsRepository.settings.first()
        val (owner, repo) = settings.ownerRepo ?: return PublishResult(error = "Repository not configured.")
        val branch = settings.branch
        val contentPath = settings.contentPath
        val targetPath = post.targetPath(contentPath)

        val assembled = FrontMatter.assemble(post)
        if (assembled.warnings.isNotEmpty()) {
            // Warnings are surfaced but don't block publish
        }

        val encodedContent = encodeContent(assembled.content)
        val commitMessage = "New post: ${post.title.ifBlank { "Untitled" }}"

        val request = ContentRequest(
            message = commitMessage,
            content = encodedContent,
            branch = branch,
            sha = post.blobSha,
        )

        val response = try {
            gitHubService.putContent(owner, repo, targetPath, request)
        } catch (e: Exception) {
            return PublishResult(error = "Network error: ${e.message}")
        }

        if (response.isSuccessful) {
            val updateBody = response.body()
            val newSha = updateBody?.content?.sha ?: post.blobSha
            val published = post.copy(
                syncState = SyncState.SYNCED,
                blobSha = newSha,
                repoPath = targetPath,
                slugAutoDerive = false,
            )
            postRepository.savePost(published)
            return PublishResult(post = published)
        }

        if (response.code() in setOf(409, 422) && post.blobSha != null) {
            val refetchResult = refetchAndRetry(post, owner, repo, branch, targetPath, encodedContent, commitMessage)
            return refetchResult
        }

        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return PublishResult(error = "GitHub API error (${response.code()}): $errorBody")
    }

    private suspend fun refetchAndRetry(
        post: PostDraft,
        owner: String,
        repo: String,
        branch: String,
        targetPath: String,
        encodedContent: String,
        commitMessage: String,
    ): PublishResult {
        val refetchResponse = try {
            gitHubService.getContent(owner, repo, targetPath, branch)
        } catch (e: Exception) {
            return PublishResult(error = "Failed to refetch stale file: ${e.message}", staleSha = true)
        }

        if (!refetchResponse.isSuccessful) {
            return PublishResult(error = "Failed to refetch stale file: ${refetchResponse.code()}", staleSha = true)
        }

        val currentSha = refetchResponse.body()?.sha
            ?: return PublishResult(error = "Could not determine current file SHA.", staleSha = true)

        val retryRequest = ContentRequest(
            message = commitMessage,
            content = encodedContent,
            branch = branch,
            sha = currentSha,
        )

        val retryResponse = try {
            gitHubService.putContent(owner, repo, targetPath, retryRequest)
        } catch (e: Exception) {
            return PublishResult(error = "Network error on retry: ${e.message}", staleSha = true)
        }

        if (retryResponse.isSuccessful) {
            val newSha = retryResponse.body()?.content?.sha ?: currentSha
            val published = post.copy(
                syncState = SyncState.SYNCED,
                blobSha = newSha,
                repoPath = targetPath,
                slugAutoDerive = false,
            )
            postRepository.savePost(published)
            return PublishResult(post = published)
        }

        return PublishResult(
            error = "Push failed after SHA refresh (${retryResponse.code()}). Remote may have unexpected changes.",
            staleSha = true,
        )
    }

    fun isOnline(): Boolean = networkMonitor.checkConnectivity()

    private fun encodeContent(content: String): String =
        Base64.getEncoder().encodeToString(content.toByteArray())

    private fun decodeContent(response: ContentResponse): String? {
        val content = response.content ?: return null
        val cleaned = content.replace("\n", "")
        return try {
            String(Base64.getDecoder().decode(cleaned))
        } catch (e: Exception) {
            null
        }
    }
}
