package com.videoforge.android.ui.diagnostics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoforge.android.R
import com.videoforge.android.util.formatFileSize
import com.videoforge.core.adaptive.DeviceClass
import com.videoforge.core.adaptive.PerformancePolicy
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.diagnostics_title),
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
            DeviceClassHero(state)

            VfSectionHeader(title = stringResource(R.string.live_meters_label))
            LiveMetersCard(state)

            VfSectionHeader(title = stringResource(R.string.policy_label))
            PolicyCard(policy = state.policy)

            VfSectionHeader(title = stringResource(R.string.codecs_label))
            CodecsCard(state)
        }
    }
}

@Composable
private fun DeviceClassHero(
    state: DiagnosticsUiState,
    modifier: Modifier = Modifier
) {
    val profile = state.profile

    val deviceClassName = when (profile?.deviceClass) {
        DeviceClass.LOW -> stringResource(R.string.device_class_low)
        DeviceClass.MID -> stringResource(R.string.device_class_mid)
        DeviceClass.HIGH -> stringResource(R.string.device_class_high)
        null -> "…"
    }

    val deviceClassColor = when (profile?.deviceClass) {
        DeviceClass.LOW -> Color(0xFFE8A33D)
        DeviceClass.MID -> Color(0xFF63C9B8)
        DeviceClass.HIGH -> Color(0xFF2E7D32)
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
            Text(
                text = stringResource(R.string.device_class_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = deviceClassName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = deviceClassColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatBlock(
                    value = if (profile != null) {
                        String.format("%.1f GB", profile.totalRamBytes / 1_073_741_824.0)
                    } else {
                        "…"
                    },
                    label = stringResource(R.string.ram_label),
                    modifier = Modifier.weight(1f)
                )

                StatBlock(
                    value = profile?.cpuCores?.toString() ?: "…",
                    label = stringResource(R.string.cores_label),
                    modifier = Modifier.weight(1f)
                )

                StatBlock(
                    value = profile?.cpuAbi ?: "…",
                    label = stringResource(R.string.abi_label),
                    modifier = Modifier.weight(1f)
                )

                StatBlock(
                    value = profile?.freeStorageBytes?.formatFileSize() ?: "…",
                    label = stringResource(R.string.free_storage_label),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveMetersCard(
    state: DiagnosticsUiState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val thermalColor = when {
        state.thermalStatus <= 1 -> Color(0xFF2E7D32)
        state.thermalStatus == 2 -> Color(0xFFF9A825)
        state.thermalStatus == 3 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    val memoryFraction by animateFloatAsState(
        targetValue = if (state.maxMemoryBytes > 0) {
            (state.usedMemoryBytes.toFloat() / state.maxMemoryBytes).coerceIn(0f, 1f)
        } else {
            0f
        },
        label = "memory_fraction"
    )

    val batteryFraction by animateFloatAsState(
        targetValue = state.battery.level / 100f,
        label = "battery_fraction"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MeterRow(
                label = stringResource(R.string.thermal_label),
                valueText = thermalLabel(state.thermalStatus),
                valueColor = thermalColor
            ) {
                ThermalSegments(status = state.thermalStatus, color = thermalColor)
            }

            MeterRow(
                label = stringResource(R.string.battery_label),
                valueText = "${state.battery.level}% " +
                    (if (state.battery.charging) {
                        stringResource(R.string.charging)
                    } else {
                        stringResource(R.string.not_charging)
                    }),
                valueColor = colors.onSurface
            ) {
                LinearProgressIndicator(
                    progress = { batteryFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MeterRow(
                label = stringResource(R.string.memory_label),
                valueText = "${state.usedMemoryBytes.formatFileSize()} / ${state.maxMemoryBytes.formatFileSize()}",
                valueColor = colors.onSurface
            ) {
                LinearProgressIndicator(
                    progress = { memoryFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MeterRow(
    label: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    meter: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = valueColor
            )
        }

        meter()
    }
}

@Composable
private fun ThermalSegments(
    status: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(6) { index ->
            val segmentColor by animateFloatAsState(
                targetValue = if (index <= status) 1f else 0f,
                label = "thermal_segment_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (segmentColor > 0.5f) {
                            color
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun thermalLabel(status: Int): String {
    return when (status) {
        1 -> stringResource(R.string.thermal_1)
        2 -> stringResource(R.string.thermal_2)
        3 -> stringResource(R.string.thermal_3)
        4 -> stringResource(R.string.thermal_4)
        5 -> stringResource(R.string.thermal_5)
        else -> stringResource(R.string.thermal_0)
    }
}

@Composable
private fun PolicyCard(
    policy: PerformancePolicy?,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (policy == null) {
                Text(text = "…", style = MaterialTheme.typography.bodyMedium)
            } else {
                PolicyRow(
                    label = stringResource(R.string.policy_threads),
                    value = policy.threadCount.toString()
                )

                PolicyRow(
                    label = stringResource(R.string.policy_thumbnails),
                    value = "${policy.thumbnailCount} (${policy.thumbnailWidth}x${policy.thumbnailHeight})"
                )

                PolicyRow(
                    label = stringResource(R.string.policy_cache),
                    value = policy.imageCacheBytes.formatFileSize()
                )

                PolicyRow(
                    label = stringResource(R.string.policy_max_bitrate),
                    value = "${policy.maxVideoBitrate / 1_000_000} Mbps"
                )

                PolicyRow(
                    label = stringResource(R.string.policy_concurrent),
                    value = policy.maxConcurrentTasks.toString()
                )

                PolicyRow(
                    label = stringResource(R.string.policy_thermal_pause),
                    value = if (policy.thermalPauseEnabled) {
                        stringResource(R.string.enabled_label)
                    } else {
                        stringResource(R.string.disabled_label)
                    }
                )
            }
        }
    }
}

@Composable
private fun PolicyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CodecsCard(
    state: DiagnosticsUiState,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CodecRow(name = "H.264", supported = state.codecH264)
            CodecRow(name = "HEVC", supported = state.codecHevc)
            CodecRow(name = "AV1", supported = state.codecAv1)
        }
    }
}

@Composable
private fun CodecRow(
    name: String,
    supported: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = if (supported) {
                stringResource(R.string.codec_supported)
            } else {
                stringResource(R.string.codec_unsupported)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (supported) {
                Color(0xFF2E7D32)
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}