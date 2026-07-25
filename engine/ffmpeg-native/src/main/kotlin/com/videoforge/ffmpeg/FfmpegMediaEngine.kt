package com.videoforge.ffmpeg

import android.content.Context
import android.net.Uri
import com.videoforge.core.subtitle.ClipSegment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegMediaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun analyzeKeyframes(uri: Uri): LongArray? = withContext(Dispatchers.IO) {
        if (!FfmpegBridge.isAvailable) return@withContext null

        val tempFile = copyToTemp(uri, "analyze")

        try {
            FfmpegBridge.analyzeKeyframes(tempFile.absolutePath)
        } finally {
            tempFile.delete()
        }
    }

    suspend fun losslessCopySegments(
        uri: Uri,
        outputUri: Uri,
        segments: List<ClipSegment>,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!FfmpegBridge.isAvailable) return@withContext false

        val inputFile = copyToTemp(uri, "lossless_in")
        val outputFile = File(context.cacheDir, "lossless_out_${System.currentTimeMillis()}.mp4")

        try {
            val segmentsArray = segments.map { segment ->
                longArrayOf(segment.start, segment.end)
            }.toTypedArray()

            val success = FfmpegBridge.losslessCut(
                inputFile.absolutePath,
                outputFile.absolutePath,
                segmentsArray
            )

            if (success) {
                onProgress(100)
                copyToOutput(outputFile, outputUri)
            }

            success
        } finally {
            inputFile.delete()
            outputFile.delete()
        }
    }

    suspend fun encodeWithCrf(
        uri: Uri,
        outputUri: Uri,
        crf: Int,
        speed: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!FfmpegBridge.isAvailable) return@withContext false

        val inputFile = copyToTemp(uri, "crf_in")
        val outputFile = File(context.cacheDir, "crf_out_${System.currentTimeMillis()}.mp4")

        try {
            val success = FfmpegBridge.encodeCrf(
                inputFile.absolutePath,
                outputFile.absolutePath,
                crf,
                speed
            )

            if (success) {
                onProgress(100)
                copyToOutput(outputFile, outputUri)
            }

            success
        } finally {
            inputFile.delete()
            outputFile.delete()
        }
    }

    private fun copyToTemp(uri: Uri, tag: String): File {
        val tempFile = File(context.cacheDir, "${tag}_${System.currentTimeMillis()}.video")

        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun copyToOutput(file: File, outputUri: Uri) {
        file.inputStream().use { input ->
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                input.copyTo(output)
            }
        }
    }
}