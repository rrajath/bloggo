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
class EditorViewModelTest {

    private val postRepository = mockk<PostRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val gitHubRepository = mockk<GitHubRepository>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testSettings = Settings(
        frontMatterTemplate = "date: {date}\ntags: []\nsummary: \"\"",
    )

    private val existingPost = PostDraft(
        localId = "post-1",
        title = "Existing Post",
        slug = "existing-post",
        draft = true,
        syncState = SyncState.LOCAL_ONLY,
        rawFrontMatter = "date: 2026-06-21\ntags: [test]",
        body = "Original body.",
        slugAutoDerive = true,
    )

    private val syncedPost = PostDraft(
        localId = "post-2",
        title = "Synced Post",
        slug = "synced-post",
        draft = false,
        syncState = SyncState.SYNCED,
        rawFrontMatter = "date: 2026-06-15\ntags: [published]",
        body = "Published body.",
        slugAutoDerive = false,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.settings } returns flowOf(testSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPost_newPost_seedsFrontMatter() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.isNew).isTrue()
            assertThat(state.title).isEmpty()
            assertThat(state.body).isEmpty()
            assertThat(state.rawFrontMatter).contains("date:")
            assertThat(state.draft).isTrue()
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadPost_newPost_dateTokenReplaced() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.rawFrontMatter).doesNotContain("{date}")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadPost_existingPost_loadsAllFields() = runTest {
        coEvery { postRepository.getPost("post-1") } returns existingPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-1")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.isNew).isFalse()
            assertThat(state.title).isEqualTo("Existing Post")
            assertThat(state.slug).isEqualTo("existing-post")
            assertThat(state.draft).isTrue()
            assertThat(state.body).isEqualTo("Original body.")
            assertThat(state.rawFrontMatter).contains("tags: [test]")
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadPost_syncedPost_slugIsFrozen() = runTest {
        coEvery { postRepository.getPost("post-2") } returns syncedPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-2")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.slugFrozen).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadPost_localOnly_slugNotFrozen() = runTest {
        coEvery { postRepository.getPost("post-1") } returns existingPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-1")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.slugFrozen).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTitleChange_setsDirty() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("New Title")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.title).isEqualTo("New Title")
            assertThat(state.dirty).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTitleFocusLost_newPostDerivesSlug() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Hello World")
        vm.onTitleFocusLost()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.slug).isEqualTo("hello-world")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTitleFocusLost_syncedPostDoesNotDerive() = runTest {
        coEvery { postRepository.getPost("post-2") } returns syncedPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-2")
        advanceUntilIdle()

        vm.onTitleChange("New Title")
        vm.onTitleFocusLost()
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.slug).isEqualTo("synced-post")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSlugChange_freezesSlug() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onSlugChange("custom-slug")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.slug).isEqualTo("custom-slug")
            assertThat(state.slugFrozen).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onBodyChange_setsDirtyAndWordCount() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onBodyChange("one two three four")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.body).isEqualTo("one two three four")
            assertThat(state.dirty).isTrue()
            assertThat(state.wordCount).isEqualTo(4)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun wordCount_emptyBody() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.wordCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun wordCount_whitespaceOnly() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onBodyChange("   \n  \n  ")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.wordCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFrontMatter() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isFrontMatterOpen).isFalse()
        vm.toggleFrontMatter()
        assertThat(vm.uiState.value.isFrontMatterOpen).isTrue()
        vm.toggleFrontMatter()
        assertThat(vm.uiState.value.isFrontMatterOpen).isFalse()
    }

    @Test
    fun togglePreview() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        assertThat(vm.uiState.value.isPreview).isFalse()
        vm.togglePreview()
        assertThat(vm.uiState.value.isPreview).isTrue()
    }

    @Test
    fun setDraft_changesDraftFlag() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.setDraft(false)
        assertThat(vm.uiState.value.draft).isFalse()
        vm.setDraft(true)
        assertThat(vm.uiState.value.draft).isTrue()
    }

    @Test
    fun canPublish_falseForEmptyTitle() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        assertThat(vm.canPublish()).isFalse()
    }

    @Test
    fun canPublish_trueForNonEmptyTitle() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("My Post")
        assertThat(vm.canPublish()).isTrue()
    }

    @Test
    fun saveLocal_savesPostToRepository() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Test Post")
        vm.onBodyChange("Test body.")
        vm.saveLocal()
        advanceUntilIdle()

        coVerify { postRepository.savePost(any()) }
    }

    @Test
    fun saveLocal_clearsDirty() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Test")
        vm.onBodyChange("Body")
        assertThat(vm.uiState.value.dirty).isTrue()
        vm.saveLocal()
        advanceUntilIdle()
        assertThat(vm.uiState.value.dirty).isFalse()
    }

    @Test
    fun saveLocal_deletesAutosave() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Test")
        vm.saveLocal()
        advanceUntilIdle()

        coVerify { postRepository.deleteAutosave(any()) }
    }

    @Test
    fun getPostForPublish_returnsPostWithCurrentFields() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Publish Me")
        vm.onBodyChange("Body text.")
        vm.setDraft(false)

        val post = vm.getPostForPublish()
        assertThat(post.title).isEqualTo("Publish Me")
        assertThat(post.body).isEqualTo("Body text.")
        assertThat(post.draft).isFalse()
    }

    @Test
    fun getPostForPublish_emptySlugDerivesFromTitle() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Hello World")
        val post = vm.getPostForPublish()
        assertThat(post.slug).isEqualTo("hello-world")
    }

    @Test
    fun displaySlug_emptySlugShowsDerived() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("My Title")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.displaySlug).isEqualTo("my-title")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun displaySlug_nonEmptySlugShowsActual() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("My Title")
        vm.onSlugChange("custom-slug")
        vm.uiState.test {
            val state = awaitItem()
            assertThat(state.displaySlug).isEqualTo("custom-slug")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun discardChanges_deletesAutosaveAndNavigatesBack() = runTest {
        coEvery { postRepository.getPost("post-1") } returns existingPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-1")
        advanceUntilIdle()

        vm.discardChanges()
        advanceUntilIdle()

        coVerify { postRepository.deleteAutosave("post-1") }
    }

    @Test
    fun onPublishSuccess_savesPostAndClearsAutosave() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        val published = existingPost.copy(syncState = SyncState.SYNCED)
        vm.onPublishSuccess(published)
        advanceUntilIdle()

        coVerify { postRepository.savePost(published) }
        coVerify { postRepository.deleteAutosave(any()) }
    }

    @Test
    fun autosave_savesWhenDirty() = runTest {
        coEvery { postRepository.getPost(any()) } returns null
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost(null)
        advanceUntilIdle()

        vm.onTitleChange("Changed")
        vm.onBodyChange("Body")
        vm.autosave()
        advanceUntilIdle()

        coVerify { postRepository.saveAutosave(any()) }
    }

    @Test
    fun autosave_doesNotSaveWhenNotDirty() = runTest {
        coEvery { postRepository.getPost("post-1") } returns existingPost
        val vm = EditorViewModel(postRepository, settingsRepository, gitHubRepository)
        vm.loadPost("post-1")
        advanceUntilIdle()

        vm.autosave()
        advanceUntilIdle()

        coVerify(exactly = 0) { postRepository.saveAutosave(any()) }
    }
}
