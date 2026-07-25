package com.videoforge.android.ui.filepicker

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

data class FilePickerAssetUi(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int
)

data class FilePickerUiState(
    val assets: List<FilePickerAssetUi> = emptyList()
)

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<FilePickerUiState> = mediaRepository.observeRecentAssets()
        .map { assets: List<MediaAsset> ->
            FilePickerUiState(
                assets = assets.map { asset ->
                    FilePickerAssetUi(
                        uri = asset.uri,
                        name = asset.displayName,
                        durationMs = asset.durationMs,
                        sizeBytes = asset.sizeBytes,
                        width = asset.width,
                        height = asset.height
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilePickerUiState()
        )
}