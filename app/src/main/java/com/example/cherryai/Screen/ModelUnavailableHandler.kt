package com.example.cherryai.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cherryai.cherryAiViewModel

private val ModelDialogTitle = Color(0xFF111133)
private val ModelDialogBody = Color(0xFF6B6B80)
private val ModelDialogPrimary = Color(0xFF5B4DFF)
private val ModelDialogIconBg = Color(0xFFF3F1FF)

@Composable
fun ModelUnavailableHandler(viewModel: cherryAiViewModel) {
    val message = viewModel.pendingChatDialogMessage ?: return
    val isModelIssue = message.contains("unavailable", ignoreCase = true) ||
        message.contains("Choose another model", ignoreCase = true)
    val dialogTitle = viewModel.pendingChatDialogTitle
        ?: if (isModelIssue) "Model unavailable" else "Couldn't send message"

    Dialog(
        onDismissRequest = { viewModel.clearChatDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ModelDialogIconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = ModelDialogPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ModelDialogTitle,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = ModelDialogBody,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isModelIssue) {
                            viewModel.requestOpenModelPicker()
                        } else {
                            viewModel.clearChatDialog()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModelDialogPrimary),
                ) {
                    Text(
                        text = if (isModelIssue) "Choose another model" else "OK",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }

                TextButton(
                    onClick = { viewModel.clearChatDialog() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Cancel",
                        color = ModelDialogBody,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
