package com.videoforge.android.ui.editor.timeline

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.view.Surface
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScrubFrameDecoder(
    private val context: Context,
    private val uri: Uri
) {

    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)

    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null

    @Volatile
    private var attached = false

    fun attach(surface: Surface): Boolean {
        return try {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(context, uri, null)

            var videoTrack = -1
            var videoFormat: MediaFormat? = null

            for (index in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(index)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/")) {
                    videoTrack = index
                    videoFormat = trackFormat
                    break
                }
            }

            if (videoTrack < 0 || videoFormat == null) {
                mediaExtractor.release()
                return false
            }

            mediaExtractor.selectTrack(videoTrack)

            val mime = videoFormat.getString(MediaFormat.KEY_MIME) ?: return false

            val mediaCodec = MediaCodec.createDecoderByType(mime)
            mediaCodec.configure(videoFormat, surface, null, 0)
            mediaCodec.start()

            extractor = mediaExtractor
            codec = mediaCodec
            attached = true

            true
        } catch (exception: Exception) {
            release()
            false
        }
    }

    fun requestFrame(timeMs: Long) {
        if (!attached) return
        if (!busy.compareAndSet(false, true)) return

        executor.execute {
            try {
                decodeTo(timeMs)
            } catch (ignored: Exception) {
            } finally {
                busy.set(false)
            }
        }
    }

    private fun decodeTo(timeMs: Long) {
        val mediaExtractor = extractor ?: return
        val mediaCodec = codec ?: return

        val targetUs = timeMs * 1000L

        mediaExtractor.seekTo(targetUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        mediaCodec.flush()

        val bufferInfo = MediaCodec.BufferInfo()
        val timeoutUs = 8_000L
        val deadline = System.currentTimeMillis() + 400

        var inputDone = false

        while (System.currentTimeMillis() < deadline) {
            if (!inputDone) {
                val inputIndex = mediaCodec.dequeueInputBuffer(timeoutUs)

                if (inputIndex >= 0) {
                    val inputBuffer = mediaCodec.getInputBuffer(inputIndex)

                    if (inputBuffer != null) {
                        val read = mediaExtractor.readSampleData(inputBuffer, 0)

                        if (read < 0) {
                            mediaCodec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            mediaCodec.queueInputBuffer(
                                inputIndex,
                                0,
                                read,
                                mediaExtractor.sampleTime,
                                0
                            )
                            mediaExtractor.advance()
                        }
                    }
                }
            }

            val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs)

            if (outputIndex >= 0) {
                val reachedTarget = bufferInfo.presentationTimeUs >= targetUs

                mediaCodec.releaseOutputBuffer(outputIndex, reachedTarget || inputDone)

                if (reachedTarget) break

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    fun release() {
        attached = false

        executor.execute {
            try {
                codec?.stop()
            } catch (ignored: Exception) {
            }

            try {
                codec?.release()
            } catch (ignored: Exception) {
            }

            try {
                extractor?.release()
            } catch (ignored: Exception) {
            }

            codec = null
            extractor = null
        }

        executor.shutdown()
    }
}