package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timelines",
    indices = [
        Index(value = ["assetUri"], unique = true)
    ]
)
data class TimelineEntity(
    @PrimaryKey
    val id: String,
    val assetUri: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val historyIndex: Int
)