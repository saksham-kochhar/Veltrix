package com.example.cherryai.Screen

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.cherryai.cherryAiViewModel

@Composable
fun OfflineDataScreen(navController: NavHostController, viewModel: cherryAiViewModel) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<cherryAiViewModel.OfflineModelFile?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshOfflineModelFiles(context) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 32.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Offline Data", style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Text("Downloaded models", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Models are stored on this device and can be removed at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
            }
            if (viewModel.offlineModelFiles.isEmpty()) {
                item {
                    Text("No offline models downloaded.", color = Color.Gray, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(viewModel.offlineModelFiles, key = { it.path }) { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.name, style = MaterialTheme.typography.bodyLarge)
                            Text(Formatter.formatFileSize(context, model.sizeBytes), color = Color.Gray)
                        }
                        IconButton(onClick = { pendingDelete = model }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${model.name}", tint = Color(0xFFD32F2F))
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete model?") },
            text = { Text("Delete ${model.name} from this device? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteOfflineModel(context, model)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}
