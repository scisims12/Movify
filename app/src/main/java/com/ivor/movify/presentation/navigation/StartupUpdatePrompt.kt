package com.ivor.movify.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivor.movify.data.update.UpdateUiState
import com.ivor.movify.ui.theme.ExpressiveShapes

@Composable
fun StartupUpdatePrompt(
    viewModel: AppViewModel,
    onOpenUpdates: () -> Unit
) {
    val state by viewModel.updateState.collectAsState()

    if (state is UpdateUiState.Available) {
        val available = state as UpdateUiState.Available
        val release = available.release
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate(release.tagName) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpdate(release.tagName)
                        onOpenUpdates()
                    },
                    shape = ExpressiveShapes.large
                ) {
                    Text("Update now")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate(release.tagName) }) {
                    Text("Later")
                }
            },
            icon = {
                Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Update available", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Movify ${release.tagName.removePrefix("v")} is available.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    val notes = release.name ?: release.body?.take(100)?.let { it + "..." } ?: "Bug fixes and performance improvements."
                    Text(
                        text = "What's new:\n$notes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            shape = ExpressiveShapes.extraLarge
        )
    }
}
