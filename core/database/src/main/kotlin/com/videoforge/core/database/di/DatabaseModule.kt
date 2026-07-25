package com.videoforge.core.database.di

import android.content.Context
import androidx.room.Room
import com.videoforge.core.database.AppDatabase
import com.videoforge.core.database.dao.ClipDao
import com.videoforge.core.database.dao.HistoryDao
import com.videoforge.core.database.dao.MarkerDao
import com.videoforge.core.database.dao.MediaAssetDao
import com.videoforge.core.database.dao.OperationLogDao
import com.videoforge.core.database.dao.SubtitleCueDao
import com.videoforge.core.database.dao.SubtitleTrackDao
import com.videoforge.core.database.dao.TaskDao
import com.videoforge.core.database.dao.TimelineDao
import com.videoforge.core.database.migration.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "videoforge.db"
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaAssetDao(database: AppDatabase): MediaAssetDao {
        return database.mediaAssetDao()
    }

    @Provides
    @Singleton
    fun provideTimelineDao(database: AppDatabase): TimelineDao {
        return database.timelineDao()
    }

    @Provides
    @Singleton
    fun provideClipDao(database: AppDatabase): ClipDao {
        return database.clipDao()
    }

    @Provides
    @Singleton
    fun provideMarkerDao(database: AppDatabase): MarkerDao {
        return database.markerDao()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideOperationLogDao(database: AppDatabase): OperationLogDao {
        return database.operationLogDao()
    }

    @Provides
    @Singleton
    fun provideSubtitleTrackDao(database: AppDatabase): SubtitleTrackDao {
        return database.subtitleTrackDao()
    }

    @Provides
    @Singleton
    fun provideSubtitleCueDao(database: AppDatabase): SubtitleCueDao {
        return database.subtitleCueDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
}