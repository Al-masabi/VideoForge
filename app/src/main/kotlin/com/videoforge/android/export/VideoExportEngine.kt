package com.videoforge.android.export

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.videoforge.ffmpeg.CutSegment
import com.videoforge.ffmpeg.FfmpegBridge
import com.videoforge.ffmpeg.FfmpegMediaEngine
import com.videoforge.ffmpeg.KeyframeSnap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

data class ExportClip(
    val sourceInMs: Long,
    val sourceOutMs: Long
)

sealed class ExportOutcome {
    abstract val inputUri: String?

    data class Success(
        override val inputUri: String,
        val outputUri: String,
        val strategy: String
    ) : ExportOutcome()

    data class Failure(
        override val inputUri: String?,
        val message: String
    ) : ExportOutcome()

    data class Cancelled(
        override val inputUri: String?
    ) : ExportOutcome()
}

class VideoExportEngine(
    private val context: Context
) {

    private var cancelled = false
    private var currentTransformer: Transformer? = null

    private val ffmpegEngine = FfmpegMediaEngine(context)

    fun cancel() {
        cancelled = true
        currentTransformer?.cancel()
    }

    suspend fun export(
        inputUri: Uri,
        outputUri: Uri,
        clips: List<ExportClip>,
        sourceDurationMs: Long,
        onProgress: (Int) -> Unit
    ): ExportOutcome = withContext(Dispatchers.IO) {
        cancelled = false

        val input = inputUri.toString()

        try {
            when (chooseStrategy(clips, sourceDurationMs)) {
                ExportStrategy.WHOLE_COPY -> {
                    copyWholeFile(inputUri, outputUri, onProgress)
                    ExportOutcome.Success(input, outputUri.toString(), "COPY")
                }

                ExportStrategy.LOSSLESS_SEGMENTS -> {
                    val losslessSucceeded = losslessExport(inputUri, outputUri, clips, onProgress)

                    if (losslessSucceeded) {
                        ExportOutcome.Success(input, outputUri.toString(), "LOSSLESS")
                    } else {
                        transcodeExport(inputUri, outputUri, clips, onProgress)
                        ExportOutcome.Success(input, outputUri.toString(), "TRANSCODE_FALLBACK")
                    }
                }

                ExportStrategy.TRANSCODE -> {
                    transcodeExport(inputUri, outputUri, clips, onProgress)
                    ExportOutcome.Success(input, outputUri.toString(), "TRANSCODE")
                }
            }
        } catch (cancellation: CancellationException) {
            ExportOutcome.Cancelled(input)
        } catch (exception: Exception) {
            ExportOutcome.Failure(input, exception.message ?: "Export failed")
        } finally {
            currentTransformer = null
        }
    }

    private fun chooseStrategy(
        clips: List<ExportClip>,
        sourceDurationMs: Long
    ): ExportStrategy {
        if (clips.isEmpty()) return ExportStrategy.WHOLE_COPY

        if (clips.size == 1) {
            val single = clips.first()

            if (single.sourceInMs <= 0L && single.sourceOutMs >= sourceDurationMs) {
                return ExportStrategy.WHOLE_COPY
            }
        }

        if (FfmpegBridge.isAvailable) {
            return ExportStrategy.LOSSLESS_SEGMENTS
        }

        return ExportStrategy.TRANSCODE
    }

    private suspend fun copyWholeFile(
        inputUri: Uri,
        outputUri: Uri,
        onProgress: (Int) -> Unit
    ) {
        val totalBytes = context.contentResolver
            .openFileDescriptor(inputUri, "r")
            ?.use { it.statSize } ?: 0L

        var copiedBytes = 0L

        context.contentResolver.openInputStream(inputUri)?.use { input ->
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                while (true) {
                    if (cancelled) throw CancellationException("Export cancelled")

                    val read = input.read(buffer)
                    if (read == -1) break

                    output.write(buffer, 0, read)
                    copiedBytes += read

                    if (totalBytes > 0L) {
                        onProgress(((copiedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }

                output.flush()
            }
        }

        onProgress(100)
    }

    private suspend fun losslessExport(
        inputUri: Uri,
        outputUri: Uri,
        clips: List<ExportClip>,
        onProgress: (Int) -> Unit
    ): Boolean {
        val keyframes = ffmpegEngine.analyzeKeyframes(inputUri) ?: return false

        val rawSegments: List<CutSegment> = clips.map { clip ->
            CutSegment(clip.sourceInMs, clip.sourceOutMs)
        }

        val segments = KeyframeSnap.snapToKeyframes(keyframes, rawSegments)

        if (segments.isEmpty()) return false

        return ffmpegEngine.losslessCopySegments(
            uri = inputUri,
            outputUri = outputUri,
            segments = segments
        ) { percent ->
            onProgress(percent)
        }
    }

    private suspend fun transcodeExport(
        inputUri: Uri,
        outputUri: Uri,
        clips: List<ExportClip>,
        onProgress: (Int) -> Unit
    ) {
        val cacheFile = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")

        try {
            val editedMediaItems = clips.map { clip ->
                val mediaItem = MediaItem.Builder()
                    .setUri(inputUri)
                    .setClipStartMs(clip.sourceInMs)
                    .setClipEndMs(clip.sourceOutMs)
                    .build()

                EditedMediaItem.Builder(mediaItem).build()
            }

            val items = editedMediaItems.ifEmpty {
                listOf(EditedMediaItem.Builder(MediaItem.fromUri(inputUri)).build())
            }

            val sequence = EditedMediaItemSequence(items)
            val composition = Composition.Builder(listOf(sequence)).build()

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
            transformer.start(composition, cacheFile.absolutePath)

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
        } finally {
            currentTransformer = null
            cacheFile.delete()
        }
    }

    private enum class ExportStrategy {
        WHOLE_COPY,
        LOSSLESS_SEGMENTS,
        TRANSCODE
    }
}