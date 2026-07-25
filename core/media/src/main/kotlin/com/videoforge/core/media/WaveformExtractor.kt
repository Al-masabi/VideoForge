package com.videoforge.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlin.math.abs

object WaveformExtractor {

    fun extract(
        context: Context,
        uri: Uri,
        buckets: Int
    ): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)

            var audioTrack = -1
            var audioFormat: MediaFormat? = null

            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("audio/")) {
                    audioTrack = index
                    audioFormat = format
                    break
                }
            }

            if (audioTrack < 0 || audioFormat == null) return null

            extractor.selectTrack(audioTrack)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return null

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }
                ?: return null

            val peaks = FloatArray(buckets)
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10_000L

            var inputDone = false
            var outputDone = false
            var iterations = 0
            val maxIterations = 500_000

            while (!outputDone && iterations++ < maxIterations) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(timeoutUs)

                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)

                        if (inputBuffer != null) {
                            val read = extractor.readSampleData(inputBuffer, 0)

                            if (read < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    read,
                                    extractor.sampleTime,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)

                if (outputIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)

                        if (outputBuffer != null) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            val samples = outputBuffer.asShortBuffer()
                            var maxAmplitude = 0

                            while (samples.hasRemaining()) {
                                val value = abs(samples.get().toInt())
                                if (value > maxAmplitude) maxAmplitude = value
                            }

                            val bucket = (bufferInfo.presentationTimeUs * buckets / durationUs)
                                .toInt()
                                .coerceIn(0, buckets - 1)

                            val normalized = maxAmplitude / 32768f

                            if (normalized > peaks[bucket]) {
                                peaks[bucket] = normalized
                            }
                        }
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            return peaks
        } catch (exception: Exception) {
            return null
        } finally {
            try {
                codec?.stop()
            } catch (ignored: Exception) {
            }

            try {
                codec?.release()
            } catch (ignored: Exception) {
            }

            extractor.release()
        }
    }
}