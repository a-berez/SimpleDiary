package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simplediary.data.local.entity.DailyTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyTarget(dailyTarget: DailyTargetEntity): Long

    @Query("SELECT * FROM daily_targets WHERE dateEpochMillisUtcStart = :dayStartEpochMillisUtc LIMIT 1")
    fun observeDailyTarget(dayStartEpochMillisUtc: Long): Flow<DailyTargetEntity?>

    @Query("SELECT * FROM daily_targets WHERE dateEpochMillisUtcStart = :dayStartEpochMillisUtc LIMIT 1")
    suspend fun getDailyTarget(dayStartEpochMillisUtc: Long): DailyTargetEntity?
}
