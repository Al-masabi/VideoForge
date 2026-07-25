package com.videoforge.android.ui.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoforge.android.R
import com.videoforge.android.compression.CompressionPresets
import com.videoforge.android.task.TaskService
import com.videoforge.android.util.formatFileSize
import com.videoforge.core.designsystem.component.VfEmptyState
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar

@Composable
fun BatchScreen(
    onBack: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val selectedCount = state.selectedAssetUris.size
    val activeTasks = state.tasks.count { it.state == "PENDING" || it.state == "RUNNING" }

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.batch_title),
                onBack = onBack,
                backLabel = stringResource(R.string.back)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VfSectionHeader(title = stringResource(R.string.batch_add_section))

            if (state.assets.isEmpty()) {
                VfEmptyState(
                    message = stringResource(R.string.no_videos_to_compress),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            } else {
                state.assets.forEach { asset ->
                    BatchAssetRow(
                        name = asset.name,
                        sizeLabel = asset.sizeBytes.formatFileSize(),
                        selected = state.selectedAssetUris.contains(asset.uri),
                        onClick = { viewModel.toggleAsset(asset.uri) }
                    )
                }
            }

            VfSectionHeader(title = stringResource(R.string.batch_settings_section))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompressionPresets.ALL.forEach { preset ->
                    FilterChip(
                        selected = preset.id == state.selectedPresetId,
                        onClick = { viewModel.selectPreset(preset.id) },
                        label = {
                            Text(
                                text = stringResource(preset.nameRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.enqueueSelected()
                    TaskService.start(context)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCount > 0
            ) {
                Text(text = stringResource(R.string.batch_enqueue_count, selectedCount))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VfSectionHeader(
                    title = stringResource(R.string.batch_tasks_count, state.tasks.size),
                    modifier = Modifier.weight(1f)
                )

                if (activeTasks > 0) {
                    TextButton(onClick = {
                        viewModel.cancelAll()
                        TaskService.cancelAll(context)
                    }) {
                        Text(text = stringResource(R.string.batch_cancel_all))
                    }
                }
            }

            if (state.tasks.isEmpty()) {
                VfEmptyState(
                    message = stringResource(R.string.batch_empty_queue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            } else {
                state.tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onCancel = {
                            viewModel.cancelTask(task.id)
                            TaskService.cancelTask(context, task.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchAssetRow(
    name: String,
    sizeLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: BatchTaskUi,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateLabel = when (task.state) {
        "RUNNING" -> stringResource(R.string.task_state_running)
        "COMPLETED" -> stringResource(R.string.task_state_completed)
        "FAILED" -> stringResource(R.string.task_state_failed)
        "CANCELLED" -> stringResource(R.string.task_state_cancelled)
        else -> stringResource(R.string.task_state_pending)
    }

    val stateColor = when (task.state) {
        "RUNNING" -> MaterialTheme.colorScheme.primary
        "COMPLETED" -> MaterialTheme.colorScheme.tertiary
        "FAILED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isActive = task.state == "PENDING" || task.state == "RUNNING"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.inputName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = stateColor
                )
            }

            if (task.state == "RUNNING" || task.state == "PENDING") {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${task.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.state == "FAILED" && task.errorMessage != null) {
                Text(
                    text = task.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isActive) {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }
}