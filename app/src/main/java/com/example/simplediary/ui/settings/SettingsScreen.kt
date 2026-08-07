package com.example.simplediary.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    uiState: SettingsUiState,
    onTargetCaloriesChanged: (String) -> Unit,
    onTargetProteinsChanged: (String) -> Unit,
    onTargetFatsChanged: (String) -> Unit,
    onTargetCarbsChanged: (String) -> Unit,
    onSaveTargetsClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Long, String) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onAddType: (Long, String) -> Unit,
    onRenameType: (Long, String) -> Unit,
    onDeleteType: (Long) -> Unit,
    onAddNoteCategory: (String) -> Unit,
    onRenameNoteCategory: (String, String) -> Unit,
    onDeleteNoteCategory: (String) -> Unit,
    onOpenFoodLibrary: () -> Unit,
    onExportCsv: (Uri) -> Unit,
    onImportGrowFoodCsv: (Uri) -> Unit,
    onBackupZip: (Uri) -> Unit,
    onRestoreZip: (Uri) -> Unit,
    events: SharedFlow<SettingsEvent>,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedCategories by remember { mutableStateOf(setOf<Long>()) }
    var nameDialogState by remember { mutableStateOf<NameDialogState?>(null) }
    var confirmDialogState by remember { mutableStateOf<ConfirmDialogState?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(onExportCsv)
    }
    val importGrowFoodCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportGrowFoodCsv)
    }
    val backupZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(onBackupZip)
    }
    val restoreZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            confirmDialogState = ConfirmDialogState(
                title = "Restore from ZIP",
                message = "Restoring backup will overwrite current data. Continue?",
                onConfirm = {
                    pendingRestoreUri?.let(onRestoreZip)
                    pendingRestoreUri = null
                },
            )
        }
    }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

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
                text = "Settings",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "1. Daily KBZHU targets", fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = uiState.targetCalories,
                        onValueChange = onTargetCaloriesChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Calories") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.targetProteins,
                            onValueChange = onTargetProteinsChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Proteins") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        OutlinedTextField(
                            value = uiState.targetFats,
                            onValueChange = onTargetFatsChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text("Fats") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    OutlinedTextField(
                        value = uiState.targetCarbs,
                        onValueChange = onTargetCarbsChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Carbs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Button(
                        onClick = onSaveTargetsClick,
                        enabled = !uiState.isBusy,
                    ) {
                        Text("Save targets")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "2. Workout categories and types", fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = {
                            nameDialogState = NameDialogState(
                                title = "Add category",
                                initialValue = "",
                                onConfirm = { name -> onAddCategory(name) },
                            )
                        }
                    ) {
                        Text("Add category", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (uiState.categories.isEmpty()) {
                        Text("No categories yet")
                    } else {
                        uiState.categories.forEach { category ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = category.name,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Medium,
                                        )
                                        TextButton(
                                            onClick = {
                                                expandedCategories = if (expandedCategories.contains(category.id)) {
                                                    expandedCategories - category.id
                                                } else {
                                                    expandedCategories + category.id
                                                }
                                            }
                                        ) {
                                            Text(if (expandedCategories.contains(category.id)) "Hide types" else "Show types")
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                nameDialogState = NameDialogState(
                                                    title = "Rename category",
                                                    initialValue = category.name,
                                                    onConfirm = { name -> onRenameCategory(category.id, name) },
                                                )
                                            }
                                        ) { Text("Rename", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        TextButton(
                                            onClick = {
                                                confirmDialogState = ConfirmDialogState(
                                                    title = "Delete category",
                                                    message = "Delete category '${category.name}' and all its types?",
                                                    onConfirm = { onDeleteCategory(category.id) },
                                                )
                                            }
                                        ) { Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        TextButton(
                                            onClick = {
                                                nameDialogState = NameDialogState(
                                                    title = "Add type",
                                                    initialValue = "",
                                                    onConfirm = { name -> onAddType(category.id, name) },
                                                )
                                            }
                                        ) { Text("Add type", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    }

                                    if (expandedCategories.contains(category.id)) {
                                        if (category.types.isEmpty()) {
                                            Text("No types")
                                        } else {
                                            category.types.forEach { type ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Text(
                                                        text = "• ${type.name}",
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            nameDialogState = NameDialogState(
                                                                title = "Rename type",
                                                                initialValue = type.name,
                                                                onConfirm = { name -> onRenameType(type.id, name) },
                                                            )
                                                        }
                                                    ) { Text("Rename", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                                    TextButton(
                                                        onClick = {
                                                            confirmDialogState = ConfirmDialogState(
                                                                title = "Delete type",
                                                                message = "Delete type '${type.name}'?",
                                                                onConfirm = { onDeleteType(type.id) },
                                                            )
                                                        }
                                                    ) { Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "3. State note categories", fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = {
                            nameDialogState = NameDialogState(
                                title = "Add state note category",
                                initialValue = "",
                                onConfirm = onAddNoteCategory,
                            )
                        }
                    ) {
                        Text("Add category", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    uiState.noteCategories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                            )
                            TextButton(
                                onClick = {
                                    nameDialogState = NameDialogState(
                                        title = "Rename state note category",
                                        initialValue = category,
                                        onConfirm = { newName -> onRenameNoteCategory(category, newName) },
                                    )
                                }
                            ) { Text("Rename", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            TextButton(
                                onClick = {
                                    confirmDialogState = ConfirmDialogState(
                                        title = "Delete state note category",
                                        message = "Delete category '$category'?",
                                        onConfirm = { onDeleteNoteCategory(category) },
                                    )
                                }
                            ) { Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "4. Food library", fontWeight = FontWeight.Medium)
                    Button(
                        onClick = onOpenFoodLibrary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Manage food library", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            importGrowFoodCsvLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "text/csv",
                                    "application/csv",
                                    "application/vnd.ms-excel",
                                    "application/octet-stream",
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Import Grow Food CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "5. Data management", fontWeight = FontWeight.Medium)
                    Button(
                        onClick = { exportCsvLauncher.launch("simple_diary_export.csv.zip") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Export CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { backupZipLauncher.launch("simple_diary_backup.zip") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Backup ZIP", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Restore ZIP", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp),
    )

    nameDialogState?.let { dialog ->
        NameInputDialog(
            title = dialog.title,
            initialValue = dialog.initialValue,
            onDismiss = { nameDialogState = null },
            onConfirm = { name ->
                dialog.onConfirm(name)
                nameDialogState = null
            },
        )
    }

    confirmDialogState?.let { dialog ->
        ConfirmDialog(
            title = dialog.title,
            message = dialog.message,
            onDismiss = { confirmDialogState = null },
            onConfirm = {
                dialog.onConfirm()
                confirmDialogState = null
            },
        )
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private data class NameDialogState(
    val title: String,
    val initialValue: String,
    val onConfirm: (String) -> Unit,
)

private data class ConfirmDialogState(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit,
)
