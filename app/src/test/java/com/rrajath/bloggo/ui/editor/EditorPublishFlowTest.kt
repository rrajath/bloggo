package com.rrajath.bloggo.ui.editor

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.GitHubRepository
import com.rrajath.bloggo.data.PostRepository
import com.rrajath.bloggo.data.Settings
import com.rrajath.bloggo.data.SettingsRepository
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
class EditorPublishFlowTest {

    private val postRepository = mockk<PostRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val gitHubRepository = mockk<GitHubRepository>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testSettings = Settings(
        frontMatterTemplate = "date: {date}\ntags: []",
        contentPath = "content/posts",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.settings } returns flowOf(testSettings)
        coEvery { postRepository.getPost(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EditorViewModel {
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        return vm
    }

    @Test
    fun startPublish_draftTrue_showsDraftFlipDialog() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")

        vm.startPublish()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showDraftFlip).isTrue()
            assertThat(state.showPushConfirm).isFalse()
            assertThat(state.post).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startPublish_draftFalse_showsPushConfirm() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)

        vm.startPublish()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showDraftFlip).isFalse()
            assertThat(state.showPushConfirm).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startPublish_emptyTitle_doesNothing() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.startPublish()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showDraftFlip).isFalse()
            assertThat(state.showPushConfirm).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun keepDraft_closesAllDialogs() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test")
        vm.startPublish()
        advanceUntilIdle()

        vm.keepDraft()
        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showDraftFlip).isFalse()
            assertThat(state.showPushConfirm).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmDraftFlip_setsDraftFalseAndShowsPushConfirm() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test")
        vm.startPublish()
        advanceUntilIdle()

        vm.confirmDraftFlip()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showDraftFlip).isFalse()
            assertThat(state.showPushConfirm).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.uiState.value.draft).isFalse()
    }

    @Test
    fun cancelPublish_closesAllDialogs() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        vm.cancelPublish()
        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showPushConfirm).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPush_callsGitHubRepositoryPublish() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        val publishedPost = PostDraft(
            localId = "1",
            title = "Test Post",
            slug = "test-post",
            draft = false,
            syncState = SyncState.SYNCED,
        )
        coEvery { gitHubRepository.publish(any()) } returns GitHubRepository.PublishResult(post = publishedPost)

        vm.confirmPush()
        advanceUntilIdle()

        coVerify { gitHubRepository.publish(any()) }
    }

    @Test
    fun confirmPush_success_savesPostAndClearsAutosave() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        val publishedPost = PostDraft(
            localId = "1",
            title = "Test Post",
            syncState = SyncState.SYNCED,
        )
        coEvery { gitHubRepository.publish(any()) } returns GitHubRepository.PublishResult(post = publishedPost)

        vm.confirmPush()
        advanceUntilIdle()

        coVerify { postRepository.savePost(publishedPost) }
        coVerify { postRepository.deleteAutosave(any()) }
    }

    @Test
    fun confirmPush_success_clearsPublishState() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        coEvery { gitHubRepository.publish(any()) } returns GitHubRepository.PublishResult(
            post = PostDraft(localId = "1", title = "Test Post", syncState = SyncState.SYNCED),
        )

        vm.confirmPush()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.showPushConfirm).isFalse()
            assertThat(state.isPushing).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPush_error_showsErrorAndStopsPushing() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        coEvery { gitHubRepository.publish(any()) } returns GitHubRepository.PublishResult(error = "Network error")

        vm.confirmPush()
        advanceUntilIdle()

        vm.publishState.test {
            val state = awaitItem()
            assertThat(state.isPushing).isFalse()
            assertThat(state.error).isEqualTo("Network error")
            assertThat(state.showPushConfirm).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPush_whileAlreadyPushing_doesNothing() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        coEvery { gitHubRepository.publish(any()) } returns GitHubRepository.PublishResult(
            post = PostDraft(localId = "1", syncState = SyncState.SYNCED),
        )

        vm.confirmPush()
        vm.confirmPush()
        advanceUntilIdle()

        coVerify(exactly = 1) { gitHubRepository.publish(any()) }
    }

    @Test
    fun getPushConfirmDataAsync_returnsDataWithContentPath() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onTitleChange("Test Post")
        vm.setDraft(false)
        vm.startPublish()
        advanceUntilIdle()

        val data = vm.getPushConfirmDataAsync()
        assertThat(data).isNotNull()
        assertThat(data!!.targetPath).startsWith("content/posts/")
    }
}
