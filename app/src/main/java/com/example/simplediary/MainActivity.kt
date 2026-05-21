package com.example.simplediary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.ui.feed.FeedViewModel
import com.example.simplediary.ui.SimpleDiaryApp
import com.example.simplediary.ui.navigation.SimpleDiaryDestination
import com.example.simplediary.ui.theme.SimpleDiaryTheme

class MainActivity : ComponentActivity() {
    private var pendingShortcutRoute: String? by mutableStateOf(null)

    private val feedViewModel: FeedViewModel by viewModels {
        FeedViewModel.factory(
            journalRepository = (application as SimpleDiaryApplication).journalRepository,
            context = applicationContext,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShortcutRoute = resolveShortcutRoute(intent)
        enableEdgeToEdge()
        setContent {
            SimpleDiaryTheme {
                SimpleDiaryApp(
                    feedViewModel = feedViewModel,
                    pendingShortcutRoute = pendingShortcutRoute,
                    onShortcutConsumed = { pendingShortcutRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutRoute = resolveShortcutRoute(intent)
    }

    private fun resolveShortcutRoute(intent: Intent?): String? {
        return when (intent?.action) {
            ACTION_SHORTCUT_ADD_MEAL -> SimpleDiaryDestination.MealEditor.createRoute()
            ACTION_SHORTCUT_ADD_WORKOUT -> SimpleDiaryDestination.WorkoutEditor.createRoute()
            ACTION_SHORTCUT_ADD_NOTE -> SimpleDiaryDestination.StateNoteEditor.createRoute()
            else -> null
        }
    }

    companion object {
        const val ACTION_SHORTCUT_ADD_MEAL = "com.example.simplediary.action.ADD_MEAL"
        const val ACTION_SHORTCUT_ADD_WORKOUT = "com.example.simplediary.action.ADD_WORKOUT"
        const val ACTION_SHORTCUT_ADD_NOTE = "com.example.simplediary.action.ADD_NOTE"
    }
}