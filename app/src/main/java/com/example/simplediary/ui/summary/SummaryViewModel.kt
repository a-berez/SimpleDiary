package com.example.simplediary.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.domain.model.DailySummary
import com.example.simplediary.domain.model.DailyWorkoutStats
import com.example.simplediary.domain.model.WeeklyFoodSummary
import com.example.simplediary.domain.model.WeeklyWorkoutStats
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val selectedSummaryMode = MutableStateFlow(SummaryMode.DAY)
    private val selectedInterval = MutableStateFlow(currentWeekInterval(todayStartEpochMillis))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dailySummaryFlow = selectedDayStartEpochMillis
        .flatMapLatest { dayStart -> repository.observeDailySummary(dayStart) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dailyWorkoutStatsFlow = selectedDayStartEpochMillis
        .flatMapLatest { dayStart -> repository.observeDailyWorkoutStats(dayStart) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val intervalFoodSummaryFlow = selectedInterval
        .flatMapLatest { interval ->
            repository.observeFoodSummaryInRange(
                fromEpochMillisInclusive = interval.startEpochMillis,
                toEpochMillisExclusive = interval.endEpochMillis + ONE_DAY_MILLIS,
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val intervalWorkoutSummaryFlow = selectedInterval
        .flatMapLatest { interval ->
            repository.observeWorkoutSummaryInRange(
                fromEpochMillisInclusive = interval.startEpochMillis,
                toEpochMillisExclusive = interval.endEpochMillis + ONE_DAY_MILLIS,
            )
        }

    private val summaryHeaderFlow = combine(
        selectedSummaryMode,
        selectedDayStartEpochMillis,
        selectedInterval,
    ) { mode, selectedDayStart, interval ->
        SummaryHeader(
            mode = mode,
            selectedDayStart = selectedDayStart,
            interval = interval,
        )
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        summaryHeaderFlow,
        dailySummaryFlow,
        dailyWorkoutStatsFlow,
        intervalFoodSummaryFlow,
        intervalWorkoutSummaryFlow,
    ) { header, dailySummary, dailyWorkoutStats, intervalFoodSummary, intervalWorkoutSummary ->
        SummaryUiState(
            summaryMode = header.mode,
            selectedDayStartEpochMillis = header.selectedDayStart,
            isTodaySelected = header.selectedDayStart == todayStartEpochMillis,
            selectedIntervalStartEpochMillis = header.interval.startEpochMillis,
            selectedIntervalEndEpochMillis = header.interval.endEpochMillis,
            dailySummary = dailySummary,
            dailyWorkoutStats = dailyWorkoutStats,
            intervalFoodSummary = intervalFoodSummary,
            intervalWorkoutSummary = intervalWorkoutSummary,
            hasTarget = hasTarget(dailySummary),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(
            summaryMode = SummaryMode.DAY,
            selectedDayStartEpochMillis = todayStartEpochMillis,
            isTodaySelected = true,
            selectedIntervalStartEpochMillis = currentWeekInterval(todayStartEpochMillis).startEpochMillis,
            selectedIntervalEndEpochMillis = currentWeekInterval(todayStartEpochMillis).endEpochMillis,
            dailySummary = emptyDailySummary(todayStartEpochMillis),
            dailyWorkoutStats = DailyWorkoutStats(
                dayStartEpochMillisUtc = todayStartEpochMillis,
                totalWorkouts = 0,
                totalDurationMinutes = 0,
                totalCaloriesBurned = 0,
                totalCardioDistanceKm = 0.0,
            ),
            intervalFoodSummary = WeeklyFoodSummary(
                weekStartEpochMillisUtc = currentWeekInterval(todayStartEpochMillis).startEpochMillis,
                actualProteinsGrams = 0.0,
                actualFatsGrams = 0.0,
                actualCarbsGrams = 0.0,
                actualCaloriesKcal = 0.0,
            ),
            intervalWorkoutSummary = WeeklyWorkoutStats(
                weekStartEpochMillisUtc = currentWeekInterval(todayStartEpochMillis).startEpochMillis,
                totalWorkouts = 0,
                totalDurationMinutes = 0,
                totalCaloriesBurned = 0,
                totalCardioDistanceKm = 0.0,
            ),
            hasTarget = false,
        ),
    )

    fun onSummaryModeSelected(mode: SummaryMode) {
        selectedSummaryMode.value = mode
    }

    fun onPreviousDayClick() {
        selectedDayStartEpochMillis.value = selectedDayStartEpochMillis.value - ONE_DAY_MILLIS
    }

    fun onNextDayClick() {
        val nextDay = selectedDayStartEpochMillis.value + ONE_DAY_MILLIS
        if (nextDay <= todayStartEpochMillis) {
            selectedDayStartEpochMillis.value = nextDay
        }
    }

    fun onPreviousWeekClick() {
        selectedInterval.value = selectedInterval.value.shiftByDays(-7)
    }

    fun onNextWeekClick() {
        selectedInterval.value = selectedInterval.value.shiftByDays(7)
    }

    fun onCurrentWeekClick() {
        selectedInterval.value = currentWeekInterval(todayStartEpochMillis)
    }

    fun onCustomIntervalSelected(rawStartEpochMillis: Long, rawEndEpochMillis: Long) {
        val start = rawStartEpochMillis.toDayStartEpochMillis()
        val end = rawEndEpochMillis.toDayStartEpochMillis()
        selectedInterval.value = IntervalSelection(
            startEpochMillis = minOf(start, end),
            endEpochMillis = maxOf(start, end),
        )
    }

    private fun dayStartEpochToWeekStartEpoch(dayStartEpochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(dayStartEpochMillis).atZone(zoneId).toLocalDate()
        var cursor = localDate
        while (cursor.dayOfWeek != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1)
        }
        return localDateToEpochMillis(cursor)
    }

    private fun currentWeekInterval(anchorDayStartEpochMillis: Long): IntervalSelection {
        val weekStart = dayStartEpochToWeekStartEpoch(anchorDayStartEpochMillis)
        val weekEnd = weekStart + (6L * ONE_DAY_MILLIS)
        return IntervalSelection(
            startEpochMillis = weekStart,
            endEpochMillis = weekEnd,
        )
    }

    private fun localDateToEpochMillis(localDate: LocalDate): Long {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun Long.toDayStartEpochMillis(): Long {
        val localDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        return localDateToEpochMillis(localDate)
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
    val summaryMode: SummaryMode,
    val selectedDayStartEpochMillis: Long,
    val isTodaySelected: Boolean,
    val selectedIntervalStartEpochMillis: Long,
    val selectedIntervalEndEpochMillis: Long,
    val dailySummary: DailySummary,
    val dailyWorkoutStats: DailyWorkoutStats,
    val intervalFoodSummary: WeeklyFoodSummary,
    val intervalWorkoutSummary: WeeklyWorkoutStats,
    val hasTarget: Boolean,
)

data class IntervalSelection(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
) {
    fun shiftByDays(days: Int): IntervalSelection {
        val delta = days.toLong() * ONE_DAY_MILLIS
        return copy(
            startEpochMillis = startEpochMillis + delta,
            endEpochMillis = endEpochMillis + delta,
        )
    }

    companion object {
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}

enum class SummaryMode {
    DAY,
    INTERVAL,
}

private data class SummaryHeader(
    val mode: SummaryMode,
    val selectedDayStart: Long,
    val interval: IntervalSelection,
)
