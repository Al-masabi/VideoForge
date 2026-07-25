package com.videoforge.core.data.editor

data class EditorClip(
    val id: String,
    val assetUri: String,
    val sourceInMs: Long,
    val sourceOutMs: Long,
    val ordinal: Int
) {
    val durationMs: Long
        get() = sourceOutMs - sourceInMs
}

data class EditorMarker(
    val id: String,
    val clipId: String,
    val offsetMs: Long,
    val label: String
)

data class TimelineInfo(
    val id: String,
    val assetUri: String,
    val name: String,
    val historyIndex: Int
)

data class EditorState(
    val timeline: TimelineInfo?,
    val clips: List<EditorClip>,
    val markers: List<EditorMarker>,
    val historyIndex: Int,
    val historyCount: Int
)