package com.videoforge.android.compression

import androidx.media3.common.MimeTypes
import com.videoforge.android.R
import com.videoforge.core.adaptive.DeviceClass
import kotlin.math.pow

data class CompressionPresetDef(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val codecMime: String,
    val bitrateFactor: Double,
    val audioBitrate: Int
) {
    val codecLabelRes: Int
        get() = if (codecMime == MimeTypes.VIDEO_H265) {
            R.string.codec_hevc
        } else {
            R.string.codec_h264
        }
}

data class CompressionSettings(
    val videoMime: String,
    val videoBitrate: Int,
    val audioBitrate: Int
)

object CompressionPresets {

    const val MIN_VIDEO_BITRATE = 300_000

    val ALL: List<CompressionPresetDef> = listOf(
        CompressionPresetDef(
            id = "fast",
            nameRes = R.string.preset_fast,
            descriptionRes = R.string.preset_fast_desc,
            codecMime = MimeTypes.VIDEO_H264,
            bitrateFactor = 0.8,
            audioBitrate = 128_000
        ),
        CompressionPresetDef(
            id = "balanced",
            nameRes = R.string.preset_balanced,
            descriptionRes = R.string.preset_balanced_desc,
            codecMime = MimeTypes.VIDEO_H264,
            bitrateFactor = 0.6,
            audioBitrate = 128_000
        ),
        CompressionPresetDef(
            id = "high_quality",
            nameRes = R.string.preset_high_quality,
            descriptionRes = R.string.preset_high_quality_desc,
            codecMime = MimeTypes.VIDEO_H265,
            bitrateFactor = 0.85,
            audioBitrate = 192_000
        ),
        CompressionPresetDef(
            id = "tiny",
            nameRes = R.string.preset_tiny,
            descriptionRes = R.string.preset_tiny_desc,
            codecMime = MimeTypes.VIDEO_H265,
            bitrateFactor = 0.25,
            audioBitrate = 96_000
        )
    )

    fun byIdOrFirst(id: String?): CompressionPresetDef {
        return ALL.firstOrNull { it.id == id } ?: ALL.first()
    }

    fun estimatedSourceBitrate(sizeBytes: Long, durationMs: Long): Int {
        if (durationMs <= 0L) return MIN_VIDEO_BITRATE

        return ((sizeBytes * 8.0) / (durationMs / 1000.0)).toInt()
    }

    fun settingsFor(
        preset: CompressionPresetDef,
        sizeBytes: Long,
        durationMs: Long,
        maxVideoBitrate: Int,
        encoderSupportChecker: (String) -> Boolean
    ): CompressionSettings {
        val sourceBitrate = estimatedSourceBitrate(sizeBytes, durationMs)

        val targetBitrate = (sourceBitrate * preset.bitrateFactor)
            .toInt()
            .coerceIn(MIN_VIDEO_BITRATE, maxVideoBitrate)

        val mime = if (encoderSupportChecker(preset.codecMime)) {
            preset.codecMime
        } else {
            MimeTypes.VIDEO_H264
        }

        return CompressionSettings(
            videoMime = mime,
            videoBitrate = targetBitrate,
            audioBitrate = preset.audioBitrate
        )
    }

    fun bitrateForCrf(sourceBitrate: Int, crf: Int): Int {
        val factor = 2.0.pow((23.0 - crf.toDouble()) / 6.0)

        return (sourceBitrate * factor)
            .toInt()
            .coerceIn(MIN_VIDEO_BITRATE, 20_000_000)
    }

    fun bitrateForTargetSize(
        targetBytes: Long,
        durationMs: Long,
        audioBitrate: Int
    ): Int {
        if (durationMs <= 0L) return MIN_VIDEO_BITRATE

        val durationSeconds = durationMs / 1000.0
        val totalBitrate = (targetBytes * 8.0) / durationSeconds
        val videoBitrate = totalBitrate.toInt() - audioBitrate

        return videoBitrate.coerceAtLeast(MIN_VIDEO_BITRATE)
    }

    fun maxBitrateFor(deviceClass: DeviceClass): Int {
        return when (deviceClass) {
            DeviceClass.LOW -> 12_000_000
            DeviceClass.MID -> 16_000_000
            DeviceClass.HIGH -> 20_000_000
        }
    }
}