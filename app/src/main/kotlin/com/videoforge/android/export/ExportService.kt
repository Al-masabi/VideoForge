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
import java.io.File
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
                val outputUri = intent.getStringExtra(EXTRA_OUTPUT_URI)

                if (timelineId == null || outputUri == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(
                    NOTIFICATION_ID,
                    buildProgressNotification(0),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                )

                serviceScope.launch {
                    runExport(timelineId, outputUri)
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
        outputUri: String
    ) {
        val startedAt = System.currentTimeMillis()

        val timeline = timelineDao.getById(timelineId)

        if (timeline == null) {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
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
        val subtitleFile = if (cues.isNotEmpty()) writeTemporarySrt(cues) else null

        val engine = VideoExportEngine(applicationContext)
        currentEngine = engine

        try {
            val outcome = engine.export(
                inputUri = Uri.parse(timeline.assetUri),
                outputUri = Uri.parse(outputUri),
                clips = clips,
                sourceDurationMs = sourceDurationMs,
                subtitleUri = subtitleFile?.let { Uri.fromFile(it) }
            ) { progress ->
                updateNotification(progress)
            }

            val completedAt = System.currentTimeMillis()

            when (outcome) {
                is ExportOutcome.Success -> {
                    operationLogRepository.log(
                        operationType = OPERATION_EXPORT,
                        status = STATUS_SUCCESS,
                        startedAt = startedAt,
                        durationMs = completedAt - startedAt,
                        inputUri = timeline.assetUri,
                        outputUri = outcome.outputUri,
                        errorMessage = outcome.strategy
                    )

                    showResultNotification(
                        getString(R.string.export_success) + " • " + outcome.strategy
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

                    showResultNotification(getString(R.string.export_failed))
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
        } finally {
            subtitleFile?.delete()
            currentEngine = null
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun writeTemporarySrt(cues: List<SubtitleCueEntity>): File? {
        return runCatching {
            val file = File(cacheDir, "subtitle_export_${System.currentTimeMillis()}.srt")
            val builder = StringBuilder()

            cues.forEachIndexed { index, cue ->
                builder.append(index + 1).append("\n")
                builder
                    .append(formatSrtTime(cue.startMs))
                    .append(" --> ")
                    .append(formatSrtTime(cue.endMs))
                    .append("\n")
                builder.append(cue.text.replace("\r", "")).append("\n\n")
            }

            file.writeText(builder.toString(), Charsets.UTF_8)
            file
        }.getOrNull()
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
        private const val EXTRA_OUTPUT_URI = "extra_output_uri"

        private const val OPERATION_EXPORT = "EXPORT"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_FAILED = "FAILED"
        private const val STATUS_CANCELLED = "CANCELLED"

        fun start(
            context: Context,
            timelineId: String,
            outputUri: String
        ) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TIMELINE_ID, timelineId)
                putExtra(EXTRA_OUTPUT_URI, outputUri)
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
