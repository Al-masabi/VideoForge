package com.videoforge.android.compression

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
import com.videoforge.core.adaptive.AdaptiveManager
import com.videoforge.core.data.logs.OperationLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.videoforge.android.task.CompressionOutcome

@AndroidEntryPoint
class CompressionService : Service() {

    @Inject
    lateinit var operationLogRepository: OperationLogRepository

    @Inject
    lateinit var adaptiveManager: AdaptiveManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var currentEngine: CompressionEngine? = null

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
                val inputUri = intent.getStringExtra(EXTRA_INPUT_URI)
                val outputUri = intent.getStringExtra(EXTRA_OUTPUT_URI)
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID) ?: "balanced"
                val mode = intent.getStringExtra(EXTRA_MODE) ?: "preset"
                val crf = intent.getIntExtra(EXTRA_CRF, 23)
                val crfSpeed = intent.getStringExtra(EXTRA_CRF_SPEED) ?: "fast"
                val targetBytes = intent.getLongExtra(EXTRA_TARGET_BYTES, 0L)

                if (inputUri == null || outputUri == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(
                    NOTIFICATION_ID,
                    buildProgressNotification(0),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                )

                serviceScope.launch {
                    runCompression(inputUri, outputUri, presetId, mode, crf, crfSpeed, targetBytes)
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

    private suspend fun runCompression(
        inputUri: String,
        outputUri: String,
        presetId: String,
        mode: String,
        crf: Int,
        crfSpeed: String,
        targetBytes: Long
    ) {
        val startedAt = System.currentTimeMillis()

        val compressionMode = when (mode) {
            "crf" -> CompressionMode.Crf(crf, crfSpeed)
            "target" -> CompressionMode.TargetSize(targetBytes)
            else -> CompressionMode.Preset(presetId)
        }

        val maxVideoBitrate = CompressionPresets.maxBitrateFor(
            adaptiveManager.deviceProfile.deviceClass
        )

        val engine = CompressionEngine(applicationContext)
        currentEngine = engine

        val outcome = engine.compress(
            inputUri = Uri.parse(inputUri),
            outputUri = Uri.parse(outputUri),
            mode = compressionMode,
            maxVideoBitrate = maxVideoBitrate
        ) { progress ->
            updateNotification(progress)
        }

        val completedAt = System.currentTimeMillis()

        when (outcome) {
            is CompressionOutcome.Success -> {
                operationLogRepository.log(
                    operationType = OPERATION_COMPRESS,
                    status = STATUS_SUCCESS,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = inputUri,
                    outputUri = outcome.outputUri,
                    errorMessage = null
                )

                showResultNotification(getString(R.string.compression_success))
            }

            is CompressionOutcome.Failure -> {
                operationLogRepository.log(
                    operationType = OPERATION_COMPRESS,
                    status = STATUS_FAILED,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = inputUri,
                    outputUri = null,
                    errorMessage = outcome.message
                )

                showResultNotification(getString(R.string.compression_failed))
            }

            is CompressionOutcome.Cancelled -> {
                operationLogRepository.log(
                    operationType = OPERATION_COMPRESS,
                    status = STATUS_CANCELLED,
                    startedAt = startedAt,
                    durationMs = completedAt - startedAt,
                    inputUri = inputUri,
                    outputUri = null,
                    errorMessage = null
                )

                showResultNotification(getString(R.string.compression_cancelled))
            }
        }

        currentEngine = null
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.compression_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildProgressNotification(progress: Int): android.app.Notification {
        val cancelIntent = Intent(this, CompressionService::class.java).apply {
            action = ACTION_CANCEL
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            5,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_delete,
            getString(R.string.cancel),
            cancelPendingIntent
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.compression_notification_title))
            .setContentText(getString(R.string.compression_notification_text, progress))
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
            .setContentTitle(getString(R.string.compression_notification_title))
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .clearActions()
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "compression_channel"
        private const val NOTIFICATION_ID = 7001

        private const val ACTION_START = "com.videoforge.android.compression.START"
        private const val ACTION_CANCEL = "com.videoforge.android.compression.CANCEL"

        private const val EXTRA_INPUT_URI = "extra_input_uri"
        private const val EXTRA_OUTPUT_URI = "extra_output_uri"
        private const val EXTRA_PRESET_ID = "extra_preset_id"
        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_CRF = "extra_crf"
        private const val EXTRA_CRF_SPEED = "extra_crf_speed"
        private const val EXTRA_TARGET_BYTES = "extra_target_bytes"

        private const val OPERATION_COMPRESS = "COMPRESS"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_FAILED = "FAILED"
        private const val STATUS_CANCELLED = "CANCELLED"

        fun start(
            context: Context,
            inputUri: String,
            outputUri: String,
            presetId: String,
            mode: String = "preset",
            crf: Int = 23,
            crfSpeed: String = "fast",
            targetBytes: Long = 0L
        ) {
            val intent = Intent(context, CompressionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INPUT_URI, inputUri)
                putExtra(EXTRA_OUTPUT_URI, outputUri)
                putExtra(EXTRA_PRESET_ID, presetId)
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_CRF, crf)
                putExtra(EXTRA_CRF_SPEED, crfSpeed)
                putExtra(EXTRA_TARGET_BYTES, targetBytes)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, CompressionService::class.java).apply {
                action = ACTION_CANCEL
            }

            context.startService(intent)
        }
    }
}