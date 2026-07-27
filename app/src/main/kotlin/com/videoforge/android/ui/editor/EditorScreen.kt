package com.videoforge.android.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.videoforge.android.R
import com.videoforge.android.export.ExportService
import com.videoforge.android.ui.editor.timeline.TimelineClipVisual
import com.videoforge.android.ui.editor.timeline.TimelineView
import com.videoforge.android.ui.shared.vfSharedMediaBounds
import com.videoforge.android.util.formatDuration
import com.videoforge.android.util.formatTimecode
import com.videoforge.core.designsystem.component.VfEmptyState
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar
import com.videoforge.core.designsystem.icons.VfIcons

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val hasSelection = state.selectedClipId != null

    val selectedClip = state.clips.firstOrNull { it.id == state.selectedClipId }
    val pointerInSelected = selectedClip != null &&
        state.currentClipIndex in state.clips.indices &&
        state.clips[state.currentClipIndex].id == selectedClip.id

    val hint = remember(selectedClip, pointerInSelected, state.currentClipPositionMs, hasSelection) {
        when {
            !hasSelection || selectedClip == null ->
                "اضغط على مقطع في الخط الزمني لتحديده، ثم حرّك المؤشر (زر تشغيل أو إطار) إلى الموضع الذي تريد القص عنده."

            !pointerInSelected ->
                "المقطع المحدّد لا يمرّ به المؤشر حاليًا. شغّل الفيديو أو اسحب المؤشر داخل المقطع المحدّد لتفعيل أزرار القص."

            else -> {
                val pos = state.currentClipPositionMs.formatDuration()
                val dur = selectedClip.durationMs.formatDuration()
                "المقطع المحدّد مدته $dur والمؤشر عند $pos منه.\n" +
                    "• قص البداية: يحذف من بداية المقطع حتى $pos.\n" +
                    "• قص النهاية: يحذف من $pos حتى نهاية المقطع.\n" +
                    "• تقسيم: يشطر المقطع عند $pos إلى مقطعين.\n" +
                    "• حذف: يزيل المقطع المحدّد كاملًا."
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importSubtitle(it) }
    }

    var zoomScale by remember { mutableFloatStateOf(80f) }
    var isScrubbingHud by remember { mutableStateOf(false) }

    val scrubSurface = remember { mutableStateOf<Surface?>(null) }

    val scrubSurfaceCallback = remember {
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                scrubSurface.value = holder.surface
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                scrubSurface.value = null
            }
        }
    }

    LaunchedEffect(scrubSurface.value) {
        val surface = scrubSurface.value

        if (surface != null) {
            viewModel.attachScrubSurface(surface)
        } else {
            viewModel.releaseScrubDecoder()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        uri?.let { outputUri ->
            state.timelineId?.let { timelineId ->
                ExportService.start(
                    context = context,
                    timelineId = timelineId,
                    outputUri = outputUri.toString()
                )

                Toast.makeText(
                    context,
                    context.getString(R.string.export_started),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            createDocumentLauncher.launch("videoforge_export.mp4")
        }
    }

    val onExportClick: () -> Unit = {
        if (state.timelineId != null) {
            val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (notificationPermissionGranted) {
                createDocumentLauncher.launch("videoforge_export.mp4")
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.editor_title),
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
                    .vfSharedMediaBounds("media:${state.assetUri}")
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = { viewModel.stepFrame(-1) }) {
                    Text(text = "◀ إطار")
                }

                Button(onClick = { viewModel.togglePlayPause() }) {
                    Text(text = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل")
                }

                TextButton(onClick = { viewModel.stepFrame(1) }) {
                    Text(text = "إطار ▶")
                }

                Text(
                    text = "${state.timelinePositionMs.formatDuration()} / ${state.timelineDurationMs.formatDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.subtitleTrackName != null) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                            )

                            Text(
                                text = "الترجمة المنفصلة",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (state.subtitleTrackName != null) {
                            Text(
                                text = "محمّلة: ${state.subtitleTrackName} • ${state.subtitleCueCount} سطر. ستُصدَّر ملفًّا منفصلًا مزامَنًا، ويُحذف منها ما حذفتَه من الفيديو.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text(
                                text = "لا ترجمة محمّلة. استورد ملف SRT أو VTT — سيُصدَّر منفصلًا ومزامَنًا مع قصّك، دون دمجه في الفيديو.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { subtitlePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (state.subtitleTrackName != null) "استبدال الترجمة" else "استيراد ترجمة"
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )

                            Text(
                                text = "حذف نطاق بنقرتين",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Text(
                            text = "حرّك المؤشر إلى بداية الجزء المراد حذفه واضغط «بداية الحذف»، ثم إلى نهايته واضغط «نهاية الحذف»، ثم «احذف النطاق». يبقى ما حول النطاق، وتُحذف الترجمة المطابقة معه.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.markDeleteStart() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "بداية الحذف")
                                    Text(
                                        text = "عند المؤشر",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.markDeleteEnd() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "نهاية الحذف")
                                    Text(
                                        text = "عند المؤشر",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        val total = state.timelineDurationMs.coerceAtLeast(1L)
                        val ds = state.pendingDeleteStartMs
                        val de = state.pendingDeleteEndMs
                        val hasRange = ds != null && de != null && kotlin.math.abs(de - ds) >= 50L
                        val rStart = if (hasRange) minOf(ds!!, de!!) else 0L
                        val rEnd = if (hasRange) maxOf(ds!!, de!!) else 0L
                        val beforeFrac = (rStart.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        val rangeFrac = if (hasRange) {
                            ((rEnd - rStart).toFloat() / total.toFloat()).coerceIn(0f, 1f - beforeFrac)
                        } else {
                            0f
                        }
                        val afterFrac = (1f - beforeFrac - rangeFrac).coerceAtLeast(0f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            if (beforeFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(beforeFrac)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                                )
                            }
                            if (rangeFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(rangeFrac)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                            if (afterFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(afterFrac)
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                                )
                            }
                        }

                        Text(
                            text = if (hasRange) {
                                "سيُحذف من ${rStart.formatDuration()} إلى ${rEnd.formatDuration()} — مدته ${(rEnd - rStart).formatDuration()}"
                            } else {
                                "لم تُحدّد نطاقًا بعد. اللون الأحمر يمثّل الجزء الذي سيُحذف."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasRange) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.commitDeleteRange() },
                                enabled = hasRange,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "احذف النطاق المحدّد")
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearDeleteSelection() },
                                enabled = ds != null || de != null
                            ) {
                                Text(text = "مسح")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pointerInSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                            )

                            Text(
                                text = if (hasSelection) "أداة القص" else "ابدأ بتحديد مقطع",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Text(
                            text = "لحذف جزء من الوسط يدويًا (يبقى ما حوله):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "١) حرّك المؤشر لبداية الجزء ← «تقسيم».\n" +
                                "٢) حرّك المؤشر لنهاية الجزء ← «تقسيم» (يصير ٣ مقاطع).\n" +
                                "٣) اضغط المقطع الأوسط في الخط الزمني لتحديده.\n" +
                                "٤) «حذف المقطع» ← يبقى الأول والأخير فقط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                val losslessAvailable = state.losslessAvailable

                val losslessTransition = rememberInfiniteTransition(label = "lossless_pulse")

                val losslessPulse by losslessTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lossless_pulse_alpha"
                )

                AnimatedVisibility(
                    visible = losslessAvailable != null,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    val losslessColor = if (losslessAvailable == true) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFEF6C00)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(losslessColor.copy(alpha = losslessPulse))
                        )

                        Text(
                            text = if (losslessAvailable == true) {
                                stringResource(R.string.lossless_available)
                            } else {
                                stringResource(R.string.lossless_unavailable)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = losslessColor
                        )
                    }
                }

                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.timelineId != null
                ) {
                    Text(text = stringResource(R.string.action_export))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.weight(1f),
                        enabled = state.canUndo
                    ) {
                        Text(text = stringResource(R.string.undo))
                    }

                    OutlinedButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.weight(1f),
                        enabled = state.canRedo
                    ) {
                        Text(text = stringResource(R.string.redo))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.splitSelectedClip() },
                            enabled = hasSelection && pointerInSelected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.action_split))
                                Text(
                                    text = "يشطر عند المؤشر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.trimStartSelectedClip() },
                            enabled = hasSelection && pointerInSelected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.action_trim_start))
                                Text(
                                    text = "يحذف ما قبل المؤشر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.trimEndSelectedClip() },
                            enabled = hasSelection && pointerInSelected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.action_trim_end))
                                Text(
                                    text = "يحذف ما بعد المؤشر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.deleteSelectedClip() },
                            enabled = hasSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = stringResource(R.string.action_delete_clip))
                                Text(
                                    text = "يزيل المقطع كاملًا",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VfSectionHeader(
                        title = stringResource(R.string.timeline_section),
                        icon = VfIcons.Waveform
                    )
                }

                Text(
                    text = stringResource(R.string.timeline_px_per_sec, zoomScale.toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoxWithConstraints {
                    val fitScale = with(LocalDensity.current) {
                        val widthPx = constraints.maxWidth.toFloat()
                        val durationSec = (state.timelineDurationMs / 1000f).coerceAtLeast(0.1f)
                        (widthPx / durationSec).coerceIn(4f, 900f)
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { zoomScale = (zoomScale / 1.6f).coerceIn(4f, 900f) }
                            ) {
                                Text(text = "−")
                            }

                            OutlinedButton(
                                onClick = { zoomScale = (zoomScale * 1.6f).coerceIn(4f, 900f) }
                            ) {
                                Text(text = "+")
                            }

                            OutlinedButton(
                                onClick = { zoomScale = fitScale }
                            ) {
                                Text(text = stringResource(R.string.timeline_zoom_fit))
                            }
                        }

                        AnimatedVisibility(
                            visible = isScrubbingHud,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = state.timelinePositionMs.formatTimecode(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isScrubbingHud,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box {
                                    AndroidView(
                                        factory = { viewContext ->
                                            SurfaceView(viewContext).apply {
                                                holder.addCallback(scrubSurfaceCallback)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )

                                    Surface(
                                        color = Color(0xAA000000),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = state.timelinePositionMs.formatTimecode(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.scrub_precise_frame),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val visualClips = remember(state.clips, state.markers, state.selectedClipId) {
                            var clipStart = 0L

                            state.clips.map { clip ->
                                val visual = TimelineClipVisual(
                                    id = clip.id,
                                    timelineStartMs = clipStart,
                                    durationMs = clip.durationMs,
                                    sourceInMs = clip.sourceInMs,
                                    sourceOutMs = clip.sourceOutMs,
                                    selected = clip.id == state.selectedClipId,
                                    markerOffsets = state.markers
                                        .filter { it.clipId == clip.id }
                                        .map { it.offsetMs }
                                )

                                clipStart += clip.durationMs

                                visual
                            }
                        }

                        TimelineView(
                            durationMs = state.timelineDurationMs,
                            clips = visualClips,
                            frames = state.timelineFrames,
                            peaks = state.waveformPeaks,
                            positionMs = state.timelinePositionMs,
                            isPlaying = state.isPlaying,
                            scale = zoomScale,
                            onSeek = { viewModel.seekTimeline(it) },
                            onScrubbing = { scrubbing ->
                                isScrubbingHud = scrubbing
                                viewModel.setScrubbing(scrubbing)
                            },
                            onSelectClip = { viewModel.selectClip(it) },
                            onScaleChange = { newScale -> zoomScale = newScale },
                            deleteRangeStartMs = state.pendingDeleteStartMs,
                            deleteRangeEndMs = state.pendingDeleteEndMs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                VfSectionHeader(
                    title = stringResource(R.string.markers_section),
                    icon = VfIcons.Clock
                )

                if (state.markers.isEmpty()) {
                    VfEmptyState(
                        message = stringResource(R.string.no_markers),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    state.markers.forEach { marker ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = marker.label,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            TextButton(onClick = { viewModel.deleteMarker(marker.id) }) {
                                Text(text = stringResource(R.string.delete_marker))
                            }
                        }
                    }
                }
            }
        }
    }
}
