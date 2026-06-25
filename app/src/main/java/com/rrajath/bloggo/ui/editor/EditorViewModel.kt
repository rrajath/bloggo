package com.rrajath.bloggo.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.bloggo.data.GitHubRepository
import com.rrajath.bloggo.data.PostRepository
import com.rrajath.bloggo.data.SettingsRepository
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import com.rrajath.bloggo.domain.onSlugManuallyEdited
import com.rrajath.bloggo.domain.onTitleCommitted
import com.rrajath.bloggo.domain.seedFrontMatter
import com.rrajath.bloggo.domain.slugify
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository,
    private val gitHubRepository: GitHubRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _publishState = MutableStateFlow(PublishDialogState())
    val publishState: StateFlow<PublishDialogState> = _publishState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var postDraft: PostDraft? = null

    fun loadPost(postId: String?) {
        viewModelScope.launch {
            if (postId != null) {
                val post = postRepository.getPost(postId)
                if (post != null) {
                    postDraft = post
                    _uiState.value = EditorUiState(
                        title = post.title,
                        slug = post.slug,
                        slugFrozen = post.isSynced || !post.slugAutoDerive,
                        draft = post.draft,
                        rawFrontMatter = post.rawFrontMatter,
                        body = post.body,
                        isNew = false,
                        dirty = false,
                        wordCount = countWords(post.body),
                        localId = post.localId,
                        syncState = post.syncState,
                        isLoading = false,
                    )
                    return@launch
                }
            }

            val settings = settingsRepository.settings.first()
            val seededFm = seedFrontMatter(settings.frontMatterTemplate)
            val newDraft = PostDraft(
                localId = UUID.randomUUID().toString(),
                rawFrontMatter = seededFm,
            )
            postDraft = newDraft
            _uiState.value = EditorUiState(
                rawFrontMatter = seededFm,
                isNew = true,
                isLoading = false,
                localId = newDraft.localId,
            )
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, dirty = true, wordCount = countWords(it.body)) }
    }

    fun onTitleFocusLost() {
        val current = _uiState.value
        if (current.isNew && !current.slugFrozen) {
            val derived = slugify(current.title)
            _uiState.update { it.copy(slug = derived) }
            postDraft = postDraft?.copy(slug = derived, title = current.title)
        }
    }

    fun onSlugChange(newSlug: String) {
        _uiState.update { it.copy(slug = newSlug, slugFrozen = true, dirty = true) }
        postDraft = postDraft?.onSlugManuallyEdited(newSlug)
    }

    fun onBodyChange(newBody: String) {
        _uiState.update { it.copy(body = newBody, dirty = true, wordCount = countWords(newBody)) }
    }

    fun onFrontMatterChange(newFm: String) {
        _uiState.update { it.copy(rawFrontMatter = newFm, dirty = true) }
    }

    fun toggleFrontMatter() {
        _uiState.update { it.copy(isFrontMatterOpen = !it.isFrontMatterOpen) }
    }

    fun togglePreview() {
        _uiState.update { it.copy(isPreview = !it.isPreview) }
    }

    fun setDraft(draft: Boolean) {
        _uiState.update { it.copy(draft = draft, dirty = true) }
    }

    fun wrapSelection(before: String, after: String, placeholder: String) {
        // This is handled in the composable where we have TextFieldValue access
    }

    fun applyHeading(level: Int) {
        // This is handled in the composable where we have TextFieldValue access
    }

    fun saveLocal() {
        viewModelScope.launch {
            val current = _uiState.value
            val draft = buildPostDraft(current)
            postRepository.savePost(draft)
            postRepository.deleteAutosave(draft.localId)
            _uiState.update { it.copy(dirty = false) }
            _events.tryEmit(EditorEvent.NavigateBack("Saved locally."))
        }
    }

    fun canPublish(): Boolean = _uiState.value.title.isNotBlank()

    fun getPostForPublish(): PostDraft = buildPostDraft(_uiState.value)

    fun startPublish() {
        val current = _uiState.value
        if (!canPublish()) return
        val post = getPostForPublish()
        _publishState.value = PublishDialogState(post = post)
        if (current.draft) {
            _publishState.value = _publishState.value.copy(showDraftFlip = true)
        } else {
            _publishState.value = _publishState.value.copy(showPushConfirm = true)
        }
    }

    fun keepDraft() {
        _publishState.value = PublishDialogState()
    }

    fun confirmDraftFlip() {
        setDraft(false)
        _publishState.value = _publishState.value.copy(
            showDraftFlip = false,
            showPushConfirm = true,
            post = getPostForPublish(),
        )
    }

    fun cancelPublish() {
        _publishState.value = PublishDialogState()
    }

    fun confirmPush() {
        val post = _publishState.value.post ?: return
        if (_publishState.value.isPushing) return

        _publishState.value = _publishState.value.copy(isPushing = true, error = null)

        viewModelScope.launch {
            val result = gitHubRepository.publish(post)
            if (result.post != null) {
                postRepository.savePost(result.post)
                postRepository.deleteAutosave(result.post.localId)
                _publishState.value = PublishDialogState()
                _events.tryEmit(EditorEvent.NavigateBack("Published to GitHub."))
            } else {
                _publishState.value = _publishState.value.copy(
                    isPushing = false,
                    error = result.error ?: "Unknown error",
                )
            }
        }
    }

    fun getPushConfirmData(): PushConfirmData? {
        val post = _publishState.value.post ?: return null
        val settings = settingsRepository.settings
        var contentPath = "content/posts"
        viewModelScope.launch {
            contentPath = settings.first().contentPath
        }
        return buildPushConfirmData(post, contentPath)
    }

    suspend fun getPushConfirmDataAsync(): PushConfirmData? {
        val post = _publishState.value.post ?: return null
        val contentPath = settingsRepository.settings.first().contentPath
        return buildPushConfirmData(post, contentPath)
    }

    fun discardChanges() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.localId != null) {
                postRepository.deleteAutosave(current.localId)
            }
            _events.tryEmit(EditorEvent.NavigateBack(null))
        }
    }

    fun onPublishSuccess(post: PostDraft) {
        viewModelScope.launch {
            postRepository.savePost(post)
            postRepository.deleteAutosave(post.localId)
            _events.tryEmit(EditorEvent.NavigateBack("Published to GitHub."))
        }
    }

    fun autosave() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current.dirty && current.localId != null) {
                val draft = buildPostDraft(current)
                postRepository.saveAutosave(draft)
            }
        }
    }

    private fun buildPostDraft(state: EditorUiState): PostDraft {
        val base = postDraft ?: PostDraft(localId = UUID.randomUUID().toString())
        return base.copy(
            title = state.title,
            slug = state.slug.ifBlank { slugify(state.title) },
            draft = state.draft,
            slugAutoDerive = !state.slugFrozen,
            rawFrontMatter = state.rawFrontMatter,
            body = state.body,
            syncState = state.syncState,
        )
    }

    private fun countWords(text: String): Int {
        val trimmed = text.trim()
        return if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
    }
}

private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
