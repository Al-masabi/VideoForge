package com.videoforge.android.ui.editor.timeline

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class TimelineClipVisual(
    val id: String,
    val timelineStartMs: Long,
    val durationMs: Long,
    val sourceInMs: Long,
    val sourceOutMs: Long,
    val selected: Boolean,
    val markerOffsets: List<Long>
)

data class TimelineFrameVisual(
    val timelinePositionMs: Long,
    val bitmap: Bitmap
)

@Composable
fun TimelineView(
    durationMs: Long,
    clips: List<TimelineClipVisual>,
    frames: List<TimelineFrameVisual>,
    peaks: FloatArray?,
    positionMs: Long,
    isPlaying: Boolean,
    scale: Float,
    onSeek: (Long) -> Unit,
    onScrubbing: (Boolean) -> Unit,
    onSelectClip: (String) -> Unit,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    var scrollPx by remember { mutableFloatStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(0f) }

    val maxScrollPx = max(0f, durationMs / 1000f * scale - widthPx)

    LaunchedEffect(scale, durationMs, widthPx) {
        scrollPx = scrollPx.coerceIn(0f, maxScrollPx)
    }

    LaunchedEffect(positionMs, isPlaying, isScrubbing) {
        if (isPlaying && !isScrubbing && widthPx > 0f) {
            val playheadX = positionMs / 1000f * scale - scrollPx
            if (playheadX > widthPx - 48f || playheadX < 0f) {
                scrollPx = (positionMs / 1000f * scale - widthPx * 0.25f).coerceIn(0f, maxScrollPx)
            }
        }
    }

    val waveReveal = remember { Animatable(0f) }
    LaunchedEffect(peaks) {
        if (peaks != null && waveReveal.value == 0f) {
            waveReveal.animateTo(1f, animationSpec = tween(900))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "playhead_pulse")
    val playheadPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "playhead_pulse_scale"
    )

    val rulerStepSec = remember(scale) {
        val candidates = floatArrayOf(0.5f, 1f, 2f, 5f, 10f, 15f, 30f, 60f, 120f, 300f, 600f)
        candidates.firstOrNull { it * scale >= 80f } ?: 600f
    }

    Column(
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainerHighest)
            .pointerInput(scale, durationMs) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (isScrubbing) return@detectTransformGestures
                    if (zoom != 1f) {
                        val focusTimeSec = (scrollPx + centroid.x) / scale
                        val newScale = (scale * zoom).coerceIn(4f, 900f)
                        onScaleChange(newScale)
                        scrollPx = (focusTimeSec * newScale - centroid.x)
                            .coerceIn(0f, max(0f, durationMs / 1000f * newScale - size.width))
                    } else if (pan.x != 0f) {
                        scrollPx = (scrollPx - pan.x).coerceIn(0f, maxScrollPx)
                    }
                }
            }
            .pointerInput(scale, durationMs, positionMs, clips) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val playheadX = positionMs / 1000f * scale - scrollPx
                    val grabRadius = 26.dp.toPx()

                    if (abs(down.position.x - playheadX) <= grabRadius) {
                        isScrubbing = true
                        onScrubbing(true)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            val targetMs = ((scrollPx + change.position.x) / scale * 1000f)
                                .coerceIn(0f, durationMs.toFloat())
                                .toLong()
                            onSeek(targetMs)
                            change.consume()
                        }

                        isScrubbing = false
                        onScrubbing(false)
                    } else {
                        val slop = viewConfiguration.touchSlop
                        var moved = false
                        var upPosition = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                upPosition = change.position
                                break
                            }
                            if ((change.position - down.position).getDistance() > slop) {
                                moved = true
                                break
                            }
                        }

                        if (!moved) {
                            val targetMs = ((scrollPx + upPosition.x) / scale * 1000f)
                                .coerceIn(0f, durationMs.toFloat())
                                .toLong()
                            onSeek(targetMs)
                            clips.firstOrNull {
                                targetMs >= it.timelineStartMs &&
                                    targetMs < it.timelineStartMs + it.durationMs
                            }?.let { onSelectClip(it.id) }
                        }
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                ) {
                    drawRect(color = colors.surfaceContainer)
                    val baselineY = size.height - 1.dp.toPx()
                    drawLine(
                        colors.outlineVariant,
                        Offset(0f, baselineY),
                        Offset(size.width, baselineY),
                        1.dp.toPx()
                    )

                    val labelPaint = Paint().apply {
                        color = colors.onSurfaceVariant.toArgb()
                        textSize = 9.sp.toPx()
                        typeface = Typeface.MONOSPACE
                        isAntiAlias = true
                    }

                    val stepPx = rulerStepSec * scale
                    val firstVisibleSec = scrollPx / scale
                    val startSec = kotlin.math.floor(firstVisibleSec / rulerStepSec) * rulerStepSec
                    var second = startSec

                    while (second * scale - scrollPx < size.width + stepPx) {
                        val x = second * scale - scrollPx
                        if (x >= -stepPx) {
                            drawLine(
                                colors.onSurfaceVariant.copy(alpha = 0.7f),
                                Offset(x, baselineY),
                                Offset(x, baselineY - 8.dp.toPx()),
                                1.dp.toPx()
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                formatRulerLabel(second),
                                x + 3.dp.toPx(),
                                baselineY - 10.dp.toPx(),
                                labelPaint
                            )
                            for (minor in 1..4) {
                                val minorX = (second + rulerStepSec * minor / 5f) * scale - scrollPx
                                if (minorX in 0f..size.width) {
                                    drawLine(
                                        colors.outlineVariant,
                                        Offset(minorX, baselineY),
                                        Offset(minorX, baselineY - 4.dp.toPx()),
                                        1.dp.toPx()
                                    )
                                }
                            }
                        }
                        second += rulerStepSec
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                ) {
                    drawRect(color = colors.surfaceContainerHighest)
                    val blockTop = 12.dp.toPx()
                    val blockBottom = size.height - 12.dp.toPx()

                    for (clip in clips) {
                        val x0 = clip.timelineStartMs / 1000f * scale - scrollPx
                        val blockWidth = clip.durationMs / 1000f * scale
                        if (x0 + blockWidth < 0f || x0 > size.width) continue

                        val blockRect = Rect(x0, blockTop, x0 + blockWidth, blockBottom)

                        clipRect(
                            left = blockRect.left,
                            top = blockRect.top,
                            right = blockRect.right,
                            bottom = blockRect.bottom
                        ) {
                            drawRect(color = if (clip.selected) colors.primaryContainer else colors.surfaceVariant)

                            val clipFrames = frames.filter {
                                it.timelinePositionMs >= clip.timelineStartMs &&
                                    it.timelinePositionMs < clip.timelineStartMs + clip.durationMs
                            }

                            clipFrames.forEachIndexed { index, frame ->
                                val frameX = frame.timelinePositionMs / 1000f * scale - scrollPx
                                val nextX = if (index == clipFrames.lastIndex) {
                                    x0 + blockWidth
                                } else {
                                    clipFrames[index + 1].timelinePositionMs / 1000f * scale - scrollPx
                                }
                                val dstLeft = max(frameX, x0)
                                val dstRight = max(nextX, frameX + 1f)
                                drawImageRect(
                                    image = frame.bitmap.asImageBitmap(),
                                    srcOffset = IntOffset.Zero,
                                    srcSize = IntSize(frame.bitmap.width, frame.bitmap.height),
                                    dstOffset = IntOffset(dstLeft.roundToInt(), blockTop.roundToInt()),
                                    dstSize = IntSize(
                                        max(1f, dstRight - dstLeft).roundToInt(),
                                        (blockBottom - blockTop).roundToInt()
                                    ),
                                    filterQuality = FilterQuality.Low
                                )
                            }
                        }

                        val holeWidth = 5.dp.toPx()
                        val holeHeight = 3.5.dp.toPx()
                        val holeGap = 12.dp.toPx()
                        var holeX = x0 + 6.dp.toPx()
                        while (holeX + holeWidth < x0 + blockWidth - 4.dp.toPx()) {
                            drawRoundRect(
                                colors.surfaceContainerHighest,
                                Offset(holeX, blockTop + 3.dp.toPx()),
                                Size(holeWidth, holeHeight),
                                CornerRadius(2.dp.toPx())
                            )
                            drawRoundRect(
                                colors.surfaceContainerHighest,
                                Offset(holeX, blockBottom - 3.dp.toPx() - holeHeight),
                                Size(holeWidth, holeHeight),
                                CornerRadius(2.dp.toPx())
                            )
                            holeX += holeWidth + holeGap
                        }

                        drawRoundRect(
                            color = if (clip.selected) colors.primary else colors.outlineVariant,
                            topLeft = Offset(x0, blockTop),
                            size = Size(blockWidth, blockBottom - blockTop),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = if (clip.selected) 2.5.dp.toPx() else 1.dp.toPx())
                        )

                        if (blockWidth > 64.dp.toPx()) {
                            val durationPaint = Paint().apply {
                                color = colors.onSurface.toArgb()
                                textSize = 9.sp.toPx()
                                typeface = Typeface.MONOSPACE
                                isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                formatClipDuration(clip.durationMs),
                                x0 + 6.dp.toPx(),
                                blockBottom - 6.dp.toPx(),
                                durationPaint
                            )
                        }

                        for (markerOffset in clip.markerOffsets) {
                            val markerX = (clip.timelineStartMs + markerOffset) / 1000f * scale - scrollPx
                            if (markerX < x0 || markerX > x0 + blockWidth) continue
                            val markerY = blockTop + 10.dp.toPx()
                            val diamond = Path().apply {
                                moveTo(markerX, markerY - 5.dp.toPx())
                                lineTo(markerX + 4.dp.toPx(), markerY)
                                lineTo(markerX, markerY + 5.dp.toPx())
                                lineTo(markerX - 4.dp.toPx(), markerY)
                                close()
                            }
                            drawPath(diamond, colors.tertiary)
                        }
                    }

                    for (index in 0 until clips.size - 1) {
                        val gapStartX = (clips[index].timelineStartMs + clips[index].durationMs) /
                            1000f * scale - scrollPx
                        val gapEndX = clips[index + 1].timelineStartMs / 1000f * scale - scrollPx
                        val gapWidth = gapEndX - gapStartX
                        if (gapWidth in 6f..40.dp.toPx()) {
                            val centerX = (gapStartX + gapEndX) / 2f
                            val centerY = (blockTop + blockBottom) / 2f
                            val arm = 4.dp.toPx()
                            drawLine(
                                colors.error.copy(alpha = 0.8f),
                                Offset(centerX - arm, centerY - arm),
                                Offset(centerX + arm, centerY + arm),
                                1.5.dp.toPx()
                            )
                            drawLine(
                                colors.error.copy(alpha = 0.8f),
                                Offset(centerX - arm, centerY + arm),
                                Offset(centerX + arm, centerY - arm),
                                1.5.dp.toPx()
                            )
                        }
                    }
                }

                if (peaks != null) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val midY = size.height / 2f
                        drawLine(
                            colors.primary.copy(alpha = 0.2f),
                            Offset(0f, midY),
                            Offset(size.width, midY),
                            1.dp.toPx()
                        )
                        val reveal = waveReveal.value
                        if (reveal > 0f) {
                            val step = 2.dp.toPx()
                            var x = 0f
                            while (x < size.width) {
                                val timelineMs = (scrollPx + x) / scale * 1000f
                                val clip = clips.firstOrNull {
                                    timelineMs >= it.timelineStartMs &&
                                        timelineMs < it.timelineStartMs + it.durationMs
                                }
                                if (clip != null) {
                                    val sourceMs = clip.sourceInMs + (timelineMs - clip.timelineStartMs)
                                    val sourceDuration = (clip.sourceOutMs - clip.sourceInMs).coerceAtLeast(1L)
                                    val peakIndex = (sourceMs * peaks.size / sourceDuration)
                                        .toInt()
                                        .coerceIn(0, peaks.size - 1)
                                    val amplitude = peaks[peakIndex] * midY * 0.92f * reveal
                                    if (amplitude > 0.5f) {
                                        drawLine(
                                            colors.primary.copy(alpha = 0.55f),
                                            Offset(x, midY - amplitude),
                                            Offset(x, midY + amplitude),
                                            1.6.dp.toPx()
                                        )
                                    }
                                }
                                x += step
                            }
                        }
                    }
                }
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val playheadX = positionMs / 1000f * scale - scrollPx
                if (playheadX >= -4f && playheadX <= size.width + 4f) {
                    drawLine(
                        colors.primary,
                        Offset(playheadX, 0f),
                        Offset(playheadX, size.height),
                        2.dp.toPx()
                    )
                    val handleRadius = 6.5.dp.toPx() * (if (isScrubbing) playheadPulse else 1f)
                    drawCircle(colors.primary, handleRadius, Offset(playheadX, 10.dp.toPx()))
                    drawCircle(colors.onPrimary, handleRadius * 0.45f, Offset(playheadX, 10.dp.toPx()))
                }
            }
        }
    }
}

private fun formatRulerLabel(seconds: Float): String {
    val total = seconds.roundToInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs) else "%02d:%02d".format(minutes, secs)
}

private fun formatClipDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}