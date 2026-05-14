package com.rrajath.hugowriter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.hugowriter.data.AppSettings
import com.rrajath.hugowriter.data.GitHubConfig
import com.rrajath.hugowriter.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _frontmatterTemplate = MutableStateFlow(AppSettings.DEFAULT_FRONTMATTER_TEMPLATE)
    val frontmatterTemplate: StateFlow<String> = _frontmatterTemplate.asStateFlow()

    private val _githubPat = MutableStateFlow("")
    val githubPat: StateFlow<String> = _githubPat.asStateFlow()

    private val _githubRepoOwner = MutableStateFlow("")
    val githubRepoOwner: StateFlow<String> = _githubRepoOwner.asStateFlow()

    private val _githubRepoName = MutableStateFlow("")
    val githubRepoName: StateFlow<String> = _githubRepoName.asStateFlow()

    private val _githubBranch = MutableStateFlow("main")
    val githubBranch: StateFlow<String> = _githubBranch.asStateFlow()

    private val _githubTargetDirectories = MutableStateFlow<List<String>>(listOf("content/posts/"))
    val githubTargetDirectories: StateFlow<List<String>> = _githubTargetDirectories.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Only load initial values, don't continuously collect
            val settings = settingsRepository.appSettings.first()
            _isDarkMode.value = settings.isDarkMode
            _frontmatterTemplate.value = settings.frontmatterTemplate
        }

        viewModelScope.launch {
            // Only load initial values, don't continuously collect
            val config = settingsRepository.gitHubConfig.first()
            _githubPat.value = config.personalAccessToken
            _githubRepoOwner.value = config.repositoryOwner
            _githubRepoName.value = config.repositoryName
            _githubBranch.value = config.branch
            _githubTargetDirectories.value = config.targetDirectories
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newValue = !_isDarkMode.value
            _isDarkMode.value = newValue
            settingsRepository.updateDarkMode(newValue)
        }
    }

    fun updateFrontmatterTemplate(template: String) {
        _frontmatterTemplate.value = template
    }

    fun updateGitHubPat(pat: String) {
        _githubPat.value = pat
    }

    fun updateGitHubRepoOwner(owner: String) {
        _githubRepoOwner.value = owner
    }

    fun updateGitHubRepoName(name: String) {
        _githubRepoName.value = name
    }

    fun updateGitHubBranch(branch: String) {
        _githubBranch.value = branch
    }

    fun updateGitHubTargetDirectory(index: Int, dir: String) {
        val currentList = _githubTargetDirectories.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = dir
            _githubTargetDirectories.value = currentList
        }
    }

    fun addGitHubTargetDirectory() {
        val currentList = _githubTargetDirectories.value.toMutableList()
        currentList.add("content/posts/")
        _githubTargetDirectories.value = currentList
    }

    fun removeGitHubTargetDirectory(index: Int) {
        val currentList = _githubTargetDirectories.value.toMutableList()
        if (index in currentList.indices && currentList.size > 1) {
            currentList.removeAt(index)
            _githubTargetDirectories.value = currentList
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                settingsRepository.updateFrontmatterTemplate(_frontmatterTemplate.value)
                settingsRepository.updateGitHubConfig(
                    GitHubConfig(
                        personalAccessToken = _githubPat.value,
                        repositoryOwner = _githubRepoOwner.value,
                        repositoryName = _githubRepoName.value,
                        branch = _githubBranch.value,
                        targetDirectories = _githubTargetDirectories.value
                    )
                )
            } finally {
                _isSaving.value = false
            }
        }
    }
}
