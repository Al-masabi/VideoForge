package com.videoforge.android.compression

import android.content.Context
import android.media.MediaCodecList
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.EncodingSettings
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.videoforge.ffmpeg.FfmpegBridge
import com.videoforge.ffmpeg.FfmpegMediaEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

sealed class CompressionMode {
    data class Preset(val presetId: String) : CompressionMode()
    data class Crf(val crf: Int, val speed: String) : CompressionMode()
    data class TargetSize(val targetBytes: Long) : CompressionMode()
}

class CompressionEngine(
    private val context: Context
) {

    private var cancelled = false
    private var currentTransformer: Transformer? = null

    private val ffmpegEngine = FfmpegMediaEngine(context)

    private val encoderMimes: Set<String> by lazy {
        MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .filter { it.isEncoder }
            .flatMap { it.supportedTypes.toList() }
            .map { it.lowercase() }
            .toSet()
    }

    fun cancel() {
        cancelled = true
        currentTransformer?.cancel()
    }

    suspend fun compress(
        inputUri: Uri,
        outputUri: Uri,
        mode: CompressionMode,
        maxVideoBitrate: Int,
        onProgress: (Int) -> Unit
    ): CompressionOutcome = withContext(Dispatchers.IO) {
        cancelled = false

        val input = inputUri.toString()
        val cacheFile = File(context.cacheDir, "compress_${System.currentTimeMillis()}.mp4")

        try {
            val sizeBytes = context.contentResolver
                .openFileDescriptor(inputUri, "r")
                ?.use { it.statSize } ?: 0L

            val durationMs = readDuration(inputUri)

            when (mode) {
                is CompressionMode.Crf -> {
                    val crfSucceeded = FfmpegBridge.isAvailable &&
                        ffmpegEngine.encodeWithCrf(
                            uri = inputUri,
                            outputUri = outputUri,
                            crf = mode.crf,
                            speed = mode.speed
                        ) { percent -> onProgress(percent) }

                    if (crfSucceeded) {
                        return@withContext CompressionOutcome.Success(input, outputUri.toString())
                    }

                    val sourceBitrate = CompressionPresets.estimatedSourceBitrate(sizeBytes, durationMs)
                    val fallbackBitrate = CompressionPresets
                        .bitrateForCrf(sourceBitrate, mode.crf)
                        .coerceAtMost(maxVideoBitrate)

                    transcodeWithBitrate(inputUri, outputUri, cacheFile, fallbackBitrate, 128_000, onProgress)
                }

                is CompressionMode.TargetSize -> {
                    val videoBitrate = CompressionPresets
                        .bitrateForTargetSize(mode.targetBytes, durationMs, 128_000)
                        .coerceAtMost(maxVideoBitrate)

                    transcodeWithBitrate(inputUri, outputUri, cacheFile, videoBitrate, 128_000, onProgress)
                }

                is CompressionMode.Preset -> {
                    val preset = CompressionPresets.byIdOrFirst(mode.presetId)

                    val settings = CompressionPresets.settingsFor(
                        preset = preset,
                        sizeBytes = sizeBytes,
                        durationMs = durationMs,
                        maxVideoBitrate = maxVideoBitrate,
                        encoderSupportChecker = { mime -> encoderMimes.contains(mime.lowercase()) }
                    )

                    transcodeWithBitrate(
                        inputUri,
                        outputUri,
                        cacheFile,
                        settings.videoBitrate,
                        settings.audioBitrate,
                        onProgress
                    )
                }
            }

            CompressionOutcome.Success(input, outputUri.toString())
        } catch (exception: Exception) {
            CompressionOutcome.Failure(input, exception.message ?: "Compression failed")
        } finally {
            currentTransformer = null
            cacheFile.delete()
        }
    }

    private suspend fun transcodeWithBitrate(
        inputUri: Uri,
        outputUri: Uri,
        cacheFile: File,
        videoBitrate: Int,
        audioBitrate: Int,
        onProgress: (Int) -> Unit
    ) {
        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
        val sequence = EditedMediaItemSequence(listOf(editedMediaItem))

        val completion = CompletableDeferred<Result<Unit>>()

        val transformer = Transformer.Builder(context)
            .setEncodingSettings(
                EncodingSettings.Builder()
                    .setBitrate(videoBitrate)
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    exportResult: androidx.media3.transformer.ExportResult
                ) {
                    completion.complete(Result.success(Unit))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: androidx.media3.transformer.ExportResult,
                    exportException: ExportException
                ) {
                    completion.complete(Result.failure(exportException))
                }
            })
            .build()

        currentTransformer = transformer
        transformer.start(sequence, cacheFile.absolutePath)

        val progressHolder = ProgressHolder()

        while (completion.isActive) {
            delay(500)

            if (cancelled) {
                transformer.cancel()
                return
            }

            val progressState = transformer.getProgress(progressHolder)

            if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                onProgress(progressHolder.progress.coerceIn(0, 100))
            }
        }

        completion.await().getOrThrow()

        onProgress(100)

        cacheFile.inputStream().use { input ->
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun readDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (exception: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}