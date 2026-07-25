package com.videoforge.core.data.task

import com.videoforge.core.data.model.TaskInfo
import com.videoforge.core.database.dao.TaskDao
import com.videoforge.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<TaskInfo>> {
        return taskDao.observeAll().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun enqueueCompression(
        inputUri: String,
        inputName: String,
        presetId: String
    ) {
        taskDao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                type = TYPE_COMPRESS,
                state = STATE_PENDING,
                priority = 0,
                createdAt = System.currentTimeMillis(),
                startedAt = null,
                completedAt = null,
                progress = 0,
                inputUri = inputUri,
                inputName = inputName,
                outputUri = null,
                presetId = presetId,
                errorMessage = null
            )
        )
    }

    override suspend fun cancelTask(taskId: String) {
        taskDao.cancelTask(taskId, System.currentTimeMillis())
    }

    override suspend fun cancelAllPending() {
        taskDao.cancelAllPending(System.currentTimeMillis())
    }

    private fun TaskEntity.toDomain(): TaskInfo {
        return TaskInfo(
            id = id,
            type = type,
            state = state,
            priority = priority,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            progress = progress,
            inputUri = inputUri,
            inputName = inputName,
            outputUri = outputUri,
            presetId = presetId,
            errorMessage = errorMessage
        )
    }

    companion object {
        private const val TYPE_COMPRESS = "COMPRESS"
        private const val STATE_PENDING = "PENDING"
    }
}