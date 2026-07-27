package com.videoforge.android.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.videoforge.android.R
import com.videoforge.core.data.logs.OperationLogRepository
import com.videoforge.core.database.dao.ClipDao
import com.videoforge.core.database.dao.MediaAssetDao
import com.videoforge.core.database.dao.SubtitleCueDao
import com.videoforge.core.database.dao.TimelineDao
import com.videoforge.core.database.entity.SubtitleCueEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExportService : Service() {

    @Inject
    lateinit var timelineDao: TimelineDao

    @Inject
    lateinit var clipDao: ClipDao

    @Inject
    lateinit var mediaAssetDao: MediaAssetDao

    @Inject
    lateinit var subtitleCueDao: SubtitleCueDao

    @Inject
    lateinit var operationLogRepository: OperationLogRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var currentEngine: VideoExportEngine? = null

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                currentEngine?.cancel()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val timelineId = intent.getStringExtra(EXTRA_TIMELINE_ID)
                val outputTreeUri = intent.getStringExtra(EXTRA_OUTPUT_TREE_URI)

                if (timelineId == null || outputTreeUri == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(
                    NOTIFICATION_ID,
                    buildProgressNotification(0),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                )

                serviceScope.launch {
                    runExport(timelineId, outputTreeUri)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        currentEngine?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runExport(
        timelineId: String,
        outputTreeUri: String
    ) {
        val startedAt = System.currentTimeMillis()

        val timeline = timelineDao.getById(timelineId)

        if (timeline == null) {
            finishExport()
            return
        }

        val clipEntities = clipDao.getByTimeline(timelineId)

        val clips = clipEntities.map { clip ->
            ExportClip(
                sourceInMs = clip.sourceInMs,
                sourceOutMs = clip.sourceOutMs
            )
        }

        val sourceDurationMs = mediaAssetDao.getByUri(timeline.assetUri)?.durationMs ?: 0L

        val cues = subtitleCueDao.observeByTimeline(timelineId).first()

        val treeUri = Uri.parse(outputTreeUri)

        runCatching {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        val treeDocUri = runCatching {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        }.getOrNull()

        if (treeDocUri == null) {
            operationLogRepository.log(
                operationType = OPERATION_EXPORT,
                status = STATUS_FAILED,
                startedAt = startedAt,
                durationMs = System.currentTimeMillis() - startedAt,
                inputUri = timeline.assetUri,
                outputUri = null,
                errorMessage = "Cannot access the chosen folder"
            )
            showResultNotification(getString(R.string.export_failed))
            finishExport()
            return
        }

        val baseName = timeline.name
            .substringBeforeLast('.', timeline.name)
            .ifBlank { "videoforge" }

        val videoUri = runCatching {
            DocumentsContract.createDocument(
                contentResolver,
                treeDocUri,
                "video/mp4",
                "$baseName.mp4"
            )
        }.getOrNull()

        if (videoUri == null) {
            operationLogRepository.log(
                operationType = OPERATION_EXPORT,
                status = STATUS_FAILED,
                startedAt = startedAt,
                durationMs = System.currentTimeMillis() - startedAt,
                inputUri = timeline.assetUri,
                outputUri = null,
                errorMessage = "Cannot create the video file in the chosen folder"
            )
            showResultNotification(getString(R.string.export_failed))
            finishExport()
            return
        }

        val engine = VideoExportEngine(applicationContext)
        currentEngine = engine

        val outcome = engine.export(
            inputUri = Uri.parse(timeline.assetUri),
            outputUri = videoUri,
            clips = clips,
            sourceDurationMs = sourceDurationMs
        ) { progress ->
            updateNotification(progress)
        }

        val completedAt = System.currentTimeMillis()

        when (outcome) {
            is ExportOutcome.Success -> {
                var subtitleName: String? = null

                if (cues.isNotEmpty()) {
                    val srtUri = runCatching {
                        DocumentsContract.createDocument(
                            contentResolver,
                            treeDocUri,
                            "application/x-subrip",
                            "$baseName.srt"
                        )
                    }.getOrNull()

                    if (srtUri != null && writeSyncedSubtitleToUri(srtUri, cues, clips)) {
                        subtitleName = "$baseName.srt"
                    }
                }

                operationLogRepository.log(
                    operationType = OPERATION_EXPORT,
                    status = STATUS_SUCCESS,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = timeline.assetUri,
                    outputUri = videoUri.toString(),
                    errorMessage = outcome.strategy
                )

                val extra = subtitleName?.let { " • الترجمة: $it" }.orEmpty()
                showResultNotification(
                    getString(R.string.export_success) + " • " + outcome.strategy + extra
                )
            }

            is ExportOutcome.Failure -> {
                operationLogRepository.log(
                    operationType = OPERATION_EXPORT,
                    status = STATUS_FAILED,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = timeline.assetUri,
                    outputUri = null,
                    errorMessage = outcome.message
                )

                val detail = outcome.message.take(160)
                showResultNotification(getString(R.string.export_failed) + ": " + detail)
            }

            is ExportOutcome.Cancelled -> {
                operationLogRepository.log(
                    operationType = OPERATION_EXPORT,
                    status = STATUS_CANCELLED,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = timeline.assetUri,
                    outputUri = null,
                    errorMessage = null
                )

                showResultNotification(getString(R.string.export_cancelled))
            }
        }

        finishExport()
    }

    private fun finishExport() {
        currentEngine = null
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun buildSyncedSrt(
        cues: List<SubtitleCueEntity>,
        clips: List<ExportClip>
    ): String {
        val builder = StringBuilder()
        var outIndex = 1
        var timelineCursor = 0L

        for (clip in clips) {
            val clipStart = clip.sourceInMs
            val clipEnd = clip.sourceOutMs

            for (cue in cues) {
                val interStart = maxOf(cue.startMs, clipStart)
                val interEnd = minOf(cue.endMs, clipEnd)

                if (interEnd <= interStart) continue

                val newStart = timelineCursor + (interStart - clipStart)
                val newEnd = timelineCursor + (interEnd - clipStart)

                builder.append(outIndex++).append('\n')
                builder
                    .append(formatSrtTime(newStart))
                    .append(" --> ")
                    .append(formatSrtTime(newEnd))
                    .append('\n')
                builder.append(cue.text.replace("\r", "")).append("\n\n")
            }

            timelineCursor += (clipEnd - clipStart)
        }

        return builder.toString()
    }

    private fun writeSyncedSubtitleToUri(
        uri: Uri,
        cues: List<SubtitleCueEntity>,
        clips: List<ExportClip>
    ): Boolean {
        return runCatching {
            val srt = buildSyncedSrt(cues, clips)
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(srt.toByteArray(Charsets.UTF_8))
            }
            true
        }.getOrDefault(false)
    }

    private fun formatSrtTime(ms: Long): String {
        val totalMs = ms.coerceAtLeast(0L)
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val seconds = (totalMs % 60_000) / 1000
        val millis = totalMs % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.export_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildProgressNotification(progress: Int): android.app.Notification {
        val cancelIntent = Intent(this, ExportService::class.java).apply {
            action = ACTION_CANCEL
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            4,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_delete,
            getString(R.string.cancel),
            cancelPendingIntent
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.export_notification_title))
            .setContentText(getString(R.string.export_notification_text, progress))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .addAction(cancelAction)
            .build()
    }

    private fun updateNotification(progress: Int) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildProgressNotification(progress)
        )
    }

    private fun showResultNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.export_notification_title))
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .clearActions()
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "export_channel"
        private const val NOTIFICATION_ID = 6001

        private const val ACTION_START = "com.videoforge.android.export.START"
        private const val ACTION_CANCEL = "com.videoforge.android.export.CANCEL"

        private const val EXTRA_TIMELINE_ID = "extra_timeline_id"
        private const val EXTRA_OUTPUT_TREE_URI = "extra_output_tree_uri"

        private const val OPERATION_EXPORT = "EXPORT"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_FAILED = "FAILED"
        private const val STATUS_CANCELLED = "CANCELLED"

        fun start(
            context: Context,
            timelineId: String,
            outputTreeUri: String
        ) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TIMELINE_ID, timelineId)
                putExtra(EXTRA_OUTPUT_TREE_URI, outputTreeUri)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_CANCEL
            }

            context.startService(intent)
        }
    }
}
