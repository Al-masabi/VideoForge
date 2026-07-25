package com.videoforge.android.task

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.videoforge.android.R
import com.videoforge.android.compression.CompressionEngine
import com.videoforge.android.compression.CompressionMode
import com.videoforge.android.compression.CompressionPresets
import com.videoforge.android.storage.OutputFileProvider
import com.videoforge.core.adaptive.AdaptiveManager
import com.videoforge.core.data.logs.OperationLogRepository
import com.videoforge.core.database.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CompressionOutcome {
    abstract val inputUri: String?

    data class Success(
        override val inputUri: String,
        val outputUri: String
    ) : CompressionOutcome()

    data class Failure(
        override val inputUri: String?,
        val message: String
    ) : CompressionOutcome()

    data class Cancelled(
        override val inputUri: String?
    ) : CompressionOutcome()
}

@AndroidEntryPoint
class TaskService : Service() {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var operationLogRepository: OperationLogRepository

    @Inject
    lateinit var adaptiveManager: AdaptiveManager

    @Inject
    lateinit var outputFileProvider: OutputFileProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var currentEngine: CompressionEngine? = null
    private var currentTaskId: String? = null
    private var isProcessing = false

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return START_NOT_STICKY

                serviceScope.launch {
                    val task = taskDao.getById(taskId)

                    if (task != null && task.state == STATE_PENDING) {
                        taskDao.cancelTask(taskId, System.currentTimeMillis())
                    }

                    if (currentTaskId == taskId) {
                        currentEngine?.cancel()
                    }
                }

