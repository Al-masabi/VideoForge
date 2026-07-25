package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.videoforge.core.database.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {

    @Query("SELECT * FROM timelines WHERE id = :timelineId LIMIT 1")
    fun observeById(timelineId: String): Flow<TimelineEntity?>

    @Query("SELECT * FROM timelines WHERE id = :timelineId LIMIT 1")
    suspend fun getById(timelineId: String): TimelineEntity?

    @Query("SELECT * FROM timelines WHERE assetUri = :assetUri LIMIT 1")
    suspend fun getByAssetUri(assetUri: String): TimelineEntity?

    @Upsert
    suspend fun upsert(timeline: TimelineEntity)

    @Query("UPDATE timelines SET historyIndex = :historyIndex, updatedAt = :updatedAt WHERE id = :timelineId")
    suspend fun updateHistoryIndex(
        timelineId: String,
        historyIndex: Int,
        updatedAt: Long
    )
}