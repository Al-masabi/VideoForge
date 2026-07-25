package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.videoforge.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE state = 'PENDING' ORDER BY priority DESC, createdAt ASC LIMIT 1")
    suspend fun getNextPending(): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET state = :state, startedAt = :startedAt WHERE id = :id")
    suspend fun updateStateAndStarted(
        id: String,
        state: String,
        startedAt: Long
    )

    @Query("UPDATE tasks SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int)

    @Query(
        """
        UPDATE tasks
        SET state = :state,
            completedAt = :completedAt,
            outputUri = :outputUri,
            errorMessage = :errorMessage
        WHERE id = :id
        """
    )
    suspend fun finalize(
        id: String,
        state: String,
        completedAt: Long,
        outputUri: String?,
        errorMessage: String?
    )

    @Query("UPDATE tasks SET state = 'CANCELLED', completedAt = :completedAt WHERE id = :id AND state IN ('PENDING', 'RUNNING')")
    suspend fun cancelTask(id: String, completedAt: Long)

    @Query("UPDATE tasks SET state = 'CANCELLED', completedAt = :completedAt WHERE state = 'PENDING'")
    suspend fun cancelAllPending(completedAt: Long)
}