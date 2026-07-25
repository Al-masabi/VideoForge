package com.videoforge.core.data.model

data class TaskInfo(
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