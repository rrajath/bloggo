package com.rrajath.hugowriter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.hugowriter.api.GitHubService
import com.rrajath.hugowriter.data.AppSettings
import com.rrajath.hugowriter.data.GitHubConfig
import com.rrajath.hugowriter.data.Post
import com.rrajath.hugowriter.repository.PostRepository
import com.rrajath.hugowriter.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PostEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository = PostRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val gitHubService = GitHubService()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _publishResult = MutableStateFlow<Result<String>?>(null)
    val publishResult: StateFlow<Result<String>?> = _publishResult.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var autoSaveJob: Job? = null

    fun loadPost(postId: String?) {
        viewModelScope.launch {
            if (postId != null) {
                val post = postRepository.getPostById(postId)
                if (post != null) {
                    _post.value = post
                    _title.value = post.title
                    _content.value = post.content
                } else {
                    // Post not found, create new
                    createNewPost()
                }
            } else {
                // Create new post
                createNewPost()
            }
        }
    }

    private suspend fun createNewPost() {
        val settings = settingsRepository.appSettings.first()
        val newPost = Post(
            id = Post.generateId(),
            title = "",
            content = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _post.value = newPost
        _title.value = ""
        _content.value = ""
    }

    fun onTitleChanged(newTitle: String) {
        if (newTitle.length <= 80) {
            _title.value = newTitle
            scheduleAutoSave()
        }
    }

    fun onContentChanged(newContent: String) {
        _content.value = newContent
        scheduleAutoSave()
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // Auto-save after 2 seconds of inactivity
            savePost()
        }
    }

    /**
     * Updates the title field in the frontmatter of the content.
     * Supports both YAML (---) and TOML (+++) frontmatter formats.
     */
    private fun updateFrontmatterTitle(content: String, newTitle: String): String {
        // Extract frontmatter - support both --- (YAML) and +++ (TOML) delimiters
        val yamlFrontmatterRegex = "^(---\\n)([\\s\\S]*?)(\\n---)".toRegex()
        val tomlFrontmatterRegex = "^(\\+\\+\\+\\n)([\\s\\S]*?)(\\n\\+\\+\\+)".toRegex()

        val yamlMatch = yamlFrontmatterRegex.find(content)
        val tomlMatch = tomlFrontmatterRegex.find(content)

        return when {
            yamlMatch != null -> {
                val frontmatterStart = yamlMatch.groupValues[1]
                val frontmatterBody = yamlMatch.groupValues[2]
                val frontmatterEnd = yamlMatch.groupValues[3]
                val remainingContent = content.substring(yamlMatch.range.last + 1)

                // Update the title field (YAML format: title: "value")
                val titleRegex = "title\\s*:\\s*[\"']?[^\"'\\n]*[\"']?".toRegex()
                val updatedFrontmatter = if (titleRegex.containsMatchIn(frontmatterBody)) {
                    frontmatterBody.replace(titleRegex, "title: \"$newTitle\"")
                } else {
                    // If title field doesn't exist, add it at the beginning
                    "title: \"$newTitle\"\n$frontmatterBody"
                }

                frontmatterStart + updatedFrontmatter + frontmatterEnd + remainingContent
            }
            tomlMatch != null -> {
                val frontmatterStart = tomlMatch.groupValues[1]
                val frontmatterBody = tomlMatch.groupValues[2]
                val frontmatterEnd = tomlMatch.groupValues[3]
                val remainingContent = content.substring(tomlMatch.range.last + 1)

                // Update the title field (TOML format: title = "value")
                val titleRegex = "title\\s*=\\s*[\"']?[^\"'\\n]*[\"']?".toRegex()
                val updatedFrontmatter = if (titleRegex.containsMatchIn(frontmatterBody)) {
                    frontmatterBody.replace(titleRegex, "title = \"$newTitle\"")
                } else {
                    // If title field doesn't exist, add it at the beginning
                    "title = \"$newTitle\"\n$frontmatterBody"
                }

                frontmatterStart + updatedFrontmatter + frontmatterEnd + remainingContent
            }
            else -> content // No frontmatter found, return as is
        }
    }

    suspend fun savePost() {
        _isSaving.value = true
        try {
            val currentPost = _post.value ?: return

            // Don't save if both title and content are empty
            if (_title.value.isBlank() && _content.value.isBlank()) {
                return
            }

            val settings = settingsRepository.appSettings.first()

            // Trim the title to remove leading/trailing spaces
            val trimmedTitle = _title.value.trim()

            // Check if content has frontmatter
            val hasFrontmatter = _content.value.startsWith("---") || _content.value.startsWith("+++")
            val isNewPost = currentPost.content.isEmpty()

            val updatedContent = when {
                // New post without frontmatter - add frontmatter
                isNewPost && trimmedTitle.isNotBlank() && !hasFrontmatter -> {
                    AppSettings.generateFrontmatter(trimmedTitle, settings.frontmatterTemplate) + _content.value
                }
                // Existing post with frontmatter - update the title in frontmatter
                hasFrontmatter && trimmedTitle != currentPost.title -> {
                    updateFrontmatterTitle(_content.value, trimmedTitle)
                }
                // Otherwise keep content as is
                else -> _content.value
            }

            val updatedPost = currentPost.copy(
                title = trimmedTitle,
                content = updatedContent,
                updatedAt = System.currentTimeMillis()
            )

            postRepository.savePost(updatedPost)
            _post.value = updatedPost
            _title.value = trimmedTitle
            _content.value = updatedContent
        } finally {
            _isSaving.value = false
        }
    }

    fun publishPost() {
        viewModelScope.launch {
            _isPublishing.value = true
            _publishResult.value = null

            try {
                // Save before publishing
                savePost()

                val currentPost = _post.value
                if (currentPost == null) {
                    _publishResult.value = Result.failure(Exception("No post to publish"))
                    return@launch
                }

                val gitHubConfig = settingsRepository.gitHubConfig.first()

                if (!gitHubConfig.isValid()) {
                    _publishResult.value = Result.failure(
                        Exception("GitHub configuration is incomplete. Please configure in Settings.")
                    )
                    return@launch
                }

                val result = gitHubService.publishPost(currentPost, gitHubConfig)

                if (result.isSuccess) {
                    // Update post as published and track the filename
                    val publishedPost = currentPost.copy(
                        isPublished = true,
                        publishedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        publishedFilename = currentPost.getFileName()  // Track the published filename
                    )
                    postRepository.savePost(publishedPost)
                    _post.value = publishedPost
                }

                _publishResult.value = result
            } finally {
                _isPublishing.value = false
            }
        }
    }

    fun clearPublishResult() {
        _publishResult.value = null
    }
}
