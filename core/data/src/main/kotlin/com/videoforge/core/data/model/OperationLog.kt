package com.videoforge.core.data.model

data class OperationLog(
    val id: String,
    val operationType: String,
    val status: String,
    val startedAt: Long,
    val durationMs: Long,
    val inputUri: String,
    val outputUri: String?,
    val errorMessage: String?
)