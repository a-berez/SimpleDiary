package com.example.simplediary.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.simplediary.data.local.dao.DailyTargetDao
import com.example.simplediary.data.local.dao.FeedDao
import com.example.simplediary.data.local.dao.MealDao
import com.example.simplediary.data.local.dao.NutritionRowDao
import com.example.simplediary.data.local.dao.StateNoteDao
import com.example.simplediary.data.local.dao.WorkoutDao
import com.example.simplediary.data.local.entity.DailyTargetEntity
import com.example.simplediary.data.local.entity.MealEntity
import com.example.simplediary.data.local.entity.NutritionRowEntity
import com.example.simplediary.data.local.entity.StateNoteEntity
import com.example.simplediary.data.local.entity.WorkoutEntity

@Database(
    entities = [
        MealEntity::class,
        NutritionRowEntity::class,
        WorkoutEntity::class,
        StateNoteEntity::class,
        DailyTargetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DbTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun nutritionRowDao(): NutritionRowDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun stateNoteDao(): StateNoteDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun feedDao(): FeedDao
}
