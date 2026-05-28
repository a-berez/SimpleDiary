package com.example.simplediary.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    contentPadding: PaddingValues,
    uiState: SummaryUiState,
    onSummaryModeSelected: (SummaryMode) -> Unit,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    onCurrentWeekClick: () -> Unit,
    onCustomIntervalSelected: (Long, Long) -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    var isCustomRangeDialogVisible by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = uiState.selectedIntervalStartEpochMillis,
        initialSelectedEndDateMillis = uiState.selectedIntervalEndEpochMillis,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.summaryMode == SummaryMode.DAY,
                    onClick = { onSummaryModeSelected(SummaryMode.DAY) },
                    label = { Text("По дням") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = uiState.summaryMode == SummaryMode.INTERVAL,
                    onClick = { onSummaryModeSelected(SummaryMode.INTERVAL) },
                    label = { Text("Интервал") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (uiState.summaryMode == SummaryMode.DAY) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onPreviousDayClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Prev", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        text = formatDay(uiState.selectedDayStartEpochMillis),
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(
                        onClick = onNextDayClick,
                        enabled = !uiState.isTodaySelected,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Next", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onPreviousWeekClick, modifier = Modifier.weight(1f)) {
                        Text("− Неделя", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(onClick = onCurrentWeekClick, modifier = Modifier.weight(1f)) {
                        Text("Текущая", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(onClick = onNextWeekClick, modifier = Modifier.weight(1f)) {
                        Text("+ Неделя", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Интервал: ${formatInterval(uiState.selectedIntervalStartEpochMillis, uiState.selectedIntervalEndEpochMillis)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(onClick = { isCustomRangeDialogVisible = true }) {
                            Text("Выбрать произвольный интервал", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (uiState.summaryMode == SummaryMode.DAY && !uiState.hasTarget) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No daily target set yet",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Set your KBZHU target in Settings to track progress.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = onOpenSettingsClick) {
                                Text("Open Settings", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.summaryMode == SummaryMode.DAY) {
            item {
                MetricProgressCard(
                    title = "Calories",
                    actual = uiState.dailySummary.actualCaloriesKcal,
                    target = uiState.dailySummary.targetCaloriesKcal,
                    unit = "kcal",
                )
            }
            item {
                MetricProgressCard(
                    title = "Proteins",
                    actual = uiState.dailySummary.actualProteinsGrams,
                    target = uiState.dailySummary.targetProteinsGrams,
                    unit = "g",
                )
            }
            item {
                MetricProgressCard(
                    title = "Fats",
                    actual = uiState.dailySummary.actualFatsGrams,
                    target = uiState.dailySummary.targetFatsGrams,
                    unit = "g",
                )
            }
            item {
                MetricProgressCard(
                    title = "Carbs",
                    actual = uiState.dailySummary.actualCarbsGrams,
                    target = uiState.dailySummary.targetCarbsGrams,
                    unit = "g",
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Daily Workout Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Workouts: ${uiState.dailyWorkoutStats.totalWorkouts}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Duration: ${uiState.dailyWorkoutStats.totalDurationMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Calories burned: ${uiState.dailyWorkoutStats.totalCaloriesBurned} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Cardio distance: ${uiState.dailyWorkoutStats.totalCardioDistanceKm.format1()} km",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Сводка по еде",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Калории: ${uiState.intervalFoodSummary.actualCaloriesKcal.format1()} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Белки: ${uiState.intervalFoodSummary.actualProteinsGrams.format1()} g",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Жиры: ${uiState.intervalFoodSummary.actualFatsGrams.format1()} g",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Углеводы: ${uiState.intervalFoodSummary.actualCarbsGrams.format1()} g",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Дашборд активности",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DashboardMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Тренировки",
                                value = uiState.intervalWorkoutSummary.totalWorkouts.toString(),
                            )
                            DashboardMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Минуты",
                                value = uiState.intervalWorkoutSummary.totalDurationMinutes.toString(),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DashboardMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Ккал",
                                value = uiState.intervalWorkoutSummary.totalCaloriesBurned.toString(),
                            )
                            DashboardMetricTile(
                                modifier = Modifier.weight(1f),
                                label = "Кардио км",
                                value = uiState.intervalWorkoutSummary.totalCardioDistanceKm.format1(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (isCustomRangeDialogVisible) {
        DatePickerDialog(
            onDismissRequest = { isCustomRangeDialogVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onCustomIntervalSelected(start, end)
                        }
                        isCustomRangeDialogVisible = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null,
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCustomRangeDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }
}

@Composable
private fun MetricProgressCard(
    title: String,
    actual: Double,
    target: Double,
    unit: String,
) {
    val progress = when {
        target <= 0.0 -> 0f
        else -> (actual / target).coerceAtMost(1.5).toFloat()
    }
    val statusColor = when {
        target > 0.0 && actual > target -> MaterialTheme.colorScheme.error
        target > 0.0 && actual >= target * 0.9 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }
    val valueTextColor = when {
        target > 0.0 && actual > target -> MaterialTheme.colorScheme.error
        target > 0.0 && actual >= target * 0.9 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${actual.format1()} / ${target.format1()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = valueTextColor,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = statusColor,
            )
        }
    }
}

@Composable
private fun DashboardMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDay(dayStartEpochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return Instant.ofEpochMilli(dayStartEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun formatInterval(startEpochMillis: Long, endEpochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val zoneId = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startEpochMillis).atZone(zoneId).toLocalDate().format(formatter)
    val end = Instant.ofEpochMilli(endEpochMillis).atZone(zoneId).toLocalDate().format(formatter)
    return "$start - $end"
}

private fun Double.format1(): String = String.format(java.util.Locale.US, "%.1f", this)
