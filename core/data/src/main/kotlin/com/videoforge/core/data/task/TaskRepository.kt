package com.videoforge.core.data.task

import com.videoforge.core.data.model.TaskInfo
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<TaskInfo>>
    suspend fun enqueueCompression(inputUri: String, inputName: String, presetId: String)
    suspend fun cancelTask(taskId: String)
    suspend fun cancelAllPending()
}