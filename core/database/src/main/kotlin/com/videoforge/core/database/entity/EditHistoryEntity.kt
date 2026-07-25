package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "edit_history",
    indices = [
        Index(value = ["timelineId", "sequence"], unique = true)
    ]
)
data class EditHistoryEntity(
    @PrimaryKey
    val id: String,
    val timelineId: String,
    val sequence: Int,
    val clipsJson: String,
    val markersJson: String,
    val createdAt: Long
)