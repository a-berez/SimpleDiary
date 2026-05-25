package com.example.simplediary.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.simplediary.domain.model.EntryType
import com.example.simplediary.domain.model.FeedFilter
import com.example.simplediary.domain.repository.FeedRow
import com.example.simplediary.domain.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

class FeedViewModel(
    private val journalRepository: JournalRepository,
    context: Context,
) : ViewModel() {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val zoneId = ZoneId.systemDefault()

    private val filtersState = MutableStateFlow(loadFiltersFromPreferences())
    private val sortOrderState = MutableStateFlow(
        runCatching {
            FeedSortOrder.valueOf(
                preferences.getString(PREF_SORT_ORDER, FeedSortOrder.NEWEST_FIRST.name) ?: FeedSortOrder.NEWEST_FIRST.name
            )
        }.getOrDefault(FeedSortOrder.NEWEST_FIRST)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val feedItemsFlow = filtersState
        .map { it.toDomainFilter() }
        .flatMapLatest { domainFilter ->
            journalRepository.observeFeed(domainFilter)
        }.map { rows ->
            rows.map { row -> row.toUiModel() }
        }

    val uiState: StateFlow<FeedUiState> = combine(filtersState, feedItemsFlow, sortOrderState) { filters, items, sortOrder ->
        val sortedItems = when (sortOrder) {
            FeedSortOrder.NEWEST_FIRST -> items
            FeedSortOrder.OLDEST_FIRST -> items.asReversed()
        }
        FeedUiState(
            items = sortedItems,
            selectedEntryTypes = filters.entryTypes,
            selectedDateSelection = filters.dateSelection,
            sortOrder = sortOrder,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    fun onEntryTypeToggle(type: EntryType) {
        filtersState.value = filtersState.value.run {
            copy(
                entryTypes = if (entryTypes.contains(type)) {
                    entryTypes - type
                } else {
                    entryTypes + type
                }
            )
        }
    }

    fun onDateRangeOptionSelected(option: FeedDateRangeOption) {
        val nextSelection = filtersState.value.dateSelection.copy(option = option)
        filtersState.value = filtersState.value.copy(dateSelection = nextSelection)
        persistDateSelection(nextSelection)
    }

    fun onCustomDateRangeSelected(rawStartEpochMillis: Long, rawEndEpochMillis: Long) {
        val startEpochMillis = rawStartEpochMillis.toDayStartEpochMillis()
        val endEpochMillis = rawEndEpochMillis.toDayStartEpochMillis()
        val normalizedStart = minOf(startEpochMillis, endEpochMillis)
        val normalizedEnd = maxOf(startEpochMillis, endEpochMillis)
        val nextSelection = FeedDateSelection(
            option = FeedDateRangeOption.CUSTOM,
            customStartEpochMillis = normalizedStart,
            customEndEpochMillis = normalizedEnd,
        )
        filtersState.value = filtersState.value.copy(dateSelection = nextSelection)
        persistDateSelection(nextSelection)
    }

    fun onToggleSortOrder() {
        val next = when (sortOrderState.value) {
            FeedSortOrder.NEWEST_FIRST -> FeedSortOrder.OLDEST_FIRST
            FeedSortOrder.OLDEST_FIRST -> FeedSortOrder.NEWEST_FIRST
        }
        sortOrderState.value = next
        preferences.edit().putString(PREF_SORT_ORDER, next.name).apply()
    }

    private fun FeedFilters.toDomainFilter(): FeedFilter {
        val (startEpochMillis, endEpochMillisInclusive) = dateSelection.toEpochRange()
        return FeedFilter(
            entryTypes = entryTypes,
            fromEpochMillisInclusive = startEpochMillis,
            toEpochMillisInclusive = endEpochMillisInclusive,
        )
    }

    private fun FeedRow.toUiModel(): FeedItemUiModel {
        return FeedItemUiModel(
            id = id,
            type = type,
            timestampEpochMillis = timestampEpochMillis,
            previewText = title,
            details = subtitle,
            expandedDetails = expandedDetails,
            photoPath = photoPath,
        )
    }

    private fun FeedDateSelection.toEpochRange(nowMillis: Long = System.currentTimeMillis()): Pair<Long?, Long?> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when (option) {
            FeedDateRangeOption.ALL_TIME -> null to null
            FeedDateRangeOption.TODAY ->
                today.atStartOfDay(zoneId).toInstant().toEpochMilli() to null
            FeedDateRangeOption.LAST_7_DAYS ->
                today.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli() to null
            FeedDateRangeOption.LAST_30_DAYS ->
                today.minusDays(29).atStartOfDay(zoneId).toInstant().toEpochMilli() to null
            FeedDateRangeOption.CUSTOM -> {
                val start = customStartEpochMillis
                val end = customEndEpochMillis
                if (start == null || end == null) {
                    null to null
                } else {
                    start to (end + ONE_DAY_MILLIS - 1L)
                }
            }
        }
    }

    private fun loadFiltersFromPreferences(): FeedFilters {
        val dateOption = runCatching {
            FeedDateRangeOption.valueOf(
                preferences.getString(PREF_DATE_RANGE_OPTION, FeedDateRangeOption.ALL_TIME.name)
                    ?: FeedDateRangeOption.ALL_TIME.name
            )
        }.getOrDefault(FeedDateRangeOption.ALL_TIME)
        val customStart = preferences.getLong(PREF_CUSTOM_DATE_START, Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }
        val customEnd = preferences.getLong(PREF_CUSTOM_DATE_END, Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }
        val dateSelection = FeedDateSelection(
            option = if (dateOption == FeedDateRangeOption.CUSTOM && (customStart == null || customEnd == null)) {
                FeedDateRangeOption.ALL_TIME
            } else {
                dateOption
            },
            customStartEpochMillis = customStart,
            customEndEpochMillis = customEnd,
        )
        return FeedFilters(dateSelection = dateSelection)
    }

    private fun persistDateSelection(selection: FeedDateSelection) {
        val editor = preferences.edit()
            .putString(PREF_DATE_RANGE_OPTION, selection.option.name)
        if (selection.customStartEpochMillis != null) {
            editor.putLong(PREF_CUSTOM_DATE_START, selection.customStartEpochMillis)
        } else {
            editor.remove(PREF_CUSTOM_DATE_START)
        }
        if (selection.customEndEpochMillis != null) {
            editor.putLong(PREF_CUSTOM_DATE_END, selection.customEndEpochMillis)
        } else {
            editor.remove(PREF_CUSTOM_DATE_END)
        }
        editor.apply()
    }

    private fun Long.toDayStartEpochMillis(): Long {
        val localDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    companion object {
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
        private const val PREFS_NAME = "feed_preferences"
        private const val PREF_SORT_ORDER = "sort_order"
        private const val PREF_DATE_RANGE_OPTION = "date_range_option"
        private const val PREF_CUSTOM_DATE_START = "custom_date_start"
        private const val PREF_CUSTOM_DATE_END = "custom_date_end"

        fun factory(
            journalRepository: JournalRepository,
            context: Context,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return FeedViewModel(journalRepository, context) as T
                }
            }
        }
    }
}

data class FeedUiState(
    val items: List<FeedItemUiModel> = emptyList(),
    val selectedEntryTypes: Set<EntryType> = emptySet(),
    val selectedDateSelection: FeedDateSelection = FeedDateSelection(),
    val sortOrder: FeedSortOrder = FeedSortOrder.NEWEST_FIRST,
)

data class FeedItemUiModel(
    val id: Long,
    val type: EntryType,
    val timestampEpochMillis: Long,
    val previewText: String,
    val details: String? = null,
    val expandedDetails: String? = null,
    val photoPath: String? = null,
)

enum class FeedDateRangeOption(val title: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    ALL_TIME("All time"),
    CUSTOM("Custom range"),
}

enum class FeedSortOrder(val title: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
}

private data class FeedFilters(
    val entryTypes: Set<EntryType> = emptySet(),
    val dateSelection: FeedDateSelection = FeedDateSelection(),
)

data class FeedDateSelection(
    val option: FeedDateRangeOption = FeedDateRangeOption.ALL_TIME,
    val customStartEpochMillis: Long? = null,
    val customEndEpochMillis: Long? = null,
)
