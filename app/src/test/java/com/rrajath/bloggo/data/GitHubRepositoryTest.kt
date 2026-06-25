package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.network.ContentRequest
import com.rrajath.bloggo.data.network.ContentResponse
import com.rrajath.bloggo.data.network.ContentUpdateResponse
import com.rrajath.bloggo.data.network.GitHubService
import com.rrajath.bloggo.data.network.TreeItem
import com.rrajath.bloggo.data.network.TreeResponse
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import com.rrajath.bloggo.ui.theme.Accent
import com.rrajath.bloggo.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Base64

class GitHubRepositoryTest {

    private val gitHubService = mockk<GitHubService>(relaxed = true)
    private val postRepository = mockk<PostRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private lateinit var repository: GitHubRepository

    private val testSettings = Settings(
        githubPat = "ghp_test",
        repository = "me/blog",
        branch = "main",
        contentPath = "content/posts",
    )

    private fun encode(content: String): String =
        Base64.getEncoder().encodeToString(content.toByteArray())

    private fun sampleFile(title: String, slug: String, draft: Boolean, sha: String): String {
        val content = """
            ---
            title: "$title"
            slug: "$slug"
            draft: $draft
            date: 2026-06-21
            ---

            Body text for $title.
        """.trimIndent()
        return encode(content)
    }

    @Before
    fun setup() {
        repository = GitHubRepository(gitHubService, postRepository, settingsRepository, networkMonitor)
        coEvery { settingsRepository.settings } returns flowOf(testSettings)
        coEvery { networkMonitor.checkConnectivity() } returns true
        coEvery { postRepository.observeAllPosts() } returns flowOf(emptyList())
        coEvery { postRepository.count() } returns 0
    }

    // ── refresh() ────────────────────────────────────────────────────────────

    @Test
    fun refresh_offline_returnsError() = runTest {
        coEvery { networkMonitor.checkConnectivity() } returns false

        val result = repository.refresh()

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Offline")
    }

    @Test
    fun refresh_repoNotConfigured_returnsError() = runTest {
        coEvery { settingsRepository.settings } returns flowOf(testSettings.copy(repository = ""))

        val result = repository.refresh()

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("not configured")
    }

    @Test
    fun refresh_successful_fetchesAndSavesPosts() = runTest {
        val treeResponse = Response.success(
            TreeResponse(
                tree = listOf(
                    TreeItem(path = "content/posts/post-1.md", mode = "100644", type = "blob", sha = "sha-1"),
                    TreeItem(path = "content/posts/post-2.md", mode = "100644", type = "blob", sha = "sha-2"),
                ),
            ),
        )
        coEvery { gitHubService.getTree("me", "blog", "main") } returns treeResponse

        val content1 = ContentResponse(
            content = sampleFile("First Post", "first-post", false, "sha-1"),
            encoding = "base64",
            sha = "sha-1",
            path = "content/posts/post-1.md",
            name = "post-1.md",
        )
        val content2 = ContentResponse(
            content = sampleFile("Second Post", "second-post", true, "sha-2"),
            encoding = "base64",
            sha = "sha-2",
            path = "content/posts/post-2.md",
            name = "post-2.md",
        )
        coEvery { gitHubService.getContent("me", "blog", "content/posts/post-1.md", "main") } returns Response.success(content1)
        coEvery { gitHubService.getContent("me", "blog", "content/posts/post-2.md", "main") } returns Response.success(content2)

        val result = repository.refresh()

        assertThat(result.error).isNull()
        assertThat(result.remotePosts).isEqualTo(2)
        coVerify(atLeast = 2) { postRepository.savePost(any()) }
    }

    @Test
    fun refresh_filtersNonMdFiles() = runTest {
        val treeResponse = Response.success(
            TreeResponse(
                tree = listOf(
                    TreeItem(path = "content/posts/post-1.md", mode = "100644", type = "blob", sha = "sha-1"),
                    TreeItem(path = "content/posts/image.png", mode = "100644", type = "blob", sha = "sha-img"),
                    TreeItem(path = "README.md", mode = "100644", type = "blob", sha = "sha-readme"),
                ),
            ),
        )
        coEvery { gitHubService.getTree(any(), any(), any()) } returns treeResponse
        coEvery { gitHubService.getContent(any(), any(), any(), any()) } returns Response.success(
            ContentResponse(
                content = sampleFile("Post", "post", true, "sha-1"),
                encoding = "base64",
                sha = "sha-1",
                path = "content/posts/post-1.md",
                name = "post-1.md",
            ),
        )

        val result = repository.refresh()

        assertThat(result.remotePosts).isEqualTo(1)
    }

