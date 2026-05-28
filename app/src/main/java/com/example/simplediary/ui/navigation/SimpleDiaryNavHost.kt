package com.example.simplediary.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.simplediary.app.SimpleDiaryApplication
import com.example.simplediary.ui.feed.FeedScreen
import com.example.simplediary.ui.feed.FeedViewModel
import com.example.simplediary.ui.meal.MealEditorScreen
import com.example.simplediary.ui.meal.MealViewModel
import com.example.simplediary.ui.settings.SettingsScreen
import com.example.simplediary.ui.settings.SettingsViewModel
import com.example.simplediary.ui.summary.DailySummaryScreen
import com.example.simplediary.ui.summary.SummaryViewModel
import com.example.simplediary.ui.state_note.StateNoteEditorScreen
import com.example.simplediary.ui.state_note.StateNoteViewModel
import com.example.simplediary.ui.workout.WorkoutEditorScreen
import com.example.simplediary.ui.workout.WorkoutViewModel

@Composable
fun SimpleDiaryNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    feedViewModel: FeedViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = SimpleDiaryDestination.Feed.route,
    ) {
        composable(SimpleDiaryDestination.Feed.route) {
            val uiState by feedViewModel.uiState.collectAsState()
            FeedScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onEntryTypeToggle = feedViewModel::onEntryTypeToggle,
                onDateRangeOptionSelected = feedViewModel::onDateRangeOptionSelected,
                onCustomDateRangeSelected = feedViewModel::onCustomDateRangeSelected,
                onToggleSortOrder = feedViewModel::onToggleSortOrder,
                onAddMealClick = { navController.navigate(SimpleDiaryDestination.MealEditor.createRoute()) },
                onAddWorkoutClick = { navController.navigate(SimpleDiaryDestination.WorkoutEditor.createRoute()) },
                onAddStateNoteClick = { navController.navigate(SimpleDiaryDestination.StateNoteEditor.createRoute()) },
                onMealClick = { mealId ->
                    navController.navigate(SimpleDiaryDestination.MealEditor.createRoute(mealId))
                },
                onWorkoutClick = { workoutId ->
                    navController.navigate(SimpleDiaryDestination.WorkoutEditor.createRoute(workoutId))
                },
                onStateNoteClick = { stateNoteId ->
                    navController.navigate(SimpleDiaryDestination.StateNoteEditor.createRoute(stateNoteId))
                },
            )
        }
        composable(
            route = SimpleDiaryDestination.MealEditor.route,
            arguments = listOf(
                navArgument(SimpleDiaryDestination.MealEditor.ARG_MEAL_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val app = LocalContext.current.applicationContext as SimpleDiaryApplication
            val mealId = backStackEntry.arguments
                ?.getString(SimpleDiaryDestination.MealEditor.ARG_MEAL_ID)
                ?.toLongOrNull()

            val mealViewModel: MealViewModel = viewModel(
                key = "meal_editor_${mealId ?: "new"}",
                factory = MealViewModel.factory(app, mealId),
            )
            val uiState by mealViewModel.uiState.collectAsState()

            MealEditorScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onTimestampChanged = mealViewModel::onTimestampChanged,
                onNoteChanged = mealViewModel::onNoteChanged,
                onRowChanged = mealViewModel::onNutritionRowChanged,
                onDeleteRow = mealViewModel::onDeleteNutritionRow,
                onAddRow = mealViewModel::onAddNutritionRow,
                onSaveClick = mealViewModel::onSaveClick,
                onDeleteClick = mealViewModel::onDeleteClick,
                onPhotoPicked = mealViewModel::onPhotoPicked,
                onRemovePhoto = mealViewModel::onPhotoRemoved,
                events = mealViewModel.events,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = SimpleDiaryDestination.WorkoutEditor.route,
            arguments = listOf(
                navArgument(SimpleDiaryDestination.WorkoutEditor.ARG_WORKOUT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val app = LocalContext.current.applicationContext as SimpleDiaryApplication
            val workoutId = backStackEntry.arguments
                ?.getString(SimpleDiaryDestination.WorkoutEditor.ARG_WORKOUT_ID)
                ?.toLongOrNull()

            val workoutViewModel: WorkoutViewModel = viewModel(
                key = "workout_editor_${workoutId ?: "new"}",
                factory = WorkoutViewModel.factory(app, workoutId),
            )
            val uiState by workoutViewModel.uiState.collectAsState()

            WorkoutEditorScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onTimestampChanged = workoutViewModel::onTimestampChanged,
                onCategorySelected = workoutViewModel::onCategorySelected,
                onTypeSelected = workoutViewModel::onTypeSelected,
                onDurationChanged = workoutViewModel::onDurationChanged,
                onCaloriesChanged = workoutViewModel::onCaloriesChanged,
                onDistanceChanged = workoutViewModel::onDistanceChanged,
                onNoteChanged = workoutViewModel::onNoteChanged,
                onSaveClick = workoutViewModel::onSaveClick,
                onDeleteClick = workoutViewModel::onDeleteClick,
                events = workoutViewModel.events,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = SimpleDiaryDestination.StateNoteEditor.route,
            arguments = listOf(
                navArgument(SimpleDiaryDestination.StateNoteEditor.ARG_STATE_NOTE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val app = LocalContext.current.applicationContext as SimpleDiaryApplication
            val stateNoteId = backStackEntry.arguments
                ?.getString(SimpleDiaryDestination.StateNoteEditor.ARG_STATE_NOTE_ID)
                ?.toLongOrNull()

            val stateNoteViewModel: StateNoteViewModel = viewModel(
                key = "state_note_editor_${stateNoteId ?: "new"}",
                factory = StateNoteViewModel.factory(app, stateNoteId),
            )
            val uiState by stateNoteViewModel.uiState.collectAsState()

            StateNoteEditorScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onTimestampChanged = stateNoteViewModel::onTimestampChanged,
                onCategoryChanged = stateNoteViewModel::onCategoryChanged,
                onTextChanged = stateNoteViewModel::onTextChanged,
                onPhotoPicked = stateNoteViewModel::onPhotoPicked,
                onRemovePhoto = stateNoteViewModel::onPhotoRemoved,
                onSaveClick = stateNoteViewModel::onSaveClick,
                onDeleteClick = stateNoteViewModel::onDeleteClick,
                events = stateNoteViewModel.events,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(SimpleDiaryDestination.DailySummary.route) {
            val app = LocalContext.current.applicationContext as SimpleDiaryApplication
            val summaryViewModel: SummaryViewModel = viewModel(
                key = "daily_summary",
                factory = SummaryViewModel.factory(app.journalRepository),
            )
            val uiState by summaryViewModel.uiState.collectAsState()
            DailySummaryScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onSummaryModeSelected = summaryViewModel::onSummaryModeSelected,
                onPreviousDayClick = summaryViewModel::onPreviousDayClick,
                onNextDayClick = summaryViewModel::onNextDayClick,
                onPreviousWeekClick = summaryViewModel::onPreviousWeekClick,
                onNextWeekClick = summaryViewModel::onNextWeekClick,
                onCurrentWeekClick = summaryViewModel::onCurrentWeekClick,
                onCustomIntervalSelected = summaryViewModel::onCustomIntervalSelected,
                onOpenSettingsClick = { navController.navigate(SimpleDiaryDestination.Settings.route) },
            )
        }
        composable(SimpleDiaryDestination.Settings.route) {
            val app = LocalContext.current.applicationContext as SimpleDiaryApplication
            val settingsViewModel: SettingsViewModel = viewModel(
                key = "settings",
                factory = SettingsViewModel.factory(app),
            )
            val uiState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                contentPadding = contentPadding,
                uiState = uiState,
                onTargetCaloriesChanged = settingsViewModel::onTargetCaloriesChanged,
                onTargetProteinsChanged = settingsViewModel::onTargetProteinsChanged,
                onTargetFatsChanged = settingsViewModel::onTargetFatsChanged,
                onTargetCarbsChanged = settingsViewModel::onTargetCarbsChanged,
                onSaveTargetsClick = settingsViewModel::onSaveTargetsClick,
                onAddCategory = settingsViewModel::addCategory,
                onRenameCategory = settingsViewModel::renameCategory,
                onDeleteCategory = settingsViewModel::deleteCategory,
                onAddType = settingsViewModel::addType,
                onRenameType = settingsViewModel::renameType,
                onDeleteType = settingsViewModel::deleteType,
                onAddNoteCategory = settingsViewModel::addNoteCategory,
                onRenameNoteCategory = settingsViewModel::renameNoteCategory,
                onDeleteNoteCategory = settingsViewModel::deleteNoteCategory,
                onExportCsv = settingsViewModel::exportCsv,
                onBackupZip = settingsViewModel::backupZip,
                onRestoreZip = settingsViewModel::restoreZip,
                events = settingsViewModel.events,
            )
        }
    }
}
