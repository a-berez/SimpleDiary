package com.example.simplediary.ui.state_note

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@Composable
fun StateNoteEditorScreen(
    contentPadding: PaddingValues,
    uiState: StateNoteUiState,
    onTimestampChanged: (Long) -> Unit,
    onTextChanged: (String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    events: SharedFlow<StateNoteEvent>,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCropOutputUri by remember { mutableStateOf<Uri?>(null) }

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
                // Surface error text via shared flow from VM is unnecessary for local crop errors.
                android.util.Log.e("StateNoteEditorScreen", "Crop failed", throwable)
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
                is StateNoteEvent.Saved -> onNavigateBack()
                is StateNoteEvent.Deleted -> onNavigateBack()
                is StateNoteEvent.Error -> snackbarHostState.showSnackbar(event.message)
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
                    text = if (uiState.isEditMode) "Edit State Note" else "Add State Note",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            item {
                StateNoteDateTimeSection(
                    timestampEpochMillis = uiState.timestampEpochMillis,
                    onTimestampChanged = onTimestampChanged,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Photo (optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Gallery", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = createCameraOutputUri(context)
                                pendingCameraUri = uri
                                takePhotoLauncher.launch(uri)
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Camera", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (uiState.photoPath != null) {
                            OutlinedButton(onClick = onRemovePhoto) {
                                Text("Remove", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    uiState.photoPath?.let { path ->
                        val bitmap = rememberBitmap(path)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "State note photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = onTextChanged,
                    label = { Text("Text note *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun StateNoteDateTimeSection(
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
private fun rememberBitmap(path: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        val file = File(path)
        if (!file.exists()) {
            value = null
            return@produceState
        }
        value = BitmapFactory.decodeFile(path)?.asImageBitmap()
    }
    return bitmap
}

private fun createCameraOutputUri(context: android.content.Context): Uri {
    val outputFile = File(context.cacheDir, "camera_${UUID.randomUUID()}.jpg")
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
