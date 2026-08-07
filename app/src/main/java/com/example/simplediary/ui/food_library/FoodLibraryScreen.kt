package com.example.simplediary.ui.food_library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FoodLibraryScreen(
    contentPadding: PaddingValues,
    uiState: FoodLibraryUiState,
    onSearchQueryChanged: (String) -> Unit,
    onUpdateFoodItem: (FoodItemEditInput) -> Unit,
    onDeleteFoodItem: (Long) -> Unit,
    events: SharedFlow<FoodLibraryEvent>,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editInput by remember { mutableStateOf<FoodItemEditInput?>(null) }
    var itemToDelete by remember { mutableStateOf<FoodLibraryItemUi?>(null) }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is FoodLibraryEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Text(
                        text = "Food library",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                    ) {
                        Text(
                            text = "No food items found.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                items(
                    items = uiState.items,
                    key = { it.id },
                ) { item ->
                    FoodItemCard(
                        item = item,
                        onEditClick = { editInput = item.toEditInput() },
                        onDeleteClick = { itemToDelete = item },
                    )
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp),
    )

    editInput?.let { input ->
        FoodItemEditDialog(
            input = input,
            onInputChanged = { editInput = it },
            onDismiss = { editInput = null },
            onSave = {
                onUpdateFoodItem(it)
                editInput = null
            },
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete food item") },
            text = { Text("Delete '${item.name}' from the library? Diary entries already created from it will stay unchanged.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFoodItem(item.id)
                        itemToDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun FoodItemCard(
    item: FoodLibraryItemUi,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = item.macroSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${item.sourceLabel()} · used ${item.useCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
            if (item.ingredients.isNotBlank()) {
                Text(
                    text = item.ingredients,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FoodItemEditDialog(
    input: FoodItemEditInput,
    onInputChanged: (FoodItemEditInput) -> Unit,
    onDismiss: () -> Unit,
    onSave: (FoodItemEditInput) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit food item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input.name,
                    onValueChange = { onInputChanged(input.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroInputField(
                        value = input.calories,
                        label = "Calories",
                        modifier = Modifier.weight(1f),
                        onValueChanged = { onInputChanged(input.copy(calories = it)) },
                    )
                    MacroInputField(
                        value = input.proteins,
                        label = "Proteins",
                        modifier = Modifier.weight(1f),
                        onValueChanged = { onInputChanged(input.copy(proteins = it)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroInputField(
                        value = input.fats,
                        label = "Fats",
                        modifier = Modifier.weight(1f),
                        onValueChanged = { onInputChanged(input.copy(fats = it)) },
                    )
                    MacroInputField(
                        value = input.carbs,
                        label = "Carbs",
                        modifier = Modifier.weight(1f),
                        onValueChanged = { onInputChanged(input.copy(carbs = it)) },
                    )
                }
                MacroInputField(
                    value = input.weight,
                    label = "Weight (g)",
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = { onInputChanged(input.copy(weight = it)) },
                )
                OutlinedTextField(
                    value = input.ingredients,
                    onValueChange = { onInputChanged(input.copy(ingredients = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ingredients") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MacroInputField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private fun FoodLibraryItemUi.macroSummary(): String {
    return "K: ${calories.ifBlank { "-" }}  P: ${proteins.ifBlank { "-" }}  F: ${fats.ifBlank { "-" }}  C: ${carbs.ifBlank { "-" }}"
}

private fun FoodLibraryItemUi.sourceLabel(): String {
    return when (source) {
        "GROW_FOOD" -> "Grow Food"
        "MANUAL" -> "Manual"
        else -> source
    }
}
