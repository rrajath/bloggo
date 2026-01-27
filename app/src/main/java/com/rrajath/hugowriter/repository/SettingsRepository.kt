package com.rrajath.hugowriter.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rrajath.hugowriter.data.AppSettings
import com.rrajath.hugowriter.data.GitHubConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val FRONTMATTER_TEMPLATE = stringPreferencesKey("frontmatter_template")
        private val GITHUB_PAT = stringPreferencesKey("github_pat")
        private val GITHUB_REPO_OWNER = stringPreferencesKey("github_repo_owner")
        private val GITHUB_REPO_NAME = stringPreferencesKey("github_repo_name")
        private val GITHUB_BRANCH = stringPreferencesKey("github_branch")
        private val GITHUB_TARGET_DIR = stringPreferencesKey("github_target_dir")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[IS_DARK_MODE] ?: false,
            frontmatterTemplate = preferences[FRONTMATTER_TEMPLATE]
                ?: AppSettings.DEFAULT_FRONTMATTER_TEMPLATE
        )
    }

    val gitHubConfig: Flow<GitHubConfig> = context.dataStore.data.map { preferences ->
        GitHubConfig(
            personalAccessToken = preferences[GITHUB_PAT] ?: "",
            repositoryOwner = preferences[GITHUB_REPO_OWNER] ?: "",
            repositoryName = preferences[GITHUB_REPO_NAME] ?: "",
            branch = preferences[GITHUB_BRANCH] ?: "main",
            targetDirectory = preferences[GITHUB_TARGET_DIR] ?: "content/posts/"
        )
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun updateFrontmatterTemplate(template: String) {
        context.dataStore.edit { preferences ->
            preferences[FRONTMATTER_TEMPLATE] = template
        }
    }

    suspend fun updateGitHubConfig(config: GitHubConfig) {
        context.dataStore.edit { preferences ->
            preferences[GITHUB_PAT] = config.personalAccessToken
            preferences[GITHUB_REPO_OWNER] = config.repositoryOwner
            preferences[GITHUB_REPO_NAME] = config.repositoryName
            preferences[GITHUB_BRANCH] = config.branch
            preferences[GITHUB_TARGET_DIR] = config.targetDirectory
        }
    }
}
