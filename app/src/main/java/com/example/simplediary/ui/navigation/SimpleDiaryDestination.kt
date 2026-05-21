package com.example.simplediary.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SimpleDiaryDestination(
    val route: String,
    val title: String,
) {
    data object Feed : SimpleDiaryDestination("feed", "Feed")
    data object MealEditor : SimpleDiaryDestination("meal_editor?mealId={mealId}", "Add/Edit Meal") {
        const val BASE_ROUTE = "meal_editor"
        const val ARG_MEAL_ID = "mealId"
        fun createRoute(mealId: Long? = null): String {
            return if (mealId == null) BASE_ROUTE else "$BASE_ROUTE?mealId=$mealId"
        }
    }
    data object WorkoutEditor : SimpleDiaryDestination("workout_editor?workoutId={workoutId}", "Add/Edit Workout") {
        const val BASE_ROUTE = "workout_editor"
        const val ARG_WORKOUT_ID = "workoutId"
        fun createRoute(workoutId: Long? = null): String {
            return if (workoutId == null) BASE_ROUTE else "$BASE_ROUTE?workoutId=$workoutId"
        }
    }
    data object StateNoteEditor : SimpleDiaryDestination("state_note_editor?stateNoteId={stateNoteId}", "Add/Edit State Note") {
        const val BASE_ROUTE = "state_note_editor"
        const val ARG_STATE_NOTE_ID = "stateNoteId"
        fun createRoute(stateNoteId: Long? = null): String {
            return if (stateNoteId == null) BASE_ROUTE else "$BASE_ROUTE?stateNoteId=$stateNoteId"
        }
    }
    data object DailySummary : SimpleDiaryDestination("daily_summary", "Daily Summary")
    data object Settings : SimpleDiaryDestination("settings", "Settings")
}

data class BottomNavDestination(
    val destination: SimpleDiaryDestination,
    val icon: ImageVector,
)

val bottomNavDestinations = listOf(
    BottomNavDestination(SimpleDiaryDestination.Feed, Icons.Outlined.Home),
    BottomNavDestination(SimpleDiaryDestination.DailySummary, Icons.Outlined.Today),
    BottomNavDestination(SimpleDiaryDestination.Settings, Icons.Outlined.Settings),
)
