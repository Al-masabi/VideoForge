package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_assets",
    indices = [
        Index(value = ["accessedAt"])
    ]
)
data class MediaAssetEntity(
    @PrimaryKey
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