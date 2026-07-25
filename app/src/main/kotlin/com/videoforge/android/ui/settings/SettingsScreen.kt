package com.videoforge.android.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoforge.android.BuildConfig
import com.videoforge.android.R
import com.videoforge.core.designsystem.component.VfSectionHeader
import com.videoforge.core.designsystem.component.VfTopBar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val highContrast by viewModel.highContrast.collectAsStateWithLifecycle()
    val outputTreeUri by viewModel.outputTreeUri.collectAsStateWithLifecycle()

    val pickFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.setOutputFolder(selectedUri)
        }
    }

    val currentFolderLabel = remember(outputTreeUri) {
        outputTreeUri?.let {
            Uri.decode(it.substringAfterLast('/'))
        }
    }

    Scaffold(
        topBar = {
            VfTopBar(
                title = stringResource(R.string.settings_title),
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
            VfSectionHeader(title = stringResource(R.string.settings_output_section))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentFolderLabel != null) {
                            stringResource(R.string.output_folder_current, currentFolderLabel!!)
                        } else {
                            stringResource(R.string.output_folder_default)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.output_folder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { pickFolderLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.output_folder_change))
                    }
                }
            }

            VfSectionHeader(title = stringResource(R.string.settings_appearance_section))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.high_contrast_label),
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = stringResource(R.string.high_contrast_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = highContrast,
                        onCheckedChange = { enabled ->
                            viewModel.setHighContrast(enabled)
                        }
                    )
                }
            }

            VfSectionHeader(title = stringResource(R.string.settings_tools_section))

            OutlinedButton(
                onClick = onNavigateToPlugins,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.open_plugins))
            }

            OutlinedButton(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.open_diagnostics))
            }

            VfSectionHeader(title = stringResource(R.string.settings_about_section))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}