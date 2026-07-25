package com.videoforge.core.data.model

data class MediaAsset(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val accessedAt: Long
)