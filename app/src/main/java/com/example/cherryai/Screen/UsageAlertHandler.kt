package com.example.cherryai.Screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.cherryai.cherryAiViewModel

@Composable
fun UsageAlertHandler(viewModel: cherryAiViewModel) {
    val alert = viewModel.pendingUsageAlert
    val context = LocalContext.current
    if (alert == null) return
    val title = when (alert.scope) {
        "daily" -> "Daily usage"
        else -> "Monthly usage"
    }
    AlertDialog(
        onDismissRequest = { viewModel.dismissUsageAlert(context) },
        title = { Text(title) },
        text = { Text(alert.message) },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissUsageAlert(context) }) {
                Text("OK")
            }
        },
    )
}
