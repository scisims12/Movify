package com.ivor.movify.presentation.update

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ivor.movify.R
import com.ivor.movify.data.remote.GithubReleaseDto
import com.ivor.movify.data.update.UpdateUiState
import com.ivor.movify.ui.theme.ExpressiveShapes
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailablePopup(
    onDismiss: (String) -> Unit,
    onViewDetails: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val release = when (val s = state) {
        is UpdateUiState.Available -> s.release
        is UpdateUiState.Downloading -> s.release
        is UpdateUiState.ReadyToInstall -> s.release
        is UpdateUiState.Failed -> s.release
        else -> null
    }

    if (release != null) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss(release.tagName) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = ExpressiveShapes.extraLarge,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Common Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "New update available",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Movify ${release.tagName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                when (val currentState = state) {
                    is UpdateUiState.Available -> {
                        Text(
                            text = "A new version of Movify is ready.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        val notes = release.body?.trim().orEmpty()
                        if (notes.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = ExpressiveShapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .heightIn(max = 160.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "What's new",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = notes,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Current: ${viewModel.currentVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "New: ${release.tagName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onDismiss(release.tagName) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Later")
                            }
                            if (notes.isNotEmpty()) {
                                TextButton(
                                    onClick = { 
                                        onDismiss(release.tagName)
                                        onViewDetails() 
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("View details")
                                }
                            }
                            Button(
                                onClick = {
                                    if (currentState.asset != null) {
                                        viewModel.download(release, currentState.asset)
                                    }
                                }
                            ) {
                                Text("Update now")
                            }
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        Text(
                            text = "Downloading Movify ${release.tagName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { currentState.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${(currentState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (currentState.totalBytes > 0) "${formatSize(currentState.downloadedBytes)} / ${formatSize(currentState.totalBytes)}" else "Starting download...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { viewModel.cancelDownload() }) {
                                Text("Cancel")
                            }
                        }
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        Text(
                            text = "Update ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Movify ${release.tagName} has been downloaded.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onDismiss(release.tagName) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Later")
                            }
                            Button(
                                onClick = {
                                    if (viewModel.canInstall()) {
                                        viewModel.install(currentState.apk)
                                    } else {
                                        context.startActivity(viewModel.installPermissionIntent())
                                    }
                                }
                            ) {
                                Text(if (viewModel.canInstall()) "Install update" else "Allow installs")
                            }
                        }
                    }

                    is UpdateUiState.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                            Text(
                                text = "Update couldn't be downloaded",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onDismiss(release.tagName) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Later")
                            }
                            Button(onClick = { viewModel.checkForUpdate() }) {
                                Text("Try again")
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1_048_576) String.format(Locale.US, "%.1f MB", bytes / 1_048_576f) else "${bytes / 1024} KB"
