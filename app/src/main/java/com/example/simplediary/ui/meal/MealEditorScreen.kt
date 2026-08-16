package com.example.simplediary.ui.meal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.simplediary.data.local.entity.FoodItemSource
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditorScreen(
    contentPadding: PaddingValues,
    uiState: MealUiState,
    onTimestampChanged: (Long) -> Unit,
    onNoteChanged: (String) -> Unit,
    onHungerBeforeChanged: (Int?) -> Unit,
    onSatietyAfterChanged: (Int?) -> Unit,
    onRowChanged: (NutritionRowInput) -> Unit,
    onDeleteRow: (String) -> Unit,
    onAddRow: () -> Unit,
    onFoodSearchQueryChanged: (String) -> Unit,
    onFoodItemSelected: (Long, Double) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    events: kotlinx.coroutines.flow.SharedFlow<MealEvent>,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    var isFoodLibraryVisible by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val outputUri = result.data?.let { intent -> UCrop.getOutput(intent) } ?: pendingCropOutputUri
            if (outputUri != null) {
                onPhotoPicked(outputUri)
            }
            pendingCropOutputUri = null
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data?.let { intent -> UCrop.getError(intent) }
            error?.let { throwable ->
                android.util.Log.e("MealEditorScreen", "Crop failed", throwable)
            }
            pendingCropOutputUri = null
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { source ->
                pendingCropOutputUri = launchCrop(context, source, cropLauncher::launch)
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { source ->
            pendingCropOutputUri = launchCrop(context, source, cropLauncher::launch)
        }
    }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is MealEvent.Saved -> onNavigateBack()
                is MealEvent.Deleted -> onNavigateBack()
                is MealEvent.Error -> snackbarHostState.showSnackbar(event.message)
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
                    text = if (uiState.isEditMode) "Edit Meal" else "Add Meal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            item {
                MealDateTimeSection(
                    timestampEpochMillis = uiState.timestampEpochMillis,
                    onTimestampChanged = onTimestampChanged,
                )
            }

            item {
                MealPhotoSection(
                    photoPath = uiState.photoPath,
                    onPickFromGallery = { galleryLauncher.launch("image/*") },
                    onTakePhoto = {
                        val uri = createCameraOutputUri(context)
                        pendingCameraUri = uri
                        takePhotoLauncher.launch(uri)
                    },
                    onRemovePhoto = onRemovePhoto,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Text note (optional)") },
                    minLines = 3,
                    maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Шкала голода / сытости",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    MealScaleSection(
                        title = "До приёма",
                        selected = uiState.hungerBefore,
                        onSelected = onHungerBeforeChanged,
                    )
                    MealScaleSection(
                        title = "После приёма",
                        selected = uiState.satietyAfter,
                        onSelected = onSatietyAfterChanged,
                    )
                }
            }

            item {
                Text(
                    text = "Nutrition rows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            items(
                items = uiState.nutritionRows,
                key = { it.localId },
            ) { row ->
                NutritionRowEditor(
                    row = row,
                    onRowChanged = onRowChanged,
                    onDeleteRow = onDeleteRow,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isFoodLibraryVisible = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("From library", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onAddRow,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Manual row", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                MealTotalsSection(rows = uiState.nutritionRows)
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
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        if (isFoodLibraryVisible) {
            ModalBottomSheet(
                onDismissRequest = { isFoodLibraryVisible = false },
            ) {
                FoodLibrarySheet(
                    query = uiState.foodSearchQuery,
                    items = uiState.foodItems,
                    onQueryChanged = onFoodSearchQueryChanged,
                    onFoodItemSelected = { foodItemId, portionMultiplier ->
                        onFoodItemSelected(foodItemId, portionMultiplier)
                        isFoodLibraryVisible = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FoodLibrarySheet(
    query: String,
    items: List<FoodItemUiModel>,
    onQueryChanged: (String) -> Unit,
    onFoodItemSelected: (Long, Double) -> Unit,
) {
    var portionText by remember { mutableStateOf("1") }
    val portionMultiplier = portionText.parsePortionMultiplier()
    val showPortionField = items.any { it.source != FoodItemSource.GROW_FOOD }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Food library",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search") },
            singleLine = true,
            leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        )
        if (showPortionField) {
            OutlinedTextField(
                value = portionText,
                onValueChange = { portionText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Множитель (для ручных)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = portionMultiplier == null,
                supportingText = {
                    Text("Для блюд Grow Food не используется")
                },
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "No saved food items yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = items,
                    key = { it.id },
                ) { item ->
                    val isGrowFood = item.source == FoodItemSource.GROW_FOOD
                    val effectiveMultiplier = if (isGrowFood) 1.0 else (portionMultiplier ?: 1.0)
                    FoodLibraryItemRow(
                        item = item,
                        portionMultiplier = effectiveMultiplier,
                        isAddEnabled = isGrowFood || portionMultiplier != null,
                        onClick = {
                            onFoodItemSelected(
                                item.id,
                                if (isGrowFood) 1.0 else (portionMultiplier ?: 1.0),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodLibraryItemRow(
    item: FoodItemUiModel,
    portionMultiplier: Double,
    isAddEnabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        text = item.macroSummary(portionMultiplier),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onClick,
                    enabled = isAddEnabled,
                ) {
                    Text("Add", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MealScaleSection(
    title: String,
    selected: Int?,
    onSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            if (selected != null) {
                TextButton(onClick = { onSelected(null) }) {
                    Text("Не указано")
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MEAL_SCALE_MAX - MEAL_SCALE_MIN + 1) { index ->
                val value = MEAL_SCALE_MIN + index
                FilterChip(
                    selected = selected == value,
                    onClick = {
                        onSelected(if (selected == value) null else value)
                    },
                    label = { Text(value.toString()) },
                )
            }
        }
    }
}

@Composable
private fun MealDateTimeSection(
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
private fun MealPhotoSection(
    photoPath: String?,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Photo (optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickFromGallery) {
                Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Gallery", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onTakePhoto) {
                Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Camera", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (photoPath != null) {
                OutlinedButton(onClick = onRemovePhoto) {
                    Text("Remove", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (photoPath != null) {
            val bitmap = rememberBitmap(photoPath)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Meal photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun NutritionRowEditor(
    row: NutritionRowInput,
    onRowChanged: (NutritionRowInput) -> Unit,
    onDeleteRow: (String) -> Unit,
) {
    Card(
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Item",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDeleteRow(row.localId) }) {
                    Icon(
                        imageVector = Icons.Outlined.RemoveCircleOutline,
                        contentDescription = "Delete row",
                    )
                }
            }

            OutlinedTextField(
                value = row.itemName,
                onValueChange = { onRowChanged(row.copy(sourceFoodItemId = null, itemName = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name / description") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.calories,
                    onValueChange = { onRowChanged(row.copy(sourceFoodItemId = null, calories = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = row.proteins,
                    onValueChange = { onRowChanged(row.copy(sourceFoodItemId = null, proteins = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Proteins") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.fats,
                    onValueChange = { onRowChanged(row.copy(sourceFoodItemId = null, fats = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Fats") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = row.carbs,
                    onValueChange = { onRowChanged(row.copy(sourceFoodItemId = null, carbs = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Carbs") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
    }
}

@Composable
private fun MealTotalsSection(rows: List<NutritionRowInput>) {
    val totals = remember(rows) {
        rows.fold(MealTotals()) { acc, row ->
            acc.copy(
                calories = acc.calories + (row.calories.toDoubleOrNull() ?: 0.0),
                proteins = acc.proteins + (row.proteins.toDoubleOrNull() ?: 0.0),
                fats = acc.fats + (row.fats.toDoubleOrNull() ?: 0.0),
                carbs = acc.carbs + (row.carbs.toDoubleOrNull() ?: 0.0),
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Calories: ${totals.calories.format1()} kcal",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Proteins: ${totals.proteins.format1()} g, Fats: ${totals.fats.format1()} g, Carbs: ${totals.carbs.format1()} g",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private data class MealTotals(
    val calories: Double = 0.0,
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbs: Double = 0.0,
)

@Composable
private fun rememberBitmap(path: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        val file = File(path)
        if (!file.exists()) {
            value = null
            return@produceState
        }
        val decoded = BitmapFactory.decodeFile(path)
        value = decoded?.asImageBitmap()
    }
    return bitmap
}

private fun createCameraOutputUri(context: android.content.Context): Uri {
    val cameraDir = File(context.cacheDir, "camera_cache").apply { mkdirs() }
    val outputFile = File(cameraDir, "camera_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
}

private fun launchCrop(
    context: android.content.Context,
    sourceUri: Uri,
    launchIntent: (android.content.Intent) -> Unit,
): Uri {
    val destination = Uri.fromFile(File(context.cacheDir, "crop_${UUID.randomUUID()}.jpg"))
    val options = UCrop.Options().apply {
        setHideBottomControls(false)
        setFreeStyleCropEnabled(true)
    }
    val intent = UCrop.of(sourceUri, destination)
        .withOptions(options)
        .getIntent(context)
    launchIntent(intent)
    return destination
}

private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun Double.format1(): String = String.format(java.util.Locale.US, "%.1f", this)

private fun FoodItemUiModel.macroSummary(portionMultiplier: Double): String {
    return "K: ${caloriesKcal.formatMacro(portionMultiplier)}  P: ${proteinsGrams.formatMacro(portionMultiplier)}  F: ${fatsGrams.formatMacro(portionMultiplier)}  C: ${carbsGrams.formatMacro(portionMultiplier)}"
}

private fun Double?.formatMacro(portionMultiplier: Double): String {
    return this?.let { (it * portionMultiplier).format1() } ?: "-"
}

private fun String.parsePortionMultiplier(): Double? {
    return trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }
}
