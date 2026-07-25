package com.videoforge.android.ui.plugins

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.android.plugin.PluginRegistry
import com.videoforge.core.data.model.MediaAsset
import com.videoforge.core.data.repository.MediaRepository
import com.videoforge.plugin.api.PluginType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PluginReportState(
    val running: Boolean = false,
    val report: String? = null
)

data class PluginUi(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: PluginType,
    val enabled: Boolean,
    val running: Boolean,
    val report: String?
)

data class PluginsUiState(
    val plugins: List<PluginUi> = emptyList(),
    val activeCount: Int = 0,
    val targetAssetName: String? = null,
    val targetAssetUri: String? = null
)

@HiltViewModel
class PluginsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pluginRegistry: PluginRegistry,
    mediaRepository: MediaRepository
) : ViewModel() {

    private val reportsFlow = MutableStateFlow<Map<String, PluginReportState>>(emptyMap())

    val uiState: StateFlow<PluginsUiState> = combine(
        pluginRegistry.enabledIdsFlow,
        mediaRepository.observeRecentAssets(),
        reportsFlow
    ) { enabledIds: Set<String>,
        assets: List<MediaAsset>,
        reports: Map<String, PluginReportState> ->

        val targetAsset = assets.firstOrNull()

        PluginsUiState(
            plugins = pluginRegistry.infos().map { info ->
                val reportState = reports[info.id]

                PluginUi(
                    id = info.id,
                    name = info.name,
                    version = info.version,
                    description = info.description,
                    type = info.type,
                    enabled = enabledIds.contains(info.id),
                    running = reportState?.running ?: false,
                    report = reportState?.report
                )
            },
            activeCount = pluginRegistry.infos().count { info ->
                enabledIds.contains(info.id)
            },
            targetAssetName = targetAsset?.displayName,
            targetAssetUri = targetAsset?.uri
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PluginsUiState()
    )

    fun togglePlugin(id: String, enabled: Boolean) {
        viewModelScope.launch {
            pluginRegistry.setEnabled(id, enabled)
        }
    }

    fun runAnalysis(pluginId: String) {
        val targetUri = uiState.value.targetAssetUri ?: return

        viewModelScope.launch {
            reportsFlow.update { current ->
                current + (pluginId to PluginReportState(running = true, report = null))
            }

            val report = pluginRegistry.runAnalysis(
                pluginId = pluginId,
                context = context,
                uri = Uri.parse(targetUri)
            )

            reportsFlow.update { current ->
                current + (pluginId to PluginReportState(running = false, report = report))
            }
        }
    }
}