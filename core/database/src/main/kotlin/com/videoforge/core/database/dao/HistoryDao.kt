package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoforge.core.database.entity.EditHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM edit_history WHERE timelineId = :timelineId AND sequence = :sequence LIMIT 1")
    suspend fun getBySequence(timelineId: String, sequence: Int): EditHistoryEntity?

    @Query("DELETE FROM edit_history WHERE timelineId = :timelineId AND sequence > :sequence")
    suspend fun deleteAfterSequence(timelineId: String, sequence: Int)

    @Query("DELETE FROM edit_history WHERE timelineId = :timelineId")
    suspend fun deleteByTimeline(timelineId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: EditHistoryEntity)

    @Query("SELECT COUNT(*) FROM edit_history WHERE timelineId = :timelineId")
    fun observeCount(timelineId: String): Flow<Int>

    @Query("SELECT MAX(sequence) FROM edit_history WHERE timelineId = :timelineId")
    suspend fun getMaxSequence(timelineId: String): Int?
}