package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtitle_tracks",
    indices = [
        Index(value = ["timelineId"])
    ]
)
data class SubtitleTrackEntity(
    @PrimaryKey
    val id: String,
    val timelineId: String,
    val uri: String,
    val displayName: String,
    val language: String,
    val mimeType: String?,
    val charset: String,
    val createdAt: Long
)