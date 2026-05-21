package com.example.simplediary.data.local.model

data class FeedDbRow(
    val entryId: Long,
    val entryType: String,
    val timestampEpochMillis: Long,
    val title: String,
    val subtitle: String?,
    val expandedDetails: String?,
    val photoPath: String?,
)
