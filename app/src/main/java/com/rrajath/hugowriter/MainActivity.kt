package com.rrajath.hugowriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rrajath.hugowriter.navigation.HugoWriterApp
import com.rrajath.hugowriter.repository.SettingsRepository
import com.rrajath.hugowriter.ui.theme.HugoWriterTheme

class MainActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val appSettings by settingsRepository.appSettings.collectAsState(
                initial = com.rrajath.hugowriter.data.AppSettings()
            )

            HugoWriterTheme(
                darkTheme = appSettings.isDarkMode
            ) {
                HugoWriterApp(
                    darkTheme = appSettings.isDarkMode
                )
            }
        }
    }
}