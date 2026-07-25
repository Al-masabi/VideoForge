package com.videoforge.android.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoforge.core.data.model.OperationLog
import com.videoforge.core.data.logs.OperationLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LogUi(
    val id: String,
    val operationType: String,
    val status: String,
    val startedAt: Long,
    val durationMs: Long,
    val outputUri: String?,
    val errorMessage: String?
)

data class LogsUiState(
    val logs: List<LogUi> = emptyList()
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    operationLogRepository: OperationLogRepository
) : ViewModel() {

    val uiState: StateFlow<LogsUiState> = operationLogRepository.observeAll()
        .map { logs: List<OperationLog> ->
            LogsUiState(
                logs = logs.map { log ->
                    LogUi(
                        id = log.id,
                        operationType = log.operationType,
                        status = log.status,
                        startedAt = log.startedAt,
                        durationMs = log.durationMs,
                        outputUri = log.outputUri,
                        errorMessage = log.errorMessage
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogsUiState()
        )
}