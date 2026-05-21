package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simplediary.data.local.entity.DailyTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTargetDao {
    @Insert
    suspend fun insertDailyTarget(dailyTarget: DailyTargetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyTarget(dailyTarget: DailyTargetEntity): Long

    @Query(
        """
        SELECT * FROM daily_targets
        WHERE effectiveFrom <= :dayStartEpochMillisUtc
        ORDER BY effectiveFrom DESC, id DESC
        LIMIT 1
        """
    )
    fun observeTargetEffectiveForDay(dayStartEpochMillisUtc: Long): Flow<DailyTargetEntity?>

    @Query(
        """
        SELECT * FROM daily_targets
        WHERE effectiveFrom <= :dayStartEpochMillisUtc
        ORDER BY effectiveFrom DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getTargetEffectiveForDay(dayStartEpochMillisUtc: Long): DailyTargetEntity?

    @Query("SELECT * FROM daily_targets WHERE effectiveFrom = :dayStartEpochMillisUtc LIMIT 1")
    fun observeDailyTarget(dayStartEpochMillisUtc: Long): Flow<DailyTargetEntity?>

    @Query("SELECT * FROM daily_targets WHERE effectiveFrom = :dayStartEpochMillisUtc LIMIT 1")
    suspend fun getDailyTarget(dayStartEpochMillisUtc: Long): DailyTargetEntity?
}