    @Test
    fun refresh_treeApiFailure_returnsError() = runTest {
        coEvery { gitHubService.getTree(any(), any(), any()) } returns Response.error(404, mockk(relaxed = true))

        val result = repository.refresh()

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("404")
    }

    @Test
    fun refresh_networkException_returnsError() = runTest {
        coEvery { gitHubService.getTree(any(), any(), any()) } throws RuntimeException("Connection refused")

        val result = repository.refresh()

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Connection refused")
    }

    @Test
    fun refresh_preservesLocalOnlyPosts() = runTest {
        val localOnly = PostDraft(
            localId = "local-1",
            syncState = SyncState.LOCAL_ONLY,
            title = "Local Only",
            slug = "local-only",
        )
        coEvery { postRepository.observeAllPosts() } returns flowOf(listOf(localOnly))

        val treeResponse = Response.success(TreeResponse(tree = emptyList()))
        coEvery { gitHubService.getTree(any(), any(), any()) } returns treeResponse

        repository.refresh()

        coVerify(exactly = 0) { postRepository.deletePost("local-1") }
    }

    @Test
    fun refresh_preservesSyncedModifiedPosts() = runTest {
        val syncedModified = PostDraft(
            localId = "local-1",
            syncState = SyncState.SYNCED_MODIFIED,
            title = "Modified",
            slug = "modified",
            repoPath = "content/posts/modified.md",
            blobSha = "sha-1",
        )
        coEvery { postRepository.observeAllPosts() } returns flowOf(listOf(syncedModified))

        val treeResponse = Response.success(
            TreeResponse(
                tree = listOf(
                    TreeItem("content/posts/modified.md", "100644", "blob", "sha-remote"),
                ),
            ),
        )
        coEvery { gitHubService.getTree(any(), any(), any()) } returns treeResponse
        coEvery { gitHubService.getContent(any(), any(), any(), any()) } returns Response.success(
            ContentResponse(
                content = sampleFile("Modified", "modified", true, "sha-remote"),
                encoding = "base64",
                sha = "sha-remote",
                path = "content/posts/modified.md",
                name = "modified.md",
            ),
        )

        repository.refresh()

        coVerify(exactly = 0) { postRepository.savePost(match { it.localId == "local-1" && it.syncState == SyncState.SYNCED }) }
    }

    @Test
    fun refresh_deletesSyncedPostsRemovedRemotely() = runTest {
        val syncedPost = PostDraft(
            localId = "local-1",
            syncState = SyncState.SYNCED,
            title = "Deleted",
            slug = "deleted",
            repoPath = "content/posts/deleted.md",
            blobSha = "sha-1",
        )
        coEvery { postRepository.observeAllPosts() } returns flowOf(listOf(syncedPost))

        val treeResponse = Response.success(TreeResponse(tree = emptyList()))
        coEvery { gitHubService.getTree(any(), any(), any()) } returns treeResponse

        repository.refresh()

        coVerify { postRepository.deletePost("local-1") }
    }

    // ── publish() ────────────────────────────────────────────────────────────

    @Test
    fun publish_offline_returnsError() = runTest {
        coEvery { networkMonitor.checkConnectivity() } returns false

        val post = PostDraft(localId = "1", title = "Test", slug = "test")
        val result = repository.publish(post)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Offline")
    }

    @Test
    fun publish_newPost_successful() = runTest {
        val post = PostDraft(localId = "1", title = "New Post", slug = "new-post", draft = false)
        val updateResponse = ContentUpdateResponse(
            content = ContentResponse(
                content = null,
                encoding = null,
                sha = "new-sha",
                path = "content/posts/new-post.md",
                name = "new-post.md",
            ),
            commit = null,
        )
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(updateResponse)

        val result = repository.publish(post)

        assertThat(result.error).isNull()
        assertThat(result.post).isNotNull()
        assertThat(result.post!!.syncState).isEqualTo(SyncState.SYNCED)
        assertThat(result.post!!.blobSha).isEqualTo("new-sha")
        assertThat(result.post!!.repoPath).isEqualTo("content/posts/new-post.md")
    }

