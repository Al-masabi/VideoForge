package com.videoforge.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `media_assets` (
                    `uri` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `mimeType` TEXT,
                    `sizeBytes` INTEGER NOT NULL,
                    `durationMs` INTEGER NOT NULL,
                    `width` INTEGER NOT NULL,
                    `height` INTEGER NOT NULL,
                    `rotation` INTEGER NOT NULL,
                    `hasAudio` INTEGER NOT NULL,
                    `hasVideo` INTEGER NOT NULL,
                    `accessedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`uri`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_media_assets_accessedAt` ON `media_assets` (`accessedAt`)"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `timelines` (
                    `id` TEXT NOT NULL,
                    `assetUri` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `historyIndex` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_timelines_assetUri` ON `timelines` (`assetUri`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `clips` (
                    `id` TEXT NOT NULL,
                    `timelineId` TEXT NOT NULL,
                    `assetUri` TEXT NOT NULL,
                    `sourceInMs` INTEGER NOT NULL,
                    `sourceOutMs` INTEGER NOT NULL,
                    `ordinal` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_clips_timelineId_ordinal` ON `clips` (`timelineId`, `ordinal`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `markers` (
                    `id` TEXT NOT NULL,
                    `clipId` TEXT NOT NULL,
                    `offsetMs` INTEGER NOT NULL,
                    `label` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_markers_clipId` ON `markers` (`clipId`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `edit_history` (
                    `id` TEXT NOT NULL,
                    `timelineId` TEXT NOT NULL,
                    `sequence` INTEGER NOT NULL,
                    `clipsJson` TEXT NOT NULL,
                    `markersJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_edit_history_timelineId_sequence` ON `edit_history` (`timelineId`, `sequence`)"
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `subtitle_tracks` (
                    `id` TEXT NOT NULL,
                    `timelineId` TEXT NOT NULL,
                    `uri` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `language` TEXT NOT NULL,
                    `mimeType` TEXT,
                    `charset` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subtitle_tracks_timelineId` ON `subtitle_tracks` (`timelineId`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `subtitle_cues` (
                    `id` TEXT NOT NULL,
                    `trackId` TEXT NOT NULL,
                    `startMs` INTEGER NOT NULL,
                    `endMs` INTEGER NOT NULL,
                    `text` TEXT NOT NULL,
                    `ordinal` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_subtitle_cues_trackId_startMs` ON `subtitle_cues` (`trackId`, `startMs`)"
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `priority` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `startedAt` INTEGER,
                    `completedAt` INTEGER,
                    `progress` INTEGER NOT NULL,
                    `inputUri` TEXT NOT NULL,
                    `inputName` TEXT NOT NULL,
                    `outputUri` TEXT,
                    `presetId` TEXT NOT NULL,
                    `errorMessage` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tasks_state_priority_createdAt` ON `tasks` (`state`, `priority`, `createdAt`)"
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `operation_logs` (
                    `id` TEXT NOT NULL,
                    `operationType` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `durationMs` INTEGER NOT NULL,
                    `inputUri` TEXT NOT NULL,
                    `outputUri` TEXT,
                    `errorMessage` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_operation_logs_startedAt` ON `operation_logs` (`startedAt`)"
            )
        }
    }
}