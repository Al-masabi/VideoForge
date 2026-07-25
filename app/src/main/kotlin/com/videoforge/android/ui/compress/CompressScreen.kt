package com.videoforge.android.ui.compress

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoforge.android.R
import com.videoforge.android.compression.CompressionPresets
import com.videoforge.android.compression.CompressionService
import com.videoforge.android.util.formatFileSize
import com.videoforge.core.designsystem.component.VfEmptyState
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar
import kotlin.math.roundToInt

@Composable
fun CompressScreen(
    onBack: () -> Unit,
    viewModel: CompressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val selectedAsset = state.assets.firstOrNull { it.uri == state.selectedAssetUri }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        uri?.let { outputUri ->
            val asset = selectedAsset ?: return@let

            CompressionService.start(
                context = context,
                inputUri = asset.uri,
                outputUri = outputUri.toString(),
                presetId = state.selectedPresetId,
                mode = when (state.params.mode) {
                    CompressionModeUi.PRESET -> "preset"
                    CompressionModeUi.CRF -> "crf"
                    CompressionModeUi.TARGET_SIZE -> "target"
                },
                crf = state.params.crf,
                crfSpeed = state.params.crfSpeed,
                targetBytes = state.params.targetMb * 1024L * 1024L
            )
        }
    }

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.compress_title),
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
            VfSectionHeader(title = stringResource(R.string.select_video_section))

            if (state.assets.isEmpty()) {
                VfEmptyState(
                    message = stringResource(R.string.no_videos_to_compress),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            } else {
                state.assets.forEach { asset ->
                    AssetRow(
                        name = asset.name,
                        sizeLabel = asset.sizeBytes.formatFileSize(),
                        selected = asset.uri == state.selectedAssetUri,
                        onClick = { viewModel.toggleAsset(asset.uri) }
                    )
                }
            }

            VfSectionHeader(title = stringResource(R.string.compression_presets_section))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeSegment(
                    label = stringResource(R.string.compress_mode_preset),
                    selected = state.params.mode == CompressionModeUi.PRESET,
                    onClick = { viewModel.setMode(CompressionModeUi.PRESET) },
                    modifier = Modifier.weight(1f)
                )

                ModeSegment(
                    label = stringResource(R.string.compress_mode_crf),
                    selected = state.params.mode == CompressionModeUi.CRF,
                    onClick = { viewModel.setMode(CompressionModeUi.CRF) },
                    modifier = Modifier.weight(1f)
                )

                ModeSegment(
                    label = stringResource(R.string.compress_mode_target),
                    selected = state.params.mode == CompressionModeUi.TARGET_SIZE,
                    onClick = { viewModel.setMode(CompressionModeUi.TARGET_SIZE) },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = state.params.mode == CompressionModeUi.PRESET,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompressionPresets.ALL.forEach { preset ->
                        PresetRow(
                            name = stringResource(preset.nameRes),
                            description = stringResource(preset.descriptionRes),
                            codecLabel = stringResource(preset.codecLabelRes),
                            selected = preset.id == state.selectedPresetId,
                            onClick = { viewModel.selectPreset(preset.id) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.params.mode == CompressionModeUi.CRF,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CrfPanel(
                    crf = state.params.crf,
                    crfSpeed = state.params.crfSpeed,
                    ffmpegAvailable = state.ffmpegAvailable,
                    onCrfChange = { viewModel.setCrf(it) },
                    onSpeedChange = { viewModel.setCrfSpeed(it) }
                )
            }

            AnimatedVisibility(
                visible = state.params.mode == CompressionModeUi.TARGET_SIZE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TargetSizePanel(
                    targetMb = state.params.targetMb,
                    derivedVideoBitrate = state.derivedVideoBitrate,
                    onTargetChange = { viewModel.setTargetMb(it) }
                )
            }

            EstimationCard(
                sourceBytes = state.sourceBytes,
                estimatedBytes = state.estimatedBytes
            )

            Button(
                onClick = { createDocumentLauncher.launch("videoforge_compressed.mp4") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAsset != null
            ) {
                Text(text = stringResource(R.string.start_compression))
            }
        }
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(stiffness = 500f),
        label = "segment_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = 500f),
        label = "segment_fg"
    )

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(9.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun AssetRow(
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
private fun PresetRow(
    name: String,
    description: String,
    codecLabel: String,
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = name, style = MaterialTheme.typography.titleSmall)

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = codecLabel,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CrfPanel(
    crf: Int,
    crfSpeed: String,
    ffmpegAvailable: Boolean,
    onCrfChange: (Int) -> Unit,
    onSpeedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = crf.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(
                        text = crfQualityLabel(crf),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.compress_mode_crf),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Slider(
                value = crf.toFloat(),
                onValueChange = { onCrfChange(it.roundToInt()) },
                valueRange = 16f..32f,
                steps = 15,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.crf_min_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.crf_max_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CrfSpeedChip("veryfast", stringResource(R.string.crf_speed_veryfast), crfSpeed, onSpeedChange, Modifier.weight(1f))
                CrfSpeedChip("fast", stringResource(R.string.crf_speed_fast), crfSpeed, onSpeedChange, Modifier.weight(1f))
                CrfSpeedChip("medium", stringResource(R.string.crf_speed_medium), crfSpeed, onSpeedChange, Modifier.weight(1f))
                CrfSpeedChip("slow", stringResource(R.string.crf_speed_slow), crfSpeed, onSpeedChange, Modifier.weight(1f))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (ffmpegAvailable) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )

                Text(
                    text = if (ffmpegAvailable) {
                        stringResource(R.string.engine_ffmpeg_ready)
                    } else {
                        stringResource(R.string.engine_fallback)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ffmpegAvailable) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                )
            }
        }
    }
}

@Composable
private fun CrfSpeedChip(
    speed: String,
    label: String,
    selectedSpeed: String,
    onSpeedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = speed == selectedSpeed,
        onClick = { onSpeedChange(speed) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        },
        modifier = modifier
    )
}

@Composable
private fun crfQualityLabel(crf: Int): String {
    return when {
        crf <= 18 -> stringResource(R.string.crf_quality_near_lossless)
        crf <= 21 -> stringResource(R.string.crf_quality_very_high)
        crf <= 24 -> stringResource(R.string.crf_quality_high)
        crf <= 27 -> stringResource(R.string.crf_quality_balanced)
        else -> stringResource(R.string.crf_quality_economy)
    }
}

@Composable
private fun TargetSizePanel(
    targetMb: Int,
    derivedVideoBitrate: Int,
    onTargetChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { onTargetChange(targetMb - 4) }) {
                    Text(text = "−")
                }

                Text(
                    text = "$targetMb MB",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                OutlinedButton(onClick = { onTargetChange(targetMb + 4) }) {
                    Text(text = "+")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8, 16, 32, 64, 128).forEach { quick ->
                    FilterChip(
                        selected = targetMb == quick,
                        onClick = { onTargetChange(quick) },
                        label = { Text(text = "$quick", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.target_bitrate_label,
                    String.format("%.1f Mbps", derivedVideoBitrate / 1_000_000.0)
                ),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (derivedVideoBitrate < 400_000) {
                Text(
                    text = stringResource(R.string.target_too_low),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EstimationCard(
    sourceBytes: Long,
    estimatedBytes: Long,
    modifier: Modifier = Modifier
) {
    val savingsFraction = if (sourceBytes > 0 && estimatedBytes in 1 until sourceBytes) {
        1f - estimatedBytes.toFloat() / sourceBytes
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.estimation_source_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = sourceBytes.formatFileSize(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "←",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.estimation_result_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = estimatedBytes.formatFileSize(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (1f - savingsFraction).coerceIn(0.02f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.estimation_savings_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${(savingsFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}