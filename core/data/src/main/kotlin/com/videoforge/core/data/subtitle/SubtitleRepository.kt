package com.videoforge.core.data.subtitle

import android.net.Uri
import com.videoforge.core.subtitle.SubtitleCue
import kotlinx.coroutines.flow.Flow

data class SubtitleTrackInfo(
    val id: String,
    val timelineId: String,
    val uri: String,
    val displayName: String,
    val language: String,
    val mimeType: String?,
    val charset: String,
    val createdAt: Long
)

interface SubtitleRepository {
    fun observeTracks(timelineId: String): Flow<List<SubtitleTrackInfo>>
    fun observeCues(timelineId: String): Flow<List<SubtitleCue>>
    suspend fun importSubtitle(timelineId: String, uri: Uri): Result<SubtitleTrackInfo>
    suspend fun deleteTrack(trackId: String)
}