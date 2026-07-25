package com.videoforge.core.data.logs

import com.videoforge.core.data.model.OperationLog
import kotlinx.coroutines.flow.Flow

interface OperationLogRepository {
    fun observeAll(): Flow<List<OperationLog>>

    suspend fun log(
        operationType: String,
        status: String,
        startedAt: Long,
        durationMs: Long,
        inputUri: String,
        outputUri: String?,
        errorMessage: String?
    )
}