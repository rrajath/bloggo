package com.rrajath.bloggo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rrajath.bloggo.ui.review.ReadabilityCheck
import kotlinx.coroutines.flow.map

/** Overrides the system theme. [System] tracks the device setting. */
enum class ThemeMode {
  System, Light, Dark;

  companion object {
    fun fromStored(name: String?): ThemeMode = entries.find { it.name == name } ?: System
  }
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * The app's small, typed settings: theme mode and the readability check set.
 *
 * Preferences DataStore rather than the Proto DataStore the TDD names, since a
 * couple of enums do not earn a `.proto` schema and codegen step. Revisit if the
 * settings surface grows enough that untyped keys start to hurt.
 */
class SettingsRepository(context: Context) {
  private val dataStore = context.settingsDataStore
  private val themeModeKey = stringPreferencesKey("theme_mode")
  // A comma-joined list of enabled check names, the same shape as
  // RepoConnection.frontmatterFields. An absent key means every check is on;
  // an empty string means the writer turned all of them off.
  private val readabilityChecksKey = stringPreferencesKey("readability_checks")

  val themeMode = dataStore.data.map { prefs -> ThemeMode.fromStored(prefs[themeModeKey]) }
  val readabilityChecks =
    dataStore.data.map { prefs -> ReadabilityCheck.fromStored(prefs[readabilityChecksKey]) }

  suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
  }

  suspend fun setReadabilityChecks(checks: Set<ReadabilityCheck>) {
    dataStore.edit { prefs -> prefs[readabilityChecksKey] = checks.joinToString(",") { it.name } }
  }
}
