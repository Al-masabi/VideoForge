package com.videoforge.core.data.editor

import kotlinx.coroutines.flow.Flow

interface EditorRepository {

    suspend fun getOrCreateTimeline(assetUri: String): String

    fun observeEditorState(timelineId: String): Flow<EditorState>

    suspend fun splitClip(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    )

    suspend fun trimClipStart(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    )

    suspend fun trimClipEnd(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    )

    suspend fun deleteClip(
        timelineId: String,
        clipId: String
    )

    suspend fun deleteTimelineRange(
        timelineId: String,
        rangeStartMs: Long,
        rangeEndMs: Long
    )

    suspend fun addMarker(
        timelineId: String,
        clipId: String,
        offsetMs: Long,
        label: String
    )

    suspend fun deleteMarker(
        timelineId: String,
        markerId: String
    )

    suspend fun undo(timelineId: String)

    suspend fun redo(timelineId: String)
}
