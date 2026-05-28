package com.example.simplediary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "state_notes",
    indices = [
        Index(value = ["timestampEpochMillis"]),
    ],
)
data class StateNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val category: String,
    val text: String,
    val photoPath: String?,
    val timestampEpochMillis: Long,
)
