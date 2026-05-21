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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DailySummaryScreen(
    contentPadding: PaddingValues,
    uiState: SummaryUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
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

        if (!uiState.hasTarget) {
            item {
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
                        text = "Weekly Workout Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Workouts: ${uiState.weeklyStats.totalWorkouts}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Duration: ${uiState.weeklyStats.totalDurationMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Calories burned: ${uiState.weeklyStats.totalCaloriesBurned} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
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

private fun formatDay(dayStartEpochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return Instant.ofEpochMilli(dayStartEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun Double.format1(): String = String.format(java.util.Locale.US, "%.1f", this)
