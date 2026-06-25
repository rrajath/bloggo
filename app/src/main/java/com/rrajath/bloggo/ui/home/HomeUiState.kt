package com.rrajath.bloggo.ui.home

import com.rrajath.bloggo.domain.PostDraft

data class HomeUiState(
    val draftPosts: List<PostRow> = emptyList(),
    val publishedPosts: List<PostRow> = emptyList(),
    val searchQuery: String = "",
    val refreshing: Boolean = false,
    val banner: BannerUi? = null,
    val isEmpty: Boolean = false,
)

data class PostRow(
    val post: PostDraft,
    val metaText: String,
    val isLocal: Boolean,
    val isSynced: Boolean,
    val isEdited: Boolean,
)

data class BannerUi(
    val type: BannerType,
    val text: String,
    val actionLabel: String,
)

enum class BannerType { SUCCESS, WARN, NEUTRAL }
