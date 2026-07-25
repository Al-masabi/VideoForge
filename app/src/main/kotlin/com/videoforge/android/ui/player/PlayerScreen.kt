package com.videoforge.android.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.videoforge.android.R
import com.videoforge.android.ui.shared.vfSharedMediaBounds
import com.videoforge.android.util.formatDuration
import com.videoforge.core.designsystem.component.VfTopBar
import com.videoforge.core.designsystem.theme.PlexMono

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val safeDuration = state.durationMs.coerceAtLeast(1L)

    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        (state.positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            VfTopBar(
                title = state.fileName.ifEmpty { stringResource(R.string.app_name) },
                onBack = onBack,
                backLabel = stringResource(R.string.back)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .vfSharedMediaBounds("media:${state.uri}")
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setKeepScreenOn(true)
                        }
                    },
                    update = { playerView ->
                        playerView.player = viewModel.player
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.thumbnails.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        state.thumbnails.forEach { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Column {
                    Slider(
                        value = currentFraction,
                        onValueChange = { fraction ->
                            isDragging = true
                            dragFraction = fraction
                        },
                        onValueChangeFinished = {
                            viewModel.seekTo((dragFraction * safeDuration).toLong())
                            isDragging = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val shownPosition = if (isDragging) {
                            (dragFraction * safeDuration).toLong()
                        } else {
                            state.positionMs
                        }

                        Text(
                            text = shownPosition.formatDuration(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = PlexMono
                        )

                        Text(
                            text = state.durationMs.formatDuration(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = PlexMono
                        )
                    }
                }

                val pointA = state.pointA
                val pointB = state.pointB

                if (pointA != null && pointB != null) {
                    Text(
                        text = stringResource(
                            R.string.ab_range,
                            pointA.formatDuration(),
                            pointB.formatDuration()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PlexMono,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.stepFrame(-1) }) {
                        Text(text = stringResource(R.string.frame_back))
                    }

                    Button(onClick = { viewModel.togglePlayPause() }) {
                        Text(
                            text = stringResource(
                                if (state.isPlaying) R.string.action_pause else R.string.action_play
                            )
                        )
                    }

                    TextButton(onClick = { viewModel.stepFrame(1) }) {
                        Text(text = stringResource(R.string.frame_forward))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        FilterChip(
                            selected = state.speed == speed,
                            onClick = { viewModel.setSpeed(speed) },
                            label = { Text(text = "${speed}x") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setPointA() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.set_point_a))
                    }

                    OutlinedButton(
                        onClick = { viewModel.setPointB() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.set_point_b))
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearABRepeat() },
                        modifier = Modifier.weight(1f),
                        enabled = pointA != null || pointB != null
                    ) {
                        Text(text = stringResource(R.string.clear_ab))
                    }
                }

                OutlinedButton(
                    onClick = { onOpenEditor(state.uri) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.uri.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.open_editor))
                }
            }
        }
    }
}