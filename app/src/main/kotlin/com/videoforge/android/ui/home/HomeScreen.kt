package com.videoforge.android.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.videoforge.android.R
import com.videoforge.android.ui.shared.vfSharedMedia
import com.videoforge.android.util.formatDuration
import com.videoforge.android.util.formatFileSize
import com.videoforge.android.util.formatResolution
import com.videoforge.android.util.formatTotalDuration
import com.videoforge.core.designsystem.component.VfActionCard
import com.videoforge.core.designsystem.component.VfCard
import com.videoforge.core.designsystem.component.VfEmptyState
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar
import com.videoforge.core.designsystem.icons.VfIcons
import com.videoforge.core.designsystem.motion.VfReveal
import com.videoforge.core.designsystem.theme.PlexMono

private val READABLE_MAX_WIDTH = 840.dp

@Composable
fun HomeScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToFilePicker: () -> Unit,
    onNavigateToCompress: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenVideo: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    val listState = rememberLazyListState()

    val rawOffset = listState.firstVisibleItemScrollOffset.toFloat() +
        (if (listState.firstVisibleItemIndex > 0) 400f else 0f)

    val collapse by animateFloatAsState(
        targetValue = (rawOffset / 260f).coerceIn(0f, 1f),
        animationSpec = tween(200),
        label = "band_collapse"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            VfTopBar(title = stringResource(R.string.app_name))
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.surfaceContainerLow,
                                colors.background
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            center = Offset(180f, -80f),
                            radius = 950f
                        )
                    )
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        LibraryBand(
                            videosCount = state.videosCount,
                            totalDurationMs = state.totalDurationMs,
                            totalSizeBytes = state.totalSizeBytes,
                            collapse = collapse,
                            modifier = Modifier
                                .widthIn(max = READABLE_MAX_WIDTH)
                                .fillMaxWidth()
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = READABLE_MAX_WIDTH)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VfSectionHeader(
                                title = stringResource(R.string.section_quick_actions),
                                icon = VfIcons.Bolt
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VfReveal(0) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_new_project),
                                        icon = VfIcons.Layers,
                                        onClick = onNavigateToProjects,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(1) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_open_video),
                                        icon = VfIcons.Film,
                                        onClick = onNavigateToFilePicker,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(2) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_cut_video),
                                        icon = VfIcons.Scissors,
                                        onClick = onNavigateToFilePicker,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(3) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_compress_video),
                                        icon = VfIcons.Compress,
                                        onClick = onNavigateToCompress,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(4) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_batch),
                                        icon = VfIcons.Waveform,
                                        onClick = onNavigateToBatch,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(5) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_settings),
                                        icon = VfIcons.Subtitle,
                                        onClick = onNavigateToSettings,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }

                                VfReveal(6) {
                                    VfActionCard(
                                        title = stringResource(R.string.action_logs),
                                        icon = VfIcons.Clock,
                                        onClick = onNavigateToLogs,
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(104.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        VfSectionHeader(
                            title = stringResource(R.string.section_recent_files),
                            icon = VfIcons.Film,
                            modifier = Modifier
                                .widthIn(max = READABLE_MAX_WIDTH)
                                .fillMaxWidth()
                        )
                    }
                }

                if (state.recentFiles.isEmpty()) {
                    item {
                        VfEmptyState(
                            message = stringResource(R.string.empty_recent_files),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp)
                        )
                    }
                } else {
                    items(
                        items = state.recentFiles,
                        key = { it.uri }
                    ) { item ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            VfReveal(0) {
                                RecentMediaItem(
                                    item = item,
                                    onClick = { onOpenVideo(item.uri) },
                                    modifier = Modifier
                                        .widthIn(max = READABLE_MAX_WIDTH)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBand(
    videosCount: Int,
    totalDurationMs: Long,
    totalSizeBytes: Long,
    collapse: Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    VfCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-collapse * 34f).dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(14) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primary.copy(alpha = 0.28f))
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_library_label),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1f - collapse * 0.35f
                        scaleY = 1f - collapse * 0.22f
                        scaleX = 1f - collapse * 0.22f
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    Text(
                        text = videosCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primary
                    )

                    Text(
                        text = stringResource(R.string.home_videos_count_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(colors.outlineVariant)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BandStat(
                        label = stringResource(R.string.home_total_duration_label),
                        value = totalDurationMs.formatTotalDuration()
                    )

                    BandStat(
                        label = stringResource(R.string.home_total_size_label),
                        value = totalSizeBytes.formatFileSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BandStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = PlexMono
        )
    }
}

@Composable
private fun RecentMediaItem(
    item: HomeMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VfCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .vfSharedMedia("media:${item.uri}"),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val details = remember(item.uri, item.width, item.height, item.durationMs, item.sizeBytes) {
                    listOfNotNull(
                        formatResolution(item.width, item.height),
                        if (item.durationMs > 0) item.durationMs.formatDuration() else null,
                        item.sizeBytes.formatFileSize()
                    ).joinToString(" • ")
                }

                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = PlexMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}