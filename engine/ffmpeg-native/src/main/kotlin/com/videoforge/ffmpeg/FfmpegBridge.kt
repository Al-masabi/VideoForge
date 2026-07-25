package com.videoforge.ffmpeg

object FfmpegBridge {

    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("videoforge-ffmpeg")
            true
        } catch (exception: UnsatisfiedLinkError) {
            false
        }
    }

    fun analyzeKeyframes(path: String): LongArray? {
        return if (isAvailable) nativeAnalyzeKeyframes(path) else null
    }

    fun losslessCut(
        inputPath: String,
        outputPath: String,
        segments: Array<LongArray>
    ): Boolean {
        return if (isAvailable) nativeLosslessCut(inputPath, outputPath, segments) else false
    }

    fun encodeCrf(
        inputPath: String,
        outputPath: String,
        crf: Int,
        speed: String
    ): Boolean {
        return if (isAvailable) nativeEncodeCrf(inputPath, outputPath, crf, speed) else false
    }

    private external fun nativeAnalyzeKeyframes(path: String): LongArray?

    private external fun nativeLosslessCut(
        inputPath: String,
        outputPath: String,
        segments: Array<LongArray>
    ): Boolean

    private external fun nativeEncodeCrf(
        inputPath: String,
        outputPath: String,
        crf: Int,
        speed: String
    ): Boolean
}