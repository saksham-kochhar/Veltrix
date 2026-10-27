package com.example.cherryai.Screen

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.cherryai.cherryAiViewModel

@Composable
fun OfflineDataScreen(navController: NavHostController, viewModel: cherryAiViewModel) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<cherryAiViewModel.OfflineModelFile?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshOfflineModelFiles(context) }

    val purple = Color(0xFF5B4BFF)
    val pageBackground = Color(0xFFF8F8FC)

    Column(modifier = Modifier.fillMaxSize().background(pageBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Offline data", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Your downloaded models", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Use AI without an internet connection. Models are stored securely on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF747482)
                )
            }
            if (viewModel.offlineModelFiles.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = purple, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No models downloaded yet", style = MaterialTheme.typography.titleMedium)
                            Text("Download a model from the home screen to use Cherry AI offline.", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(viewModel.offlineModelFiles, key = { it.path }) { model ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(46.dp).background(Color(0xFFEDEBFF), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Storage, null, tint = purple)
                            }
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(model.name, style = MaterialTheme.typography.titleSmall)
                                Text("On this device  •  ${Formatter.formatFileSize(context, model.sizeBytes)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { pendingDelete = model }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${model.name}", tint = Color(0xFFE05252))
                        }
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
