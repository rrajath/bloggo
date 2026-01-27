package com.rrajath.hugowriter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.hugowriter.api.GitHubService
import com.rrajath.hugowriter.data.Post
import com.rrajath.hugowriter.repository.PostRepository
import com.rrajath.hugowriter.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PostListViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository = PostRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val githubService = GitHubService()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<Result<Int>?>(null)
    val syncResult: StateFlow<Result<Int>?> = _syncResult.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val posts = if (_searchQuery.value.isNotBlank()) {
                    postRepository.searchPosts(_searchQuery.value)
                } else {
                    postRepository.getAllPosts()
                }
                _posts.value = posts
            } catch (e: Exception) {
                // Handle error
                _posts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        loadPosts()
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val post = postRepository.getPostById(postId)

                if (post != null && post.isPublished) {
                    // Delete from GitHub first
                    val config = settingsRepository.gitHubConfig.first()
                    val result = githubService.deletePost(post, config)

                    if (result.isSuccess) {
                        // Delete from local storage
                        postRepository.deletePost(postId)
                        _deleteResult.value = Result.success(Unit)
                    } else {
                        _deleteResult.value = result
                    }
                } else {
                    // Just delete from local storage
                    postRepository.deletePost(postId)
                    _deleteResult.value = Result.success(Unit)
                }

                loadPosts()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun syncFromGitHub() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val config = settingsRepository.gitHubConfig.first()
                val result = githubService.fetchAllPosts(config)

                if (result.isSuccess) {
                    val posts = result.getOrNull() ?: emptyList()
                    // Save all fetched posts locally, updating existing ones if they exist
                    posts.forEach { fetchedPost ->
                        // Check if a post with the same title already exists
                        val existingPost = postRepository.getPostByTitle(fetchedPost.title)
                        if (existingPost != null) {
                            // Update existing post, keeping the same ID
                            val updatedPost = fetchedPost.copy(
                                id = existingPost.id,
                                createdAt = existingPost.createdAt
                            )
                            postRepository.savePost(updatedPost)
                        } else {
                            // Save as new post
                            postRepository.savePost(fetchedPost)
                        }
                    }
                    _syncResult.value = Result.success(posts.size)
                    loadPosts()
                } else {
                    _syncResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                _syncResult.value = Result.failure(e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }
}
