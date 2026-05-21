package com.example.simplediary.ui.workout

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun WorkoutEditorScreen(
    contentPadding: PaddingValues,
    uiState: WorkoutUiState,
    onTimestampChanged: (Long) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onTypeSelected: (Long?) -> Unit,
    onDurationChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    events: SharedFlow<WorkoutEvent>,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is WorkoutEvent.Saved -> onNavigateBack()
                is WorkoutEvent.Deleted -> onNavigateBack()
                is WorkoutEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Text(
                    text = if (uiState.isEditMode) "Edit Workout" else "Add Workout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            item {
                WorkoutDateTimeSection(
                    timestampEpochMillis = uiState.timestampEpochMillis,
                    onTimestampChanged = onTimestampChanged,
                )
            }

            item {
                CategoryPicker(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = onCategorySelected,
                )
            }

            item {
                TypePicker(
                    availableTypes = uiState.availableTypes,
                    selectedTypeId = uiState.selectedTypeId,
                    onTypeSelected = onTypeSelected,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.durationMinutes,
                    onValueChange = onDurationChanged,
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.caloriesBurned,
                    onValueChange = onCaloriesChanged,
                    label = { Text("Calories burned (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChanged,
                    label = { Text("Text note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            }

            item {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isBusy,
                ) {
                    Text("Save", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (uiState.isEditMode) {
                item {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isBusy,
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                        Text(" Delete", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )

        if (uiState.isBusy) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun WorkoutDateTimeSection(
    timestampEpochMillis: Long,
    onTimestampChanged: (Long) -> Unit,
) {
    val context = LocalContext.current
    val calendar = remember(timestampEpochMillis) {
        Calendar.getInstance().apply { timeInMillis = timestampEpochMillis }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Date and time",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = formatDateTime(timestampEpochMillis),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = timestampEpochMillis
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            onTimestampChanged(updated.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    ).show()
                }
            ) { Text("Pick date", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val updated = Calendar.getInstance().apply {
                                timeInMillis = timestampEpochMillis
                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                set(Calendar.MINUTE, minute)
                            }
                            onTimestampChanged(updated.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true,
                    ).show()
                }
            ) { Text("Pick time", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<com.example.simplediary.data.local.entity.WorkoutCategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select category"

    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        ) {
            Text("Category: $selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onCategorySelected(category.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun TypePicker(
    availableTypes: List<com.example.simplediary.data.local.entity.WorkoutTypeEntity>,
    selectedTypeId: Long?,
    onTypeSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = availableTypes.firstOrNull { it.id == selectedTypeId }?.name ?: "No type"

    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        ) {
            Text("Type: $selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("No type") },
                onClick = {
                    expanded = false
                    onTypeSelected(null)
                },
            )
            availableTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        expanded = false
                        onTypeSelected(type.id)
                    },
                )
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
