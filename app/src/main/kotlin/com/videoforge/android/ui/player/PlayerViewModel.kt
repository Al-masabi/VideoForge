package com.videoforge.android.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val uri: String = "",
    val fileName: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val pointA: Long? = null,
    val pointB: Long? = null,
    val thumbnails: List<Bitmap> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val uri: Uri = Uri.parse(savedStateHandle.get<String>("uri").orEmpty())

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            uri = uri.toString(),
            fileName = uri.lastPathSegment.orEmpty()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true

        viewModelScope.launch {
            while (true) {
                delay(250)
                syncPlaybackState()
                enforceABRepeat()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            loadThumbnails()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        syncPlaybackState()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        syncPlaybackState()
    }

    fun stepFrame(direction: Int) {
        seekTo(player.currentPosition + direction * FRAME_DURATION_MS)
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        syncPlaybackState()
    }

    fun setPointA() {
        val position = player.currentPosition

        _uiState.update { state ->
            if (state.pointB != null && position >= state.pointB) {
                state.copy(pointA = position, pointB = null)
            } else {
                state.copy(pointA = position)
            }
        }
    }

    fun setPointB() {
        val position = player.currentPosition

        _uiState.update { state ->
            if (state.pointA != null && position <= state.pointA) {
                state.copy(pointA = null, pointB = position)
            } else {
                state.copy(pointB = position)
            }
        }
    }

    fun clearABRepeat() {
        _uiState.update { it.copy(pointA = null, pointB = null) }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private fun syncPlaybackState() {
        _uiState.update {
            it.copy(
                positionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L),
                isPlaying = player.isPlaying,
                speed = player.playbackParameters.speed
            )
        }
    }

    private fun enforceABRepeat() {
        val pointA = _uiState.value.pointA
        val pointB = _uiState.value.pointB

        if (pointA != null && pointB != null && pointB > pointA &&
            player.isPlaying && player.currentPosition >= pointB
        ) {
            player.seekTo(pointA)
        }
    }

    private fun loadThumbnails() {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            if (durationMs > 0L) {
                val frames = (0 until THUMBNAIL_COUNT).mapNotNull { index ->
                    val timeUs = (((index + 0.5) * durationMs / THUMBNAIL_COUNT) * 1000L).toLong()

                    runCatching {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            THUMBNAIL_WIDTH,
                            THUMBNAIL_HEIGHT
                        )
                    }.getOrNull()
                }

                _uiState.update { it.copy(thumbnails = frames) }
            }
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val FRAME_DURATION_MS = 33L
        private const val THUMBNAIL_COUNT = 8
        private const val THUMBNAIL_WIDTH = 320
        private const val THUMBNAIL_HEIGHT = 180
    }
}