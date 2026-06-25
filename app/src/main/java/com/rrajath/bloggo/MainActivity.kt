package com.rrajath.bloggo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.rrajath.bloggo.security.BiometricHelper
import com.rrajath.bloggo.ui.navigation.BloggoNavHost
import com.rrajath.bloggo.ui.theme.Accent
import com.rrajath.bloggo.ui.theme.BloggoTheme
import com.rrajath.bloggo.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloggoTheme(
                themeMode = ThemeMode.SYSTEM,
                accent = Accent.INDIGO,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    BloggoNavHost(navController = navController)
                }
            }
        }
    }
}
