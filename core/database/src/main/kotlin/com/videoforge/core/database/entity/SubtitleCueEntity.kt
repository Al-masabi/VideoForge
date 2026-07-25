package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtitle_cues",
    indices = [
        Index(value = ["trackId", "startMs"])
    ]
)
data class SubtitleCueEntity(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val ordinal: Int
)