package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["state", "priority", "createdAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val state: String,
    val priority: Int,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val progress: Int,
    val inputUri: String,
    val inputName: String,
    val outputUri: String?,
    val presetId: String,
    val errorMessage: String?
)