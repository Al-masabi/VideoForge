package com.videoforge.core.data.di

import com.videoforge.core.data.editor.EditorRepository
import com.videoforge.core.data.editor.EditorRepositoryImpl
import com.videoforge.core.data.logs.OperationLogRepository
import com.videoforge.core.data.logs.OperationLogRepositoryImpl
import com.videoforge.core.data.repository.MediaRepository
import com.videoforge.core.data.repository.MediaRepositoryImpl
import com.videoforge.core.data.repository.MediaStoreRepository
import com.videoforge.core.data.repository.MediaStoreRepositoryImpl
import com.videoforge.core.data.subtitle.SubtitleRepository
import com.videoforge.core.data.subtitle.SubtitleRepositoryImpl
import com.videoforge.core.data.task.TaskRepository
import com.videoforge.core.data.task.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindMediaStoreRepository(
        impl: MediaStoreRepositoryImpl
    ): MediaStoreRepository

    @Binds
    @Singleton
    abstract fun bindEditorRepository(
        impl: EditorRepositoryImpl
    ): EditorRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(
        impl: SubtitleRepositoryImpl
    ): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindOperationLogRepository(
        impl: OperationLogRepositoryImpl
    ): OperationLogRepository
}