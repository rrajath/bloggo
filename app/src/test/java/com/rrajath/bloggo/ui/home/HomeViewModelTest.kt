package com.rrajath.bloggo.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.GitHubRepository
import com.rrajath.bloggo.data.NetworkMonitor
import com.rrajath.bloggo.data.PostRepository
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val postRepository = mockk<PostRepository>(relaxed = true)
    private val gitHubRepository = mockk<GitHubRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private val draftPost = PostDraft(
        localId = "d1",
        title = "Draft One",
        slug = "draft-one",
        draft = true,
        syncState = SyncState.LOCAL_ONLY,
        rawFrontMatter = "date: 2026-06-21T08:14:00+00:00",
        updatedAt = 2000L,
    )

    private val publishedPost = PostDraft(
        localId = "p1",
        title = "Published One",
        slug = "published-one",
        draft = false,
        syncState = SyncState.SYNCED,
        rawFrontMatter = "date: 2026-06-15T07:40:00+00:00",
        updatedAt = 1000L,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { postRepository.observeAllPosts() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(posts: List<PostDraft> = emptyList()): HomeViewModel {
        coEvery { postRepository.observeAllPosts() } returns flowOf(posts)
        return HomeViewModel(postRepository, gitHubRepository, networkMonitor)
    }

    @Test
    fun uiState_empty_initially() = runTest {
        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.draftPosts).isEmpty()
            assertThat(state.publishedPosts).isEmpty()
            assertThat(state.isEmpty).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_partitionsByDraftStatus() = runTest {
        val vm = createViewModel(listOf(draftPost, publishedPost))
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            assertThat(state.draftPosts[0].post.title).isEqualTo("Draft One")
            assertThat(state.publishedPosts).hasSize(1)
            assertThat(state.publishedPosts[0].post.title).isEqualTo("Published One")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_localOnly_isLocalTrueIsSyncedFalse() = runTest {
        val vm = createViewModel(listOf(draftPost))
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.draftPosts[0].isLocal).isTrue()
            assertThat(state.draftPosts[0].isSynced).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_synced_isLocalFalseIsSyncedTrue() = runTest {
        val vm = createViewModel(listOf(publishedPost))
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.publishedPosts[0].isLocal).isFalse()
            assertThat(state.publishedPosts[0].isSynced).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_syncedModified_isEditedTrue() = runTest {
        val modified = publishedPost.copy(syncState = SyncState.SYNCED_MODIFIED)
        val vm = createViewModel(listOf(modified))
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.publishedPosts[0].isEdited).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_searchFiltersByTitle() = runTest {
        val post1 = draftPost.copy(title = "Rust Ownership")
        val post2 = publishedPost.copy(title = "Docker Compose")
        val vm = createViewModel(listOf(post1, post2))
        vm.uiState.test {
            awaitItem()
            vm.setSearch("rust")
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            assertThat(state.draftPosts[0].post.title).isEqualTo("Rust Ownership")
            assertThat(state.publishedPosts).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_searchFiltersByBody() = runTest {
        val post1 = draftPost.copy(title = "T1", body = "nginx reverse proxy")
        val post2 = publishedPost.copy(title = "T2", body = "docker compose")
        val vm = createViewModel(listOf(post1, post2))
        vm.uiState.test {
            awaitItem()
            vm.setSearch("nginx")
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_searchCaseInsensitive() = runTest {
        val post = draftPost.copy(title = "Hello World")
        val vm = createViewModel(listOf(post))
        vm.uiState.test {
            awaitItem()
            vm.setSearch("HELLO")
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_clearSearchShowsAll() = runTest {
        val vm = createViewModel(listOf(draftPost, publishedPost))
        vm.uiState.test {
            awaitItem()
            vm.setSearch("xyz")
            assertThat(awaitItem().draftPosts).isEmpty()
            vm.clearSearch()
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            assertThat(state.publishedPosts).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_callsGitHubRepository() = runTest {
        val vm = createViewModel()
        coEvery { gitHubRepository.refresh() } returns GitHubRepository.RefreshResult(remotePosts = 0, mergedPosts = 0)

        vm.refresh()
        advanceUntilIdle()
        coVerify { gitHubRepository.refresh() }
    }

    @Test
    fun refresh_success_showsSuccessBanner() = runTest {
        val vm = createViewModel()
        coEvery { gitHubRepository.refresh() } returns GitHubRepository.RefreshResult(remotePosts = 2, mergedPosts = 2)

        vm.refresh()
        advanceUntilIdle()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.banner).isNotNull()
            assertThat(state.banner!!.type).isEqualTo(BannerType.SUCCESS)
            assertThat(state.banner!!.text).contains("Up to date")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_offline_showsOfflineBanner() = runTest {
        val vm = createViewModel()
        coEvery { gitHubRepository.refresh() } returns GitHubRepository.RefreshResult(error = "Offline — cannot refresh from GitHub.")

        vm.refresh()
        advanceUntilIdle()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.banner).isNotNull()
            assertThat(state.banner!!.type).isEqualTo(BannerType.NEUTRAL)
            assertThat(state.banner!!.text).contains("Offline")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_error_showsWarnBanner() = runTest {
        val vm = createViewModel()
        coEvery { gitHubRepository.refresh() } returns GitHubRepository.RefreshResult(error = "GitHub API error: 404")

        vm.refresh()
        advanceUntilIdle()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.banner).isNotNull()
            assertThat(state.banner!!.type).isEqualTo(BannerType.WARN)
            assertThat(state.banner!!.text).contains("Couldn't refresh")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dismissBanner_clearsBanner() = runTest {
        val vm = createViewModel()
        coEvery { gitHubRepository.refresh() } returns GitHubRepository.RefreshResult()

        vm.refresh()
        advanceUntilIdle()
        vm.dismissBanner()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.banner).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletePost_callsRepositoryDelete() = runTest {
        val vm = createViewModel()
        vm.deletePost("post-1")
        advanceUntilIdle()
        coVerify { postRepository.deletePost("post-1") }
    }

    @Test
    fun draftSection_containsOnlyDraftPosts() = runTest {
        val drafts = listOf(
            draftPost.copy(localId = "d1", title = "D1"),
            draftPost.copy(localId = "d2", title = "D2"),
        )
        val vm = createViewModel(drafts + publishedPost)
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(2)
            assertThat(state.publishedPosts).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun publishedSection_containsOnlyPublishedPosts() = runTest {
        val published = listOf(
            publishedPost.copy(localId = "p1", title = "P1"),
            publishedPost.copy(localId = "p2", title = "P2"),
            publishedPost.copy(localId = "p3", title = "P3"),
        )
        val vm = createViewModel(published + draftPost)
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.draftPosts).hasSize(1)
            assertThat(state.publishedPosts).hasSize(3)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
