package com.videoforge.android.compression

import android.content.Context
import android.media.MediaCodecList
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.videoforge.android.task.CompressionOutcome
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

                    transcode(inputUri, outputUri, cacheFile, onProgress)
                }

                is CompressionMode.TargetSize -> {
                    transcode(inputUri, outputUri, cacheFile, onProgress)
                }

                is CompressionMode.Preset -> {
                    transcode(inputUri, outputUri, cacheFile, onProgress)
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

    private suspend fun transcode(
        inputUri: Uri,
        outputUri: Uri,
        cacheFile: File,
        onProgress: (Int) -> Unit
    ) {
        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

        val completion = CompletableDeferred<Result<Unit>>()

        val transformer = Transformer.Builder(context)
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
        transformer.start(editedMediaItem, cacheFile.absolutePath)

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
}