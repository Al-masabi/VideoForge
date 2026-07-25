package com.videoforge.android.ui.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.core.data.model.MediaAsset
import com.videoforge.core.data.repository.MediaRepository
import com.videoforge.core.data.task.TaskInfo
import com.videoforge.core.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchAssetUi(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long
)

data class BatchTaskUi(
    val id: String,
    val inputName: String,
    val state: String,
    val progress: Int,
    val outputUri: String?,
    val errorMessage: String?
)

data class BatchUiState(
    val assets: List<BatchAssetUi> = emptyList(),
    val selectedAssetUris: Set<String> = emptySet(),
    val selectedPresetId: String = "balanced",
    val tasks: List<BatchTaskUi> = emptyList()
)

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    mediaRepository: MediaRepository
) : ViewModel() {

    private val selectedAssetUrisFlow = MutableStateFlow<Set<String>>(emptySet())
    private val selectedPresetIdFlow = MutableStateFlow("balanced")

    val uiState: StateFlow<BatchUiState> = combine(
        mediaRepository.observeRecentAssets(),
        taskRepository.observeTasks(),
        selectedAssetUrisFlow,
        selectedPresetIdFlow
    ) { assets: List<MediaAsset>,
        tasks: List<TaskInfo>,
        selectedUris: Set<String>,
        presetId: String ->

        BatchUiState(
            assets = assets.map { asset ->
                BatchAssetUi(
                    uri = asset.uri,
                    name = asset.displayName,
                    durationMs = asset.durationMs,
                    sizeBytes = asset.sizeBytes
                )
            },
            selectedAssetUris = selectedUris,
            selectedPresetId = presetId,
            tasks = tasks.map { task ->
                BatchTaskUi(
                    id = task.id,
                    inputName = task.inputName,
                    state = task.state,
                    progress = task.progress,
                    outputUri = task.outputUri,
                    errorMessage = task.errorMessage
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BatchUiState()
    )

    fun toggleAsset(uri: String) {
        selectedAssetUrisFlow.update { current ->
            if (current.contains(uri)) current - uri else current + uri
        }
    }

    fun selectPreset(id: String) {
        selectedPresetIdFlow.value = id
    }

    fun enqueueSelected() {
        val snapshot = uiState.value
        val selectedAssets = snapshot.assets.filter { asset ->
            snapshot.selectedAssetUris.contains(asset.uri)
        }

        if (selectedAssets.isEmpty()) return

        viewModelScope.launch {
            selectedAssets.forEach { asset ->
                taskRepository.enqueueCompression(
                    inputUri = asset.uri,
                    inputName = asset.name,
                    presetId = snapshot.selectedPresetId
                )
            }

            selectedAssetUrisFlow.value = emptySet()
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.cancelTask(taskId)
        }
    }

    fun cancelAll() {
        viewModelScope.launch {
            taskRepository.cancelAllPending()
        }
    }
}