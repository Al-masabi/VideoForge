package com.videoforge.android.ui.compress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.android.compression.CompressionPresets
import com.videoforge.core.data.model.MediaAsset
import com.videoforge.core.data.repository.MediaRepository
import com.videoforge.ffmpeg.FfmpegBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CompressAssetUi(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long
)

enum class CompressionModeUi {
    PRESET,
    CRF,
    TARGET_SIZE
}

data class CompressParams(
    val mode: CompressionModeUi = CompressionModeUi.PRESET,
    val crf: Int = 23,
    val crfSpeed: String = "fast",
    val targetMb: Int = 32
)

data class CompressUiState(
    val assets: List<CompressAssetUi> = emptyList(),
    val selectedAssetUri: String? = null,
    val selectedPresetId: String = "fast",
    val params: CompressParams = CompressParams(),
    val ffmpegAvailable: Boolean = false,
    val sourceBytes: Long = 0L,
    val estimatedBytes: Long = 0L,
    val derivedVideoBitrate: Int = 0
)

@HiltViewModel
class CompressViewModel @Inject constructor(
    mediaRepository: MediaRepository
) : ViewModel() {

    private val selectedAssetUriFlow = MutableStateFlow<String?>(null)
    private val selectedPresetIdFlow = MutableStateFlow("fast")
    private val paramsFlow = MutableStateFlow(CompressParams())

    val uiState: StateFlow<CompressUiState> = combine(
        mediaRepository.observeRecentAssets(),
        selectedAssetUriFlow,
        selectedPresetIdFlow,
        paramsFlow
    ) { assets: List<MediaAsset>,
        selectedAssetUri: String?,
        selectedPresetId: String,
        params: CompressParams ->

        val assetUis = assets.map { asset ->
            CompressAssetUi(
                uri = asset.uri,
                name = asset.displayName,
                durationMs = asset.durationMs,
                sizeBytes = asset.sizeBytes
            )
        }

        val selectedAsset = assetUis.firstOrNull { it.uri == selectedAssetUri }
            ?: assetUis.firstOrNull()

        val sourceBitrate = if (selectedAsset != null) {
            CompressionPresets.estimatedSourceBitrate(
                selectedAsset.sizeBytes,
                selectedAsset.durationMs
            )
        } else {
            0
        }

        val derivedVideoBitrate = when (params.mode) {
            CompressionModeUi.PRESET -> {
                val preset = CompressionPresets.byIdOrFirst(selectedPresetId)
                (sourceBitrate * preset.bitrateFactor).toInt()
            }

            CompressionModeUi.CRF -> {
                CompressionPresets.bitrateForCrf(sourceBitrate, params.crf)
            }

            CompressionModeUi.TARGET_SIZE -> {
                CompressionPresets.bitrateForTargetSize(
                    targetBytes = params.targetMb * 1024L * 1024L,
                    durationMs = selectedAsset?.durationMs ?: 0L,
                    audioBitrate = 128_000
                )
            }
        }

        val estimatedBytes = when (params.mode) {
            CompressionModeUi.TARGET_SIZE -> {
                params.targetMb * 1024L * 1024L
            }

            else -> {
                if (selectedAsset != null) {
                    val totalBitrate = derivedVideoBitrate.toLong() + 128_000L
                    totalBitrate * selectedAsset.durationMs / 8000L
                } else {
                    0L
                }
            }
        }

        CompressUiState(
            assets = assetUis,
            selectedAssetUri = selectedAsset?.uri,
            selectedPresetId = selectedPresetId,
            params = params,
            ffmpegAvailable = FfmpegBridge.isAvailable,
            sourceBytes = selectedAsset?.sizeBytes ?: 0L,
            estimatedBytes = estimatedBytes,
            derivedVideoBitrate = derivedVideoBitrate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CompressUiState()
    )

    fun toggleAsset(uri: String) {
        selectedAssetUriFlow.update { current ->
            if (current == uri) null else uri
        }
    }

    fun selectPreset(id: String) {
        selectedPresetIdFlow.value = id
    }

    fun setMode(mode: CompressionModeUi) {
        paramsFlow.update { it.copy(mode = mode) }
    }

    fun setCrf(crf: Int) {
        paramsFlow.update { it.copy(crf = crf.coerceIn(16, 32)) }
    }

    fun setCrfSpeed(speed: String) {
        paramsFlow.update { it.copy(crfSpeed = speed) }
    }

    fun setTargetMb(targetMb: Int) {
        paramsFlow.update { it.copy(targetMb = targetMb.coerceIn(4, 512)) }
    }
}