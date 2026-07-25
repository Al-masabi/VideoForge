package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoforge.core.database.entity.ClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    @Query("SELECT * FROM clips WHERE timelineId = :timelineId ORDER BY ordinal ASC")
    fun observeByTimeline(timelineId: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE timelineId = :timelineId ORDER BY ordinal ASC")
    suspend fun getByTimeline(timelineId: String): List<ClipEntity>

    @Query("DELETE FROM clips WHERE timelineId = :timelineId")
    suspend fun deleteByTimeline(timelineId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<ClipEntity>)
}