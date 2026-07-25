package com.videoforge.ffmpeg

import com.videoforge.core.subtitle.ClipSegment
import kotlin.math.abs

typealias CutSegment = ClipSegment

object KeyframeSnap {

    fun snapToKeyframes(
        keyframes: List<Long>,
        segments: List<ClipSegment>
    ): List<ClipSegment> {
        if (keyframes.isEmpty()) return segments

        return segments.map { segment ->
            val snappedStart = keyframes.minByOrNull { abs(it - segment.start) } ?: segment.start
            val snappedEnd = keyframes.minByOrNull { abs(it - segment.end) } ?: segment.end

            ClipSegment(
                start = minOf(snappedStart, snappedEnd),
                end = maxOf(snappedStart, snappedEnd)
            )
        }.filter { it.end > it.start }
    }

    fun boundariesNearKeyframes(
        keyframes: List<Long>,
        segments: List<ClipSegment>,
        toleranceMs: Long = 2000L
    ): Boolean {
        if (keyframes.isEmpty()) return false

        return segments.all { segment ->
            val startNear = keyframes.any { abs(it - segment.start) <= toleranceMs }
            val endNear = keyframes.any { abs(it - segment.end) <= toleranceMs }

            startNear && endNear
        }
    }
}