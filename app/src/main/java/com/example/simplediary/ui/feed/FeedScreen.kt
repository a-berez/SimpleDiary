package com.example.simplediary.ui.feed

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.simplediary.domain.model.EntryType
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    contentPadding: PaddingValues,
    uiState: FeedUiState,
    onEntryTypeToggle: (EntryType) -> Unit,
    onDateRangeOptionSelected: (FeedDateRangeOption) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    onToggleSortOrder: () -> Unit,
    onAddMealClick: () -> Unit,
    onAddWorkoutClick: () -> Unit,
    onAddStateNoteClick: () -> Unit,
    onMealClick: (Long) -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onStateNoteClick: (Long) -> Unit,
) {
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isCustomDateDialogVisible by remember { mutableStateOf(false) }
    val expandedIds = remember { mutableStateOf(setOf<String>()) }
    val currentFilter = uiState.selectedEntryTypes to uiState.selectedDateSelection
    LaunchedEffect(currentFilter) {
        expandedIds.value = emptySet()
    }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = uiState.selectedDateSelection.customStartEpochMillis,
        initialSelectedEndDateMillis = uiState.selectedDateSelection.customEndEpochMillis,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Feed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    TextButton(onClick = onToggleSortOrder) {
                        Icon(imageVector = Icons.Outlined.Sort, contentDescription = null)
                        Text(
                            text = uiState.sortOrder.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            item {
                FeedFiltersBar(
                    uiState = uiState,
                    onEntryTypeToggle = onEntryTypeToggle,
                    onDateRangeOptionSelected = onDateRangeOptionSelected,
                    onCustomRangeClick = { isCustomDateDialogVisible = true },
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "No entries found for selected filters.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.items,
                    key = { _, item -> "${item.type.name}_${item.id}" },
                ) { index, item ->
                    val previousItem = uiState.items.getOrNull(index - 1)
                    val isNewDayGroup = previousItem == null || !isSameDay(
                        first = previousItem.timestampEpochMillis,
                        second = item.timestampEpochMillis,
                    )
                    if (isNewDayGroup) {
                        FeedDaySeparator(
                            label = item.timestampEpochMillis.toDayHeaderLabel(),
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp),
                        )
                    }
                    FeedEntryCard(
                        item = item,
                        expanded = expandedIds.value.contains("${item.type.name}_${item.id}"),
                        onToggleExpanded = {
                            val key = "${item.type.name}_${item.id}"
                            expandedIds.value = if (expandedIds.value.contains(key)) {
                                expandedIds.value - key
                            } else {
                                expandedIds.value + key
                            }
                        },
                        onClick = {
                            when (item.type) {
                                EntryType.MEAL -> onMealClick(item.id)
                                EntryType.WORKOUT -> onWorkoutClick(item.id)
                                EntryType.STATE_NOTE -> onStateNoteClick(item.id)
                            }
                        },
                    )
                }
            }
        }

        if (isCustomDateDialogVisible) {
            DatePickerDialog(
                onDismissRequest = { isCustomDateDialogVisible = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                onCustomDateRangeSelected(start, end)
                            }
                            isCustomDateDialogVisible = false
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null,
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isCustomDateDialogVisible = false }) {
                        Text("Cancel")
                    }
                },
            ) {
                DateRangePicker(state = dateRangePickerState)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            FloatingActionButton(onClick = { isFabMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add entry",
                )
            }
            DropdownMenu(
                expanded = isFabMenuExpanded,
                onDismissRequest = { isFabMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Add Meal") },
                    onClick = {
                        isFabMenuExpanded = false
                        onAddMealClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Fastfood,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text("Add Workout") },
                    onClick = {
                        isFabMenuExpanded = false
                        onAddWorkoutClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text("Add State Note") },
                    onClick = {
                        isFabMenuExpanded = false
                        onAddStateNoteClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Mood,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedFiltersBar(
    uiState: FeedUiState,
    onEntryTypeToggle: (EntryType) -> Unit,
    onDateRangeOptionSelected: (FeedDateRangeOption) -> Unit,
    onCustomRangeClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Entry type",
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EntryType.entries.forEach { entryType ->
                FilterChip(
                    selected = uiState.selectedEntryTypes.contains(entryType),
                    onClick = { onEntryTypeToggle(entryType) },
                    label = { Text(entryType.displayName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(entryType.icon(), contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = "Date range",
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                FeedDateRangeOption.TODAY,
                FeedDateRangeOption.LAST_7_DAYS,
                FeedDateRangeOption.LAST_30_DAYS,
            ).forEach { range ->
                FilterChip(
                    selected = uiState.selectedDateSelection.option == range,
                    onClick = { onDateRangeOptionSelected(range) },
                    label = { Text(range.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.selectedDateSelection.option == FeedDateRangeOption.ALL_TIME,
                onClick = { onDateRangeOptionSelected(FeedDateRangeOption.ALL_TIME) },
                label = { Text(FeedDateRangeOption.ALL_TIME.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = uiState.selectedDateSelection.option == FeedDateRangeOption.CUSTOM,
                onClick = onCustomRangeClick,
                label = {
                    Text(
                        uiState.selectedDateSelection.customChipLabel(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.weight(2f),
            )
        }

        Text(
            text = "Selected: ${uiState.selectedEntryTypesLabel()}, ${uiState.selectedDateSelection.selectedLabel()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedEntryCard(
    item: FeedItemUiModel,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = item.type.icon(),
                    contentDescription = item.type.displayName(),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatDateTime(item.timestampEpochMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.previewText.ifBlank { item.type.displayName() },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.details?.takeIf { it.isNotBlank() }?.let { details ->
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val expandedText = when (item.type) {
                                EntryType.STATE_NOTE -> item.previewText
                                else -> item.expandedDetails ?: item.details.orEmpty()
                            }.trim()
                            if (expandedText.isNotEmpty()) {
                                Text(
                                    text = expandedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            OutlinedButton(
                                onClick = onClick,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                                Text("Edit", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            item.photoPath?.let { path ->
                FeedPhoto(photoPath = path)
            }
        }
    }
}

@Composable
private fun FeedPhoto(photoPath: String) {
    val photoData = rememberFeedPhoto(photoPath)
    if (photoData != null) {
        Image(
            bitmap = photoData.bitmap,
            contentDescription = "Entry photo",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(photoData.aspectRatio)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun rememberFeedPhoto(photoPath: String): FeedPhotoData? {
    val data by produceState<FeedPhotoData?>(initialValue = null, photoPath) {
        val file = File(photoPath)
        if (!file.exists()) {
            value = null
            return@produceState
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            value = null
            return@produceState
        }

        val targetSize = 1080
        val inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetSize)
        val decoded = BitmapFactory.decodeFile(
            photoPath,
            BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        )
        value = decoded?.let {
            FeedPhotoData(
                bitmap = it.asImageBitmap(),
                aspectRatio = (bounds.outWidth.toFloat() / bounds.outHeight.toFloat())
                    .takeIf { ratio -> ratio.isFinite() && ratio > 0f }
                    ?: (16f / 9f),
            )
        }
    }
    return data
}

private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (max(w, h) / 2 >= reqSize) {
        sample *= 2
        w /= 2
        h /= 2
    }
    return sample.coerceAtLeast(1)
}

private data class FeedPhotoData(
    val bitmap: ImageBitmap,
    val aspectRatio: Float,
)

private fun EntryType.icon(): ImageVector = when (this) {
    EntryType.MEAL -> Icons.Outlined.Fastfood
    EntryType.WORKOUT -> Icons.AutoMirrored.Outlined.DirectionsRun
    EntryType.STATE_NOTE -> Icons.Outlined.Mood
}

private fun EntryType.displayName(): String = when (this) {
    EntryType.MEAL -> "Meal"
    EntryType.WORKOUT -> "Workout"
    EntryType.STATE_NOTE -> "State note"
}

private fun FeedUiState.selectedEntryTypesLabel(): String {
    return if (selectedEntryTypes.isEmpty()) "all types" else selectedEntryTypes.joinToString { it.displayName() }
}

private fun FeedDateSelection.selectedLabel(): String {
    return when (option) {
        FeedDateRangeOption.CUSTOM -> customChipLabel()
        else -> option.title
    }
}

private fun FeedDateSelection.customChipLabel(): String {
    if (option != FeedDateRangeOption.CUSTOM) return FeedDateRangeOption.CUSTOM.title
    val start = customStartEpochMillis
    val end = customEndEpochMillis
    if (start == null || end == null) return FeedDateRangeOption.CUSTOM.title
    return "${start.formatDayShort()} — ${end.formatDayShort()}"
}

private fun formatDateTime(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(FEED_DATE_TIME_FORMATTER)
}

@Composable
private fun FeedDaySeparator(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

private fun isSameDay(first: Long, second: Long): Boolean {
    val zoneId = ZoneId.systemDefault()
    val firstDay = Instant.ofEpochMilli(first).atZone(zoneId).toLocalDate()
    val secondDay = Instant.ofEpochMilli(second).atZone(zoneId).toLocalDate()
    return firstDay == secondDay
}

private fun Long.toDayHeaderLabel(now: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
    val zoneId = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    return when (day) {
        now -> "Today"
        now.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }
}

private fun Long.formatDayShort(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd MMM"))
}

private val FEED_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

