package com.example.simplediary.domain.model

data class FeedFilter(
    val entryTypes: Set<EntryType> = emptySet(),
    val workoutTypes: Set<WorkoutType> = emptySet(),
    val fromEpochMillisInclusive: Long? = null,
    val toEpochMillisInclusive: Long? = null,
)
