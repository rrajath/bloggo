package com.rrajath.bloggo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rrajath.bloggo.designsystem.component.ArtMode
import kotlinx.coroutines.flow.map

/** Overrides the system theme. [System] tracks the device setting. */
enum class ThemeMode {
  System, Light, Dark;

  companion object {
    fun fromStored(name: String?): ThemeMode = entries.find { it.name == name } ?: System
  }
}

private fun artModeFromStored(name: String?): ArtMode =
  ArtMode.entries.find { it.name == name } ?: ArtMode.Generated

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * The app's small, typed settings: theme and the Settings screen's cover-art toggle.
 *
 * Preferences DataStore rather than the Proto DataStore the TDD names, since a
 * couple of enums do not earn a `.proto` schema and codegen step. Revisit if the
 * settings surface grows enough that untyped keys start to hurt.
 */
class SettingsRepository(context: Context) {
  private val dataStore = context.settingsDataStore
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val artModeKey = stringPreferencesKey("art_mode")

  val themeMode = dataStore.data.map { prefs -> ThemeMode.fromStored(prefs[themeModeKey]) }
  val artMode = dataStore.data.map { prefs -> artModeFromStored(prefs[artModeKey]) }

  suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
  }

  suspend fun setArtMode(mode: ArtMode) {
    dataStore.edit { prefs -> prefs[artModeKey] = mode.name }
  }
}
