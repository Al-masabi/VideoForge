package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoforge.core.database.entity.MarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {

    @Query(
        """
        SELECT markers.* FROM markers
        INNER JOIN clips ON markers.clipId = clips.id
        WHERE clips.timelineId = :timelineId
        ORDER BY clips.ordinal ASC, markers.offsetMs ASC
        """
    )
    fun observeByTimeline(timelineId: String): Flow<List<MarkerEntity>>

    @Query(
        """
        SELECT markers.* FROM markers
        INNER JOIN clips ON markers.clipId = clips.id
        WHERE clips.timelineId = :timelineId
        ORDER BY clips.ordinal ASC, markers.offsetMs ASC
        """
    )
    suspend fun getByTimeline(timelineId: String): List<MarkerEntity>

    @Query("DELETE FROM markers WHERE clipId IN (SELECT id FROM clips WHERE timelineId = :timelineId)")
    suspend fun deleteByTimeline(timelineId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markers: List<MarkerEntity>)
}