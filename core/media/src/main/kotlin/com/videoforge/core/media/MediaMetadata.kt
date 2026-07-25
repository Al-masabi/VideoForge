package com.videoforge.core.media

data class MediaMetadata(
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val hasAudio: Boolean,
    val hasVideo: Boolean
)