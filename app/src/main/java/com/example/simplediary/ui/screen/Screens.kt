package com.example.simplediary.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DailySummaryScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Daily Summary",
        subtitle = "Actual vs target KBZHU and weekly workout stats will be shown here.",
        contentPadding = contentPadding,
    )
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Settings",
        subtitle = "Backup, CSV export and daily KBZHU target controls will be added here.",
        contentPadding = contentPadding,
    )
}

@Composable
fun MealEditorScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Add/Edit Meal",
        subtitle = "Meal photo, text and nutrition rows editor.",
        contentPadding = contentPadding,
    )
}

@Composable
fun StateNoteEditorScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        title = "Add/Edit State Note",
        subtitle = "State note text, optional photo and timestamp editor.",
        contentPadding = contentPadding,
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
