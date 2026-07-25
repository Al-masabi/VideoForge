package com.videoforge.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.core.data.model.MediaAsset
import com.videoforge.core.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeMediaItem(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

data class HomeUiState(
    val recentFiles: List<HomeMediaItem> = emptyList(),
    val videosCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = mediaRepository.observeRecentAssets()
        .map { assets: List<MediaAsset> ->
            HomeUiState(
                recentFiles = assets.map { asset ->
                    HomeMediaItem(
                        uri = asset.uri,
                        name = asset.displayName,
                        durationMs = asset.durationMs,
                        width = asset.width,
                        height = asset.height,
                        sizeBytes = asset.sizeBytes
                    )
                },
                videosCount = assets.size,
                totalDurationMs = assets.sumOf { it.durationMs.coerceAtLeast(0L) },
                totalSizeBytes = assets.sumOf { it.sizeBytes.coerceAtLeast(0L) }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}