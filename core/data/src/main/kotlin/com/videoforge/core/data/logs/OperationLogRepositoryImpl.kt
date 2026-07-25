package com.videoforge.core.data.logs

import com.videoforge.core.data.model.OperationLog
import com.videoforge.core.database.dao.OperationLogDao
import com.videoforge.core.database.entity.OperationLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationLogRepositoryImpl @Inject constructor(
    private val operationLogDao: OperationLogDao
) : OperationLogRepository {

    override fun observeAll(): Flow<List<OperationLog>> {
        return operationLogDao.observeAll().map { entities ->
            entities.map { entity ->
                OperationLog(
                    id = entity.id,
                    operationType = entity.operationType,
                    status = entity.status,
                    startedAt = entity.startedAt,
                    durationMs = entity.durationMs,
                    inputUri = entity.inputUri,
                    outputUri = entity.outputUri,
                    errorMessage = entity.errorMessage
                )
            }
        }
    }

    override suspend fun log(
        operationType: String,
        status: String,
        startedAt: Long,
        durationMs: Long,
        inputUri: String,
        outputUri: String?,
        errorMessage: String?
    ) {
        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                operationType = operationType,
                status = status,
                startedAt = startedAt,
                durationMs = durationMs,
                inputUri = inputUri,
                outputUri = outputUri,
                errorMessage = errorMessage
            )
        )
    }
}