package com.rrajath.bloggo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

/**
 * Fired by the "Capture a thought" app shortcut (long-press the launcher
 * icon, res/xml/shortcuts.xml). Read in [BloggoApp] to jump straight to
 * [Route.Inbox] with its capture field focused, skipping whatever screen
 * the app would otherwise resume to. A plain tap on the regular launcher
 * icon delivers [Intent.ACTION_MAIN] instead, which BloggoApp ignores —
 * this shortcut is additive, not a change to normal launch behaviour.
 */
const val ACTION_CAPTURE_THOUGHT = "com.rrajath.bloggo.action.CAPTURE_THOUGHT"

class MainActivity : ComponentActivity() {
  // Single-Activity shell: BloggoApp owns all navigation, so a shortcut
  // intent fired while the app is already running has to reach the
  // already-composed BloggoApp rather than start a second Activity
  // instance. android:launchMode="singleTask" (manifest) plus onNewIntent
  // below make that happen — with the default "standard" launch mode,
  // Android would just bring the existing task to the front and drop the
  // new intent on the floor, and the shortcut would silently do nothing
  // whenever the app was already open.
  private val launchIntentState = mutableStateOf<Intent?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    launchIntentState.value = intent
    setContent { BloggoApp(launchIntent = launchIntentState.value) }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    launchIntentState.value = intent
  }
}
