package com.example.simplediary.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.simplediary.data.local.dao.DailyTargetDao
import com.example.simplediary.data.local.dao.FeedDao
import com.example.simplediary.data.local.dao.MealDao
import com.example.simplediary.data.local.dao.NutritionRowDao
import com.example.simplediary.data.local.dao.StateNoteDao
import com.example.simplediary.data.local.dao.WorkoutCategoryDao
import com.example.simplediary.data.local.dao.WorkoutDao
import com.example.simplediary.data.local.dao.WorkoutTypeDao
import com.example.simplediary.data.local.entity.DailyTargetEntity
import com.example.simplediary.data.local.entity.MealEntity
import com.example.simplediary.data.local.entity.NutritionRowEntity
import com.example.simplediary.data.local.entity.StateNoteEntity
import com.example.simplediary.data.local.entity.WorkoutCategoryEntity
import com.example.simplediary.data.local.entity.WorkoutEntity
import com.example.simplediary.data.local.entity.WorkoutTypeEntity

@Database(
    entities = [
        MealEntity::class,
        NutritionRowEntity::class,
        WorkoutEntity::class,
        WorkoutCategoryEntity::class,
        WorkoutTypeEntity::class,
        StateNoteEntity::class,
        DailyTargetEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun nutritionRowDao(): NutritionRowDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutCategoryDao(): WorkoutCategoryDao
    abstract fun workoutTypeDao(): WorkoutTypeDao
    abstract fun stateNoteDao(): StateNoteDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun feedDao(): FeedDao
}
