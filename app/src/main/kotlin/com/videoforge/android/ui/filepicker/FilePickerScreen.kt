package com.videoforge.android.ui.filepicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.videoforge.android.R
import com.videoforge.android.util.formatDuration
import com.videoforge.android.util.formatFileSize
import com.videoforge.android.util.formatResolution
import com.videoforge.core.designsystem.component.VfEmptyState
import com.videoforge.core.designsystem.component.VfTopBar
import com.videoforge.core.designsystem.theme.PlexMono

@Composable
fun FilePickerScreen(
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    viewModel: FilePickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            onOpenVideo(selectedUri.toString())
        }
    }

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.file_picker_title),
                onBack = onBack,
                backLabel = stringResource(R.string.back)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { systemPickerLauncher.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.open_system_picker))
            }

            if (state.assets.isEmpty()) {
                VfEmptyState(
                    message = stringResource(R.string.empty_videos),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.assets,
                        key = { it.uri }
                    ) { asset ->
                        VideoPickCard(
                            asset = asset,
                            onClick = { onOpenVideo(asset.uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPickCard(
    asset: FilePickerAssetUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AsyncImage(
                model = asset.uri,
                contentDescription = asset.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = asset.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val details = listOfNotNull(
                formatResolution(asset.width, asset.height),
                if (asset.durationMs > 0) asset.durationMs.formatDuration() else null,
                asset.sizeBytes.formatFileSize()
            ).joinToString(" • ")

            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = PlexMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}