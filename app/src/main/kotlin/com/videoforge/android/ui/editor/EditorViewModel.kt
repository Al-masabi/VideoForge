package com.videoforge.android.ui.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.Surface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.videoforge.android.ui.editor.timeline.ScrubFrameDecoder
import com.videoforge.android.ui.editor.timeline.TimelineFrameVisual
import com.videoforge.android.util.formatDuration
import com.videoforge.core.adaptive.AdaptiveManager
import com.videoforge.core.data.editor.EditorClip
import com.videoforge.core.data.editor.EditorRepository
import com.videoforge.core.data.editor.EditorState
import com.videoforge.core.media.WaveformExtractor
import com.videoforge.ffmpeg.CutSegment
import com.videoforge.ffmpeg.FfmpegMediaEngine
import com.videoforge.ffmpeg.KeyframeSnap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject
import com.videoforge.ffmpeg.FfmpegBridge

data class EditorClipUi(
    val id: String,
    val sourceInMs: Long,
    val sourceOutMs: Long,
    val durationMs: Long,
    val ordinal: Int
)

data class EditorMarkerUi(
    val id: String,
    val clipId: String,
    val offsetMs: Long,
    val label: String
)

data class EditorUiState(
    val timelineId: String? = null,
    val assetUri: String = "",
    val fileName: String = "",
    val clips: List<EditorClipUi> = emptyList(),
    val markers: List<EditorMarkerUi> = emptyList(),
    val selectedClipId: String? = null,
    val currentClipIndex: Int = -1,
    val currentClipPositionMs: Long = 0L,
    val timelinePositionMs: Long = 0L,
    val timelineDurationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val losslessAvailable: Boolean? = null,
    val timelineFrames: List<TimelineFrameVisual> = emptyList(),
    val waveformPeaks: FloatArray? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val editorRepository: EditorRepository,
    private val adaptiveManager: AdaptiveManager
) : ViewModel() {

    private val assetUri: String = savedStateHandle.get<String>("uri").orEmpty()

    private var timelineId: String? = null
    private var observeJob: Job? = null
    private var lastPlaylistSignature: String = ""

    private var sourceKeyframes: List<Long>? = null

    private var scrubDecoder: ScrubFrameDecoder? = null
    private var isScrubbingNow = false
    private var wasPlayingBeforeScrub = false

    private val ffmpegEngine: FfmpegMediaEngine by lazy {
        FfmpegMediaEngine(context)
    }

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = editorRepository.getOrCreateTimeline(assetUri)
            timelineId = id
            observeEditor(id)
        }

        viewModelScope.launch {
            while (true) {
                delay(250)
                updatePlaybackPosition()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val peaks = WaveformExtractor.extract(
                context = context,
                uri = Uri.parse(assetUri),
                buckets = 1500
            )

            _uiState.update { it.copy(waveformPeaks = peaks) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (FfmpegBridge.isAvailable) {
                sourceKeyframes = ffmpegEngine.analyzeKeyframes(Uri.parse(assetUri))
                refreshLosslessState()
            }
        }
    }

    fun selectClip(clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun splitSelectedClip() {
        val params = selectedClipActionParams() ?: return

        viewModelScope.launch {
            editorRepository.splitClip(
                timelineId = params.first,
                clipId = params.second,
                offsetMs = params.third
            )
        }
    }

    fun trimStartSelectedClip() {
        val params = selectedClipActionParams() ?: return

        viewModelScope.launch {
            editorRepository.trimClipStart(
                timelineId = params.first,
                clipId = params.second,
                offsetMs = params.third
            )
        }
    }

    fun trimEndSelectedClip() {
        val params = selectedClipActionParams() ?: return

        viewModelScope.launch {
            editorRepository.trimClipEnd(
                timelineId = params.first,
                clipId = params.second,
                offsetMs = params.third
            )
        }
    }

    fun deleteSelectedClip() {
        val params = selectedClipActionParams() ?: return

        viewModelScope.launch {
            editorRepository.deleteClip(
                timelineId = params.first,
                clipId = params.second
            )
        }
    }

    fun addMarkerToSelectedClip() {
        val params = selectedClipActionParams() ?: return
        val label = "علامة " + params.third.formatDuration()

        viewModelScope.launch {
            editorRepository.addMarker(
                timelineId = params.first,
                clipId = params.second,
                offsetMs = params.third,
                label = label
            )
        }
    }

    fun deleteMarker(markerId: String) {
        val timelineId = _uiState.value.timelineId ?: return

        viewModelScope.launch {
            editorRepository.deleteMarker(timelineId, markerId)
        }
    }

    fun undo() {
        val timelineId = _uiState.value.timelineId ?: return

        viewModelScope.launch {
            editorRepository.undo(timelineId)
        }
    }

    fun redo() {
        val timelineId = _uiState.value.timelineId ?: return

        viewModelScope.launch {
            editorRepository.redo(timelineId)
        }
    }

    fun seekTimeline(positionMs: Long) {
        val clips = _uiState.value.clips
        if (clips.isEmpty()) return

        var accumulated = 0L

        clips.forEachIndexed { index, clip ->
            val isLast = index == clips.lastIndex

            if (positionMs < accumulated + clip.durationMs || isLast) {
                val offset = (positionMs - accumulated).coerceIn(0L, clip.durationMs)
                player.seekTo(index, offset)

                if (isScrubbingNow) {
                    requestScrubFrame(positionMs)
                }
                return
            }

            accumulated += clip.durationMs
        }
    }

    fun setScrubbing(scrubbing: Boolean) {
        isScrubbingNow = scrubbing

        if (scrubbing) {
            wasPlayingBeforeScrub = player.isPlaying
            player.pause()
            requestScrubFrame(_uiState.value.timelinePositionMs)
        } else if (wasPlayingBeforeScrub) {
            player.play()
        }

        _uiState.update { it.copy(isPlaying = player.isPlaying) }
    }

    fun attachScrubSurface(surface: Surface) {
        releaseScrubDecoder()

        val decoder = ScrubFrameDecoder(context, Uri.parse(assetUri))

        if (decoder.attach(surface)) {
            scrubDecoder = decoder
            decoder.requestFrame(_uiState.value.timelinePositionMs)
        }
    }

    fun releaseScrubDecoder() {
        scrubDecoder?.release()
        scrubDecoder = null
    }

    fun requestScrubFrame(positionMs: Long) {
        scrubDecoder?.requestFrame(positionMs)
    }

    override fun onCleared() {
        releaseScrubDecoder()
        player.release()
        super.onCleared()
    }

    private fun observeEditor(timelineId: String) {
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            editorRepository.observeEditorState(timelineId).collect { state ->
                onEditorState(state)
            }
        }
    }

    private fun onEditorState(state: EditorState) {
        updatePlayerFromEditor(state)
        updateStateFromEditor(state)
    }

    private fun updatePlayerFromEditor(state: EditorState) {
        val clips = state.clips

        val signature = clips.joinToString("|") { clip ->
            "${clip.id}:${clip.sourceInMs}:${clip.sourceOutMs}:${clip.ordinal}"
        }

        val timelinePosition = currentTimelinePosition()

        if (signature != lastPlaylistSignature) {
            lastPlaylistSignature = signature

            val mediaItems = clips.map { clip ->
                MediaItem.Builder()
                    .setUri(Uri.parse(clip.assetUri))
                    .setClipStartPositionMs(clip.sourceInMs)
                    .setClipEndPositionMs(clip.sourceOutMs)
                    .build()
            }

            if (mediaItems.isEmpty()) {
                player.clearMediaItems()
            } else {
                val (index, offset) = findPositionInClips(clips, timelinePosition)
                val safeIndex = index.coerceIn(0, mediaItems.size - 1)

                player.setMediaItems(
                    mediaItems,
                    safeIndex,
                    offset.coerceAtLeast(0L)
                )
                player.prepare()
            }

            regenerateTimelineFrames(clips)
        }
    }

    private fun updateStateFromEditor(state: EditorState) {
        _uiState.update { current ->
            val selectedClipId = current.selectedClipId ?: state.clips.firstOrNull()?.id

            current.copy(
                timelineId = state.timeline?.id,
                assetUri = state.timeline?.assetUri.orEmpty(),
                fileName = state.timeline?.name.orEmpty(),
                clips = state.clips.map { clip ->
                    EditorClipUi(
                        id = clip.id,
                        sourceInMs = clip.sourceInMs,
                        sourceOutMs = clip.sourceOutMs,
                        durationMs = clip.durationMs,
                        ordinal = clip.ordinal
                    )
                },
                markers = state.markers.map { marker ->
                    EditorMarkerUi(
                        id = marker.id,
                        clipId = marker.clipId,
                        offsetMs = marker.offsetMs,
                        label = marker.label
                    )
                },
                selectedClipId = selectedClipId,
                timelineDurationMs = state.clips.sumOf { it.durationMs },
                canUndo = state.historyIndex > 0,
                canRedo = state.historyIndex < state.historyCount - 1
            )
        }

        refreshLosslessState()
    }

    private fun refreshLosslessState() {
        val keyframes = sourceKeyframes

        if (keyframes == null) {
            _uiState.update { it.copy(losslessAvailable = null) }
            return
        }

        val segments = _uiState.value.clips.map { clip ->
            CutSegment(clip.sourceInMs, clip.sourceOutMs)
        }

        val available = KeyframeSnap.boundariesNearKeyframes(keyframes, segments)

        _uiState.update { it.copy(losslessAvailable = available) }
    }

    private fun updatePlaybackPosition() {
        val clips = _uiState.value.clips
        val index = player.currentMediaItemIndex
        val position = player.currentPosition

        if (clips.isEmpty() || index !in clips.indices) {
            _uiState.update {
                it.copy(
                    currentClipIndex = -1,
                    currentClipPositionMs = 0L,
                    timelinePositionMs = 0L,
                    isPlaying = player.isPlaying
                )
            }
            return
        }

        val clipPosition = position.coerceIn(0L, clips[index].durationMs)
        val timelinePosition = clips.take(index).sumOf { it.durationMs } + clipPosition

        _uiState.update {
            it.copy(
                currentClipIndex = index,
                currentClipPositionMs = clipPosition,
                timelinePositionMs = timelinePosition,
                isPlaying = player.isPlaying
            )
        }
    }

    private fun currentTimelinePosition(): Long {
        val state = _uiState.value
        val clips = state.clips
        val index = player.currentMediaItemIndex

        if (clips.isEmpty() || index !in clips.indices) return 0L

        val clipPosition = player.currentPosition.coerceIn(0L, clips[index].durationMs)

        return clips.take(index).sumOf { it.durationMs } + clipPosition
    }

    private fun findPositionInClips(
        clips: List<EditorClip>,
        timelinePosition: Long
    ): Pair<Int, Long> {
        var accumulated = 0L

        clips.forEachIndexed { index, clip ->
            val duration = clip.durationMs

            if (timelinePosition < accumulated + duration) {
                return index to (timelinePosition - accumulated).coerceAtLeast(0L)
            }

            accumulated += duration
        }

        val lastIndex = (clips.size - 1).coerceAtLeast(0)
        val lastOffset = clips.lastOrNull()?.durationMs ?: 0L

        return lastIndex to lastOffset
    }

    private fun regenerateTimelineFrames(clips: List<EditorClip>) {
        val policy = adaptiveManager.currentPolicy()

        viewModelScope.launch(Dispatchers.IO) {
            val totalDuration = clips.sumOf { it.durationMs }.coerceAtLeast(1L)
            val totalFrames = policy.thumbnailCount * 2

            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, Uri.parse(assetUri))

                var timelineStart = 0L
                val frames = mutableListOf<TimelineFrameVisual>()

                for (clip in clips) {
                    val framesForClip = maxOf(
                        1,
                        ((clip.durationMs.toFloat() / totalDuration) * totalFrames).roundToInt()
                    )

                    for (frameIndex in 0 until framesForClip) {
                        val offsetInClip = ((frameIndex + 0.5) * clip.durationMs / framesForClip).toLong()
                        val sourceTimeMs = clip.sourceInMs + offsetInClip

                        val bitmap = runCatching {
                            retriever.getScaledFrameAtTime(
                                sourceTimeMs * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                policy.thumbnailWidth,
                                policy.thumbnailHeight
                            )
                        }.getOrNull()

                        if (bitmap != null) {
                            frames.add(
                                TimelineFrameVisual(
                                    timelinePositionMs = timelineStart + offsetInClip,
                                    bitmap = bitmap
                                )
                            )
                        }
                    }

                    timelineStart += clip.durationMs
                }

                _uiState.update { it.copy(timelineFrames = frames) }
            } finally {
                retriever.release()
            }
        }
    }

    private fun selectedClipActionParams(): Triple<String, String, Long>? {
        val state = _uiState.value

        val timelineId = state.timelineId ?: return null
        val clipId = state.selectedClipId ?: return null

        val clip = state.clips.find { it.id == clipId } ?: return null

        val offset = if (
            state.currentClipIndex in state.clips.indices &&
            state.clips[state.currentClipIndex].id == clipId
        ) {
            state.currentClipPositionMs
        } else {
            clip.durationMs / 2L
        }

        return Triple(
            timelineId,
            clipId,
            offset.coerceIn(0L, clip.durationMs)
        )
    }
}