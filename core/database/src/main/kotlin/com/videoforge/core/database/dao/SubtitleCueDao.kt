package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoforge.core.database.entity.SubtitleCueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleCueDao {

    @Query(
        """
        SELECT cues.* FROM subtitle_cues AS cues
        INNER JOIN subtitle_tracks AS tracks ON cues.trackId = tracks.id
        WHERE tracks.timelineId = :timelineId
        ORDER BY cues.startMs ASC
        """
    )
    fun observeByTimeline(timelineId: String): Flow<List<SubtitleCueEntity>>

    @Query("SELECT * FROM subtitle_cues WHERE trackId = :trackId ORDER BY startMs ASC")
    suspend fun getByTrack(trackId: String): List<SubtitleCueEntity>

    @Query("DELETE FROM subtitle_cues WHERE trackId = :trackId")
    suspend fun deleteByTrack(trackId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cues: List<SubtitleCueEntity>)
}