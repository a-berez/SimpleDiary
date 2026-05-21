package com.example.simplediary.data.local.db

import androidx.room.TypeConverter
import com.example.simplediary.domain.model.WorkoutType

class DbTypeConverters {
    @TypeConverter
    fun fromWorkoutType(value: WorkoutType?): String? = value?.name

    @TypeConverter
    fun toWorkoutType(value: String?): WorkoutType? {
        return value?.let { WorkoutType.valueOf(it) }
    }
}