                return START_NOT_STICKY
            }

            ACTION_CANCEL_ALL -> {
                serviceScope.launch {
                    taskDao.cancelAllPending(System.currentTimeMillis())
                    currentEngine?.cancel()
                }

                return START_NOT_STICKY
            }

            ACTION_PROCESS -> {
                if (isProcessing) {
                    return START_NOT_STICKY
                }

                isProcessing = true

                startForeground(
                    NOTIFICATION_ID,
                    buildProgressNotification("", 0),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                )

                serviceScope.launch {
                    processQueue()
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

    private suspend fun processQueue() {
        try {
            while (true) {
                val task = taskDao.getNextPending() ?: break

                currentTaskId = task.id

                while (adaptiveManager.currentThermalStatus() >= PowerManager.THERMAL_STATUS_CRITICAL) {
                    delay(5_000)
                }

                val inputSizeBytes = contentResolver
                    .openFileDescriptor(Uri.parse(task.inputUri), "r")
                    ?.use { it.statSize } ?: 0L

                val freeSpaceBytes = StatFs(filesDir.absolutePath).availableBytes

                if (inputSizeBytes > 0L && freeSpaceBytes < inputSizeBytes * 2L) {
                    val failedAt = System.currentTimeMillis()
                    val errorMessage = getString(R.string.error_low_storage)

                    taskDao.finalize(
                        id = task.id,
                        state = STATE_FAILED,
                        completedAt = failedAt,
                        outputUri = null,
                        errorMessage = errorMessage
                    )

                    operationLogRepository.log(
                        operationType = OPERATION_COMPRESS,
                        status = STATUS_FAILED,
                        startedAt = failedAt,
                        durationMs = 0L,
                        inputUri = task.inputUri,
                        outputUri = null,
                        errorMessage = errorMessage
                    )

                    currentTaskId = null
                    continue
                }

                taskDao.updateStateAndStarted(
                    id = task.id,
                    state = STATE_RUNNING,
                    startedAt = System.currentTimeMillis()
                )

                val outputUri = outputFileProvider.createOutputFile(task.inputName)

                if (outputUri == null) {
                    val failedAt = System.currentTimeMillis()
                    val errorMessage = getString(R.string.error_cannot_create_output)

                    taskDao.finalize(
                        id = task.id,
                        state = STATE_FAILED,
                        completedAt = failedAt,
                        outputUri = null,
                        errorMessage = errorMessage
                    )

                    operationLogRepository.log(
                        operationType = OPERATION_COMPRESS,
                        status = STATUS_FAILED,
                        startedAt = failedAt,
                        durationMs = 0L,
                        inputUri = task.inputUri,
                        outputUri = null,
                        errorMessage = errorMessage
                    )

                    currentTaskId = null
                    continue
                }

                val maxVideoBitrate = CompressionPresets.maxBitrateFor(
                    adaptiveManager.deviceProfile.deviceClass
                )

                val compressionMode = CompressionMode.Preset(task.presetId)

                val engine = CompressionEngine(applicationContext)
                currentEngine = engine

                val startedAt = System.currentTimeMillis()

                val outcome = engine.compress(
                    inputUri = Uri.parse(task.inputUri),
                    outputUri = outputUri,
                    mode = compressionMode,
                    maxVideoBitrate = maxVideoBitrate
                ) { progress ->
                    updateNotification(task.inputName, progress)
                }

                val completedAt = System.currentTimeMillis()

                when (outcome) {
                    is CompressionOutcome.Success -> {
                        taskDao.finalize(
                            id = task.id,
                            state = STATE_COMPLETED,
                            completedAt = completedAt,
                            outputUri = outcome.outputUri,
                            errorMessage = null
                        )

                        operationLogRepository.log(
                            operationType = OPERATION_COMPRESS,
                            status = STATUS_SUCCESS,
                            startedAt = startedAt,
                            durationMs = completedAt - startedAt,
                            inputUri = task.inputUri,
                            outputUri = outcome.outputUri,
                            errorMessage = null
                        )
                    }

                    is CompressionOutcome.Failure -> {
                        taskDao.finalize(
                            id = task.id,
                            state = STATE_FAILED,
                            completedAt = completedAt,
                            outputUri = null,
                            errorMessage = outcome.message
                        )

                        operationLogRepository.log(
                            operationType = OPERATION_COMPRESS,
                            status = STATUS_FAILED,
                            startedAt = startedAt,
                            durationMs = completedAt - startedAt,
                            inputUri = task.inputUri,
                            outputUri = null,
                            errorMessage = outcome.message
                        )
                    }

                    is CompressionOutcome.Cancelled -> {
                        taskDao.finalize(
                            id = task.id,
                            state = STATE_CANCELLED,
                            completedAt = completedAt,
                            outputUri = null,
                            errorMessage = null
                        )

                        operationLogRepository.log(
                            operationType = OPERATION_COMPRESS,
                            status = STATUS_CANCELLED,
                            startedAt = startedAt,
                            durationMs = completedAt - startedAt,
                            inputUri = task.inputUri,
                            outputUri = null,
                            errorMessage = null
                        )
                    }
                }

                currentEngine = null
                currentTaskId = null
            }
        } finally {
            isProcessing = false
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.task_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildProgressNotification(taskName: String, progress: Int): android.app.Notification {
        val cancelIntent = Intent(this, TaskService::class.java).apply {
            action = ACTION_CANCEL_ALL
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            3,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_delete,
            getString(R.string.cancel),
            cancelPendingIntent
        ).build()

        val contentText = if (taskName.isEmpty()) {
            getString(R.string.task_notification_text, progress)
        } else {
            "$taskName — ${getString(R.string.task_notification_text, progress)}"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.task_notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .addAction(cancelAction)
            .build()
    }

    private fun updateNotification(taskName: String, progress: Int) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildProgressNotification(taskName, progress)
        )
    }

    companion object {
        private const val CHANNEL_ID = "task_channel"
        private const val NOTIFICATION_ID = 5001

        private const val ACTION_PROCESS = "com.videoforge.android.task.PROCESS"
        private const val ACTION_CANCEL_TASK = "com.videoforge.android.task.CANCEL_TASK"
        private const val ACTION_CANCEL_ALL = "com.videoforge.android.task.CANCEL_ALL"

        private const val EXTRA_TASK_ID = "extra_task_id"

        private const val OPERATION_COMPRESS = "COMPRESS"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_FAILED = "FAILED"
        private const val STATUS_CANCELLED = "CANCELLED"

        private const val STATE_PENDING = "PENDING"
        private const val STATE_RUNNING = "RUNNING"
        private const val STATE_COMPLETED = "COMPLETED"
        private const val STATE_FAILED = "FAILED"
        private const val STATE_CANCELLED = "CANCELLED"

        fun start(context: Context) {
            val intent = Intent(context, TaskService::class.java).apply {
                action = ACTION_PROCESS
            }

            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelTask(context: Context, taskId: String) {
            val intent = Intent(context, TaskService::class.java).apply {
                action = ACTION_CANCEL_TASK
                putExtra(EXTRA_TASK_ID, taskId)
            }

            context.startService(intent)
        }

        fun cancelAll(context: Context) {
            val intent = Intent(context, TaskService::class.java).apply {
                action = ACTION_CANCEL_ALL
            }

            context.startService(intent)
        }
    }
}