package com.videoforge.android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.videoforge.core.designsystem.component.VfCard
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
    val colors = MaterialTheme.colorScheme

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val safeDuration = state.durationMs.coerceAtLeast(1L)

    val currentFraction = if (isDragging) {
        dragFraction
    } else {
        (state.positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    }

    val pointA = state.pointA
    val pointB = state.pointB
    val hasAB = pointA != null && pointB != null && pointB > pointA

    val aFrac = if (hasAB) (pointA!!.toFloat() / safeDuration).coerceIn(0f, 1f) else 0f
    val bFrac = if (hasAB) (pointB!!.toFloat() / safeDuration).coerceIn(0f, 1f) else 0f

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            VfTopBar(
                title = state.fileName.ifEmpty { stringResource(R.string.app_name) },
                onBack = onBack,
                backLabel = stringResource(R.string.back)
            )
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
                            listOf(colors.surfaceContainerLow, colors.background)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(colors.primary.copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(200f, -60f),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VfCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .vfSharedMediaBounds("media:${state.uri}")
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
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

                        Text(
                            text = state.durationMs.formatDuration(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = PlexMono,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    Color(0x88000000),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (state.thumbnails.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        state.thumbnails.forEach { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                VfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (hasAB) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                if (aFrac > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(aFrac)
                                            .fillMaxSize()
                                            .background(colors.surfaceVariant)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight((bFrac - aFrac).coerceAtLeast(0.001f))
                                        .fillMaxSize()
                                        .background(colors.primary)
                                )
                                if (bFrac < 1f) {
                                    Box(
                                        modifier = Modifier
                                            .weight((1f - bFrac).coerceAtLeast(0.001f))
                                            .fillMaxSize()
                                            .background(colors.surfaceVariant)
                                    )
                                }
                            }
                        }

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
                                fontFamily = PlexMono,
                                color = colors.onSurfaceVariant
                            )

                            Text(
                                text = state.durationMs.formatDuration(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = PlexMono,
                                color = colors.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (hasAB) {
                                "التكرار مفعّل: سيُعاد تشغيل الجزء من ${pointA!!.formatDuration()} إلى ${pointB!!.formatDuration()} تلقائيًا."
                            } else {
                                "التكرار متوقّف. حدّد نقطتين أدناه لتكرار جزء معيّن."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasAB) colors.primary else colors.onSurfaceVariant
                        )
                    }
                }

                VfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "التحكم بالتشغيل",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundControl(label = "◀", caption = "إطار سابق") {
                                viewModel.stepFrame(-1)
                            }

                            Button(
                                onClick = { viewModel.togglePlayPause() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }

                            RoundControl(label = "▶", caption = "إطار تالٍ") {
                                viewModel.stepFrame(1)
                            }
                        }

                        Text(
                            text = "سرعة التشغيل",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )

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
                    }
                }

                VfCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "تكرار مقطع بين نقطتين (A → B)",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.primary
                            )
                            Text(
                                text = "اضغط «نقطة A» عند بداية الجزء الذي تريد تكراره، ثم حرّك المؤشر واضغط «نقطة B» عند نهايته — سيُعاد تشغيل هذا الجزء وحده تلقائيًا. «إلغاء» يوقف التكرار.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PointButton(
                                label = "نقطة A",
                                active = pointA != null,
                                sub = pointA?.formatDuration(),
                                onClick = { viewModel.setPointA() },
                                modifier = Modifier.weight(1f)
                            )

                            PointButton(
                                label = "نقطة B",
                                active = pointB != null,
                                sub = pointB?.formatDuration(),
                                onClick = { viewModel.setPointB() },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = { viewModel.clearABRepeat() },
                            enabled = pointA != null || pointB != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceVariant,
                                contentColor = colors.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "إلغاء التكرار (A-B)")
                        }
                    }
                }

                Button(
                    onClick = { onOpenEditor(state.uri) },
                    enabled = state.uri.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.open_editor),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RoundControl(
    label: String,
    caption: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.size(52.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text(text = label, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PointButton(
    label: String,
    active: Boolean,
    sub: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(200),
        label = "point_bg"
    )
    val content by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "point_fg"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = FontWeight.Bold)
            Text(
                text = sub ?: "—",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = PlexMono
            )
        }
    }
}
