package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    indices = [
        Index(value = ["timelineId", "ordinal"])
    ]
)
data class ClipEntity(
    @PrimaryKey
    val id: String,
    val timelineId: String,
    val assetUri: String,
    val sourceInMs: Long,
    val sourceOutMs: Long,
    val ordinal: Int
)