package com.rrajath.bloggo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.bloggo.data.GitHubRepository
import com.rrajath.bloggo.data.NetworkMonitor
import com.rrajath.bloggo.data.PostRepository
import com.rrajath.bloggo.domain.HomeSection
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import com.rrajath.bloggo.domain.section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val gitHubRepository: GitHubRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _refreshing = MutableStateFlow(false)
    private val _banner = MutableStateFlow<BannerUi?>(null)

    private val allPosts = postRepository.observeAllPosts()

    val uiState: StateFlow<HomeUiState> =
        combine(
            allPosts,
            _searchQuery,
            _refreshing,
            _banner,
        ) { posts, query, refreshing, banner ->
            val filtered = filterPosts(posts, query)
            val (draft, published) = partitionPosts(filtered)
            HomeUiState(
                draftPosts = draft,
                publishedPosts = published,
                searchQuery = query,
                refreshing = refreshing,
                banner = banner,
                isEmpty = posts.isEmpty() && !refreshing,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = HomeUiState(),
        )

    val banner: StateFlow<BannerUi?> = _banner.asStateFlow()

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val result = gitHubRepository.refresh()
            _refreshing.value = false

            _banner.value = when {
                result.error != null && result.error.contains("Offline") -> {
                    BannerUi(BannerType.NEUTRAL, "Offline — cached posts available. Publish is disabled.", "Dismiss")
                }
                result.error != null -> {
                    BannerUi(BannerType.WARN, "Couldn't refresh from GitHub. Showing cached posts.", "Retry")
                }
                else -> {
                    BannerUi(BannerType.SUCCESS, "Up to date with GitHub.", "Dismiss")
                }
            }
        }
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun dismissBanner() {
        _banner.value = null
    }

    fun deletePost(localId: String) {
        viewModelScope.launch {
            postRepository.deletePost(localId)
            _banner.value = BannerUi(BannerType.NEUTRAL, "Draft deleted.", "Dismiss")
        }
    }

    private fun filterPosts(posts: List<PostDraft>, query: String): List<PostDraft> {
        if (query.isBlank()) return posts
        val q = query.trim().lowercase()
        return posts.filter { post ->
            post.title.lowercase().contains(q) ||
                post.body.lowercase().contains(q) ||
                post.slug.lowercase().contains(q)
        }
    }

    private fun partitionPosts(posts: List<PostDraft>): Pair<List<PostRow>, List<PostRow>> {
        val draft = posts.filter { it.section() == HomeSection.DRAFT }
            .sortedByDescending { it.updatedAt }
            .map { it.toPostRow() }
        val published = posts.filter { it.section() == HomeSection.PUBLISHED }
            .sortedByDescending { it.updatedAt }
            .map { it.toPostRow() }
        return draft to published
    }

    private fun PostDraft.toPostRow(): PostRow = PostRow(
        post = this,
        metaText = buildMetaText(this),
        isLocal = syncState == SyncState.LOCAL_ONLY,
        isSynced = syncState != SyncState.LOCAL_ONLY,
        isEdited = syncState == SyncState.SYNCED_MODIFIED,
    )
}
