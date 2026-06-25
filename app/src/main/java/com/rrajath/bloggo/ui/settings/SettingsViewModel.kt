package com.rrajath.bloggo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrajath.bloggo.data.Settings
import com.rrajath.bloggo.data.SettingsRepository
import com.rrajath.bloggo.ui.theme.Accent
import com.rrajath.bloggo.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Settings(),
        )

    fun savePat(pat: String) = viewModelScope.launch {
        settingsRepository.savePat(pat)
    }

    fun saveRepository(repo: String) = viewModelScope.launch {
        settingsRepository.saveRepository(repo)
    }

    fun saveBranch(branch: String) = viewModelScope.launch {
        settingsRepository.saveBranch(branch)
    }

    fun saveContentPath(path: String) = viewModelScope.launch {
        settingsRepository.saveContentPath(path)
    }

    fun saveImageRepoPath(path: String) = viewModelScope.launch {
        settingsRepository.saveImageRepoPath(path)
    }

    fun saveImageUrlBase(url: String) = viewModelScope.launch {
        settingsRepository.saveImageUrlBase(url)
    }

    fun saveBlogBaseUrl(url: String) = viewModelScope.launch {
        settingsRepository.saveBlogBaseUrl(url)
    }

    fun saveFrontMatterTemplate(template: String) = viewModelScope.launch {
        settingsRepository.saveFrontMatterTemplate(template)
    }

    fun saveTheme(theme: ThemeMode) = viewModelScope.launch {
        settingsRepository.saveTheme(theme)
    }

    fun saveAccent(accent: Accent) = viewModelScope.launch {
        settingsRepository.saveAccent(accent)
    }

    fun saveAppLock(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.saveAppLock(enabled)
    }
}
