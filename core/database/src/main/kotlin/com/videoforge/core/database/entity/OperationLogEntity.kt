package com.videoforge.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operation_logs",
    indices = [
        Index(value = ["startedAt"])
    ]
)
data class OperationLogEntity(
    @PrimaryKey
    val id: String,
    val operationType: String,
    val status: String,
    val startedAt: Long,
    val durationMs: Long,
    val inputUri: String,
    val outputUri: String?,
    val errorMessage: String?
)