package com.example.simplediary.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.model.WeeklyWorkoutStats
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SummaryViewModel(
    private val repository: JournalRepository,
) : ViewModel() {
    private val zoneId = ZoneId.systemDefault()
    private val todayStartEpochMillis = localDateToEpochMillis(LocalDate.now(zoneId))
    private val selectedDayStartEpochMillis = MutableStateFlow(todayStartEpochMillis)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dailySummaryFlow = selectedDayStartEpochMillis
        .flatMapLatest { dayStart -> repository.observeDailySummary(dayStart) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val weeklyStatsFlow = selectedDayStartEpochMillis
        .map { dayStart -> dayStartEpochToWeekStartEpoch(dayStart) }
        .flatMapLatest { weekStart -> repository.observeWeeklyWorkoutStats(weekStart) }

    val uiState: StateFlow<SummaryUiState> = combine(
        selectedDayStartEpochMillis,
        dailySummaryFlow,
        weeklyStatsFlow,
    ) { selectedDayStart, dailySummary, weeklyStats ->
        SummaryUiState(
            selectedDayStartEpochMillis = selectedDayStart,
            isTodaySelected = selectedDayStart == todayStartEpochMillis,
            dailySummary = dailySummary,
            weeklyStats = weeklyStats,
            hasTarget = hasTarget(dailySummary),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(
            selectedDayStartEpochMillis = todayStartEpochMillis,
            isTodaySelected = true,
            dailySummary = emptyDailySummary(todayStartEpochMillis),
            weeklyStats = WeeklyWorkoutStats(
                weekStartEpochMillisUtc = dayStartEpochToWeekStartEpoch(todayStartEpochMillis),
                totalWorkouts = 0,
                totalDurationMinutes = 0,
                totalCaloriesBurned = 0,
            ),
            hasTarget = false,
        ),
    )

    fun onPreviousDayClick() {
        selectedDayStartEpochMillis.value = selectedDayStartEpochMillis.value - ONE_DAY_MILLIS
    }

    fun onNextDayClick() {
        val nextDay = selectedDayStartEpochMillis.value + ONE_DAY_MILLIS
        if (nextDay <= todayStartEpochMillis) {
            selectedDayStartEpochMillis.value = nextDay
        }
    }

    private fun dayStartEpochToWeekStartEpoch(dayStartEpochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(dayStartEpochMillis).atZone(zoneId).toLocalDate()
        var cursor = localDate
        while (cursor.dayOfWeek != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1)
        }
        return localDateToEpochMillis(cursor)
    }

    private fun localDateToEpochMillis(localDate: LocalDate): Long {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun hasTarget(summary: DailySummary): Boolean {
        return summary.targetCaloriesKcal > 0.0 ||
            summary.targetProteinsGrams > 0.0 ||
            summary.targetFatsGrams > 0.0 ||
            summary.targetCarbsGrams > 0.0
    }

    private fun emptyDailySummary(dayStartEpochMillis: Long): DailySummary {
        return DailySummary(
            dateEpochMillisUtcStart = dayStartEpochMillis,
            actualProteinsGrams = 0.0,
            targetProteinsGrams = 0.0,
            actualFatsGrams = 0.0,
            targetFatsGrams = 0.0,
            actualCarbsGrams = 0.0,
            targetCarbsGrams = 0.0,
            actualCaloriesKcal = 0.0,
            targetCaloriesKcal = 0.0,
        )
    }

    companion object {
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun factory(repository: JournalRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return SummaryViewModel(repository) as T
                }
            }
        }
    }
}

data class SummaryUiState(
    val selectedDayStartEpochMillis: Long,
    val isTodaySelected: Boolean,
    val dailySummary: DailySummary,
    val weeklyStats: WeeklyWorkoutStats,
    val hasTarget: Boolean,
)
