package com.rrajath.bloggo.ui.editor

import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState

data class EditorUiState(
    val title: String = "",
    val slug: String = "",
    val slugFrozen: Boolean = false,
    val draft: Boolean = true,
    val rawFrontMatter: String = "",
    val body: String = "",
    val isPreview: Boolean = false,
    val isFrontMatterOpen: Boolean = false,
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val wordCount: Int = 0,
    val localId: String? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val isLoading: Boolean = true,
) {
    val displaySlug: String
        get() = slug.ifBlank { com.rrajath.bloggo.domain.slugify(title) }
}

sealed class EditorEvent {
    data class ShowMessage(val text: String) : EditorEvent()
    data class NavigateBack(val message: String?) : EditorEvent()
}
