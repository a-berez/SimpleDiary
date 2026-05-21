package com.example.simplediary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simplediary.data.local.entity.StateNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StateNoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStateNote(stateNote: StateNoteEntity): Long

    @Update
    suspend fun updateStateNote(stateNote: StateNoteEntity)

    @Delete
    suspend fun deleteStateNote(stateNote: StateNoteEntity)

    @Query("DELETE FROM state_notes WHERE id = :stateNoteId")
    suspend fun deleteStateNoteById(stateNoteId: Long)

    @Query("SELECT * FROM state_notes WHERE id = :stateNoteId LIMIT 1")
    suspend fun getStateNoteById(stateNoteId: Long): StateNoteEntity?

    @Query(
        """
        SELECT * FROM state_notes
        WHERE (:fromEpochMillisInclusive IS NULL OR timestampEpochMillis >= :fromEpochMillisInclusive)
          AND (:toEpochMillisInclusive IS NULL OR timestampEpochMillis <= :toEpochMillisInclusive)
        ORDER BY timestampEpochMillis DESC
        """
    )
    fun observeStateNotes(
        fromEpochMillisInclusive: Long? = null,
        toEpochMillisInclusive: Long? = null,
    ): Flow<List<StateNoteEntity>>

    @Query("SELECT * FROM state_notes ORDER BY timestampEpochMillis DESC")
    suspend fun getAllStateNotes(): List<StateNoteEntity>
}
