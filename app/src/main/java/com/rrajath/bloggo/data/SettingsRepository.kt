package com.rrajath.bloggo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rrajath.bloggo.ui.theme.Accent
import com.rrajath.bloggo.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureStorage: SecureStorage,
) {
    companion object {
        private val REPOSITORY = stringPreferencesKey("repository")
        private val BRANCH = stringPreferencesKey("branch")
        private val CONTENT_PATH = stringPreferencesKey("content_path")
        private val IMAGE_REPO_PATH = stringPreferencesKey("image_repo_path")
        private val IMAGE_URL_BASE = stringPreferencesKey("image_url_base")
        private val BLOG_BASE_URL = stringPreferencesKey("blog_base_url")
        private val FM_TEMPLATE = stringPreferencesKey("fm_template")
        private val THEME = stringPreferencesKey("theme")
        private val ACCENT = stringPreferencesKey("accent")
        private val APP_LOCK = booleanPreferencesKey("app_lock")
    }

    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            githubPat = secureStorage.getPat(),
            repository = prefs[REPOSITORY] ?: "",
            branch = prefs[BRANCH] ?: "main",
            contentPath = prefs[CONTENT_PATH] ?: "content/posts",
            imageRepoPath = prefs[IMAGE_REPO_PATH] ?: "static/images",
            imageUrlBase = prefs[IMAGE_URL_BASE] ?: "/images",
            blogBaseUrl = prefs[BLOG_BASE_URL] ?: "",
            frontMatterTemplate = prefs[FM_TEMPLATE] ?: "date: {date}\ntags: []\nsummary: \"\"",
            theme = prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            accent = prefs[ACCENT]?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.INDIGO,
            appLock = prefs[APP_LOCK] ?: false,
        )
    }

    suspend fun savePat(pat: String) {
        secureStorage.setPat(pat)
    }

    suspend fun saveRepository(value: String) =
        dataStore.edit { it[REPOSITORY] = value }

    suspend fun saveBranch(value: String) =
        dataStore.edit { it[BRANCH] = value }

    suspend fun saveContentPath(value: String) =
        dataStore.edit { it[CONTENT_PATH] = value }

    suspend fun saveImageRepoPath(value: String) =
        dataStore.edit { it[IMAGE_REPO_PATH] = value }

    suspend fun saveImageUrlBase(value: String) =
        dataStore.edit { it[IMAGE_URL_BASE] = value }

    suspend fun saveBlogBaseUrl(value: String) =
        dataStore.edit { it[BLOG_BASE_URL] = value }

    suspend fun saveFrontMatterTemplate(value: String) =
        dataStore.edit { it[FM_TEMPLATE] = value }

    suspend fun saveTheme(value: ThemeMode) =
        dataStore.edit { it[THEME] = value.name }

    suspend fun saveAccent(value: Accent) =
        dataStore.edit { it[ACCENT] = value.name }

    suspend fun saveAppLock(value: Boolean) =
        dataStore.edit { it[APP_LOCK] = value }

    suspend fun saveAll(settings: Settings) {
        savePat(settings.githubPat)
        dataStore.edit { prefs ->
            prefs[REPOSITORY] = settings.repository
            prefs[BRANCH] = settings.branch
            prefs[CONTENT_PATH] = settings.contentPath
            prefs[IMAGE_REPO_PATH] = settings.imageRepoPath
            prefs[IMAGE_URL_BASE] = settings.imageUrlBase
            prefs[BLOG_BASE_URL] = settings.blogBaseUrl
            prefs[FM_TEMPLATE] = settings.frontMatterTemplate
            prefs[THEME] = settings.theme.name
            prefs[ACCENT] = settings.accent.name
            prefs[APP_LOCK] = settings.appLock
        }
    }
}
