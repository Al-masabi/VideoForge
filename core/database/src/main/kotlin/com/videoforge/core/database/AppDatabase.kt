package com.videoforge.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.videoforge.core.database.dao.ClipDao
import com.videoforge.core.database.dao.HistoryDao
import com.videoforge.core.database.dao.MarkerDao
import com.videoforge.core.database.dao.MediaAssetDao
import com.videoforge.core.database.dao.OperationLogDao
import com.videoforge.core.database.dao.SubtitleCueDao
import com.videoforge.core.database.dao.SubtitleTrackDao
import com.videoforge.core.database.dao.TaskDao
import com.videoforge.core.database.dao.TimelineDao
import com.videoforge.core.database.entity.ClipEntity
import com.videoforge.core.database.entity.EditHistoryEntity
import com.videoforge.core.database.entity.MarkerEntity
import com.videoforge.core.database.entity.MediaAssetEntity
import com.videoforge.core.database.entity.OperationLogEntity
import com.videoforge.core.database.entity.SubtitleCueEntity
import com.videoforge.core.database.entity.SubtitleTrackEntity
import com.videoforge.core.database.entity.TaskEntity
import com.videoforge.core.database.entity.TimelineEntity

@Database(
    entities = [
        MediaAssetEntity::class,
        TimelineEntity::class,
        ClipEntity::class,
        MarkerEntity::class,
        EditHistoryEntity::class,
        OperationLogEntity::class,
        SubtitleTrackEntity::class,
        SubtitleCueEntity::class,
        TaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun timelineDao(): TimelineDao
    abstract fun clipDao(): ClipDao
    abstract fun markerDao(): MarkerDao
    abstract fun historyDao(): HistoryDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun subtitleTrackDao(): SubtitleTrackDao
    abstract fun subtitleCueDao(): SubtitleCueDao
    abstract fun taskDao(): TaskDao
}