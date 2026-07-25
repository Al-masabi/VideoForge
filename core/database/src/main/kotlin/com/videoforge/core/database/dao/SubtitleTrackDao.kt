package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.videoforge.core.database.entity.SubtitleTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleTrackDao {

    @Query("SELECT * FROM subtitle_tracks WHERE timelineId = :timelineId ORDER BY createdAt ASC")
    fun observeByTimeline(timelineId: String): Flow<List<SubtitleTrackEntity>>

    @Query("SELECT * FROM subtitle_tracks WHERE timelineId = :timelineId ORDER BY createdAt ASC")
    suspend fun getByTimeline(timelineId: String): List<SubtitleTrackEntity>

    @Upsert
    suspend fun upsert(entity: SubtitleTrackEntity)

    @Query("DELETE FROM subtitle_tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Query("DELETE FROM subtitle_tracks WHERE timelineId = :timelineId")
    suspend fun deleteByTimeline(timelineId: String)
}