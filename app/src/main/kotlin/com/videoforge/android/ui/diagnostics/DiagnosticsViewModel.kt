package com.videoforge.android.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.core.adaptive.AdaptiveManager
import com.videoforge.core.adaptive.BatteryState
import com.videoforge.core.adaptive.DeviceProfile
import com.videoforge.core.adaptive.DeviceProfiler
import com.videoforge.core.adaptive.PerformancePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val profile: DeviceProfile? = null,
    val thermalStatus: Int = 0,
    val battery: BatteryState = BatteryState(level = 100, charging = true),
    val policy: PerformancePolicy? = null,
    val usedMemoryBytes: Long = 0L,
    val maxMemoryBytes: Long = 0L,
    val codecH264: Boolean = false,
    val codecHevc: Boolean = false,
    val codecAv1: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val adaptiveManager: AdaptiveManager,
    private val deviceProfiler: DeviceProfiler
) : ViewModel() {

    private val runtime = Runtime.getRuntime()

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()

        viewModelScope.launch {
            while (true) {
                delay(1000)
                refresh()
            }
        }
    }

    private fun refresh() {
        val profile = adaptiveManager.deviceProfile
        val policy = adaptiveManager.currentPolicy()
        val thermal = adaptiveManager.currentThermalStatus()
        val battery = adaptiveManager.currentBatteryState()

        _uiState.update {
            it.copy(
                profile = profile,
                thermalStatus = thermal,
                battery = battery,
                policy = policy,
                usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory(),
                maxMemoryBytes = runtime.maxMemory(),
                codecH264 = deviceProfiler.supportsEncoder("video/avc"),
                codecHevc = deviceProfiler.supportsEncoder("video/hevc"),
                codecAv1 = deviceProfiler.supportsEncoder("video/av01")
            )
        }
    }
}