    @Test
    fun publish_updatePost_includesSha() = runTest {
        val post = PostDraft(
            localId = "1",
            title = "Updated Post",
            slug = "updated-post",
            draft = false,
            syncState = SyncState.SYNCED,
            blobSha = "old-sha",
            repoPath = "content/posts/updated-post.md",
        )
        val updateResponse = ContentUpdateResponse(
            content = ContentResponse(
                content = null,
                encoding = null,
                sha = "new-sha",
                path = "content/posts/updated-post.md",
                name = "updated-post.md",
            ),
            commit = null,
        )
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(updateResponse)

        val result = repository.publish(post)

        assertThat(result.error).isNull()
        assertThat(result.post!!.blobSha).isEqualTo("new-sha")

        coVerify {
            gitHubService.putContent("me", "blog", "content/posts/updated-post.md", match { it.sha == "old-sha" })
        }
    }

    @Test
    fun publish_newPost_omitsSha() = runTest {
        val post = PostDraft(localId = "1", title = "New", slug = "new", draft = false, blobSha = null)
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "sha", "content/posts/new.md", "new.md"),
                commit = null,
            ),
        )

        repository.publish(post)

        coVerify {
            gitHubService.putContent(any(), any(), any(), match { it.sha == null })
        }
    }

    @Test
    fun publish_commitMessage_isNewPostTitle() = runTest {
        val post = PostDraft(localId = "1", title = "My Great Post", slug = "my-great-post")
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "sha", "p", "p"),
                commit = null,
            ),
        )

        repository.publish(post)

        coVerify {
            gitHubService.putContent(any(), any(), any(), match { it.message == "New post: My Great Post" })
        }
    }

    @Test
    fun publish_emptyTitle_commitMessageUsesUntitled() = runTest {
        val post = PostDraft(localId = "1", title = "", slug = "post")
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "sha", "p", "p"),
                commit = null,
            ),
        )

        repository.publish(post)

        coVerify {
            gitHubService.putContent(any(), any(), any(), match { it.message == "New post: Untitled" })
        }
    }

    @Test
    fun publish_staleSha_refetchesAndRetries() = runTest {
        val post = PostDraft(
            localId = "1",
            title = "Test",
            slug = "test",
            blobSha = "stale-sha",
            repoPath = "content/posts/test.md",
        )

        val staleResponse: Response<ContentUpdateResponse> = Response.error(422, mockk(relaxed = true))
        val retryResponse = Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "new-sha", "content/posts/test.md", "test.md"),
                commit = null,
            ),
        )

        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returnsMany listOf(staleResponse, retryResponse)

        val refetchResponse = Response.success(
            ContentResponse(null, null, "current-sha", "content/posts/test.md", "test.md"),
        )
        coEvery { gitHubService.getContent("me", "blog", "content/posts/test.md", "main") } returns refetchResponse

        val result = repository.publish(post)

        assertThat(result.error).isNull()
        assertThat(result.post).isNotNull()
        assertThat(result.post!!.blobSha).isEqualTo("new-sha")
    }

    @Test
    fun publish_apiError_returnsError() = runTest {
        val post = PostDraft(localId = "1", title = "Test", slug = "test")
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.error(403, mockk(relaxed = true))

        val result = repository.publish(post)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("403")
    }

    @Test
    fun publish_networkException_returnsError() = runTest {
        val post = PostDraft(localId = "1", title = "Test", slug = "test")
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } throws RuntimeException("Timeout")

        val result = repository.publish(post)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Timeout")
    }

    @Test
    fun publish_repoNotConfigured_returnsError() = runTest {
        coEvery { settingsRepository.settings } returns flowOf(testSettings.copy(repository = ""))

        val post = PostDraft(localId = "1", title = "Test", slug = "test")
        val result = repository.publish(post)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("not configured")
    }

    @Test
    fun publish_success_savesPostToRepository() = runTest {
        val post = PostDraft(localId = "1", title = "Test", slug = "test", draft = false)
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "new-sha", "content/posts/test.md", "test.md"),
                commit = null,
            ),
        )

        repository.publish(post)

        coVerify { postRepository.savePost(any()) }
    }

    @Test
    fun publish_success_freezesSlugAutoDerive() = runTest {
        val post = PostDraft(localId = "1", title = "Test", slug = "test", draft = false, slugAutoDerive = true)
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "sha", "content/posts/test.md", "test.md"),
                commit = null,
            ),
        )

        val result = repository.publish(post)

        assertThat(result.post!!.slugAutoDerive).isFalse()
    }
}
