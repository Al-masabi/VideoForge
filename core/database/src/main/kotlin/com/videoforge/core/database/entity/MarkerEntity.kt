package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "markers",
    indices = [
        Index(value = ["clipId"])
    ]
)
data class MarkerEntity(
    @PrimaryKey
    val id: String,
    val clipId: String,
    val offsetMs: Long,
    val label: String
)