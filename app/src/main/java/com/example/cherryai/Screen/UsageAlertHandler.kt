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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cherryai.UsageAlertPrompt
import com.example.cherryai.WalletSnapshot
import com.example.cherryai.cherryAiViewModel

private val UsageDialogTitle = Color(0xFF111133)
private val UsageDialogPrimary = Color(0xFF5B4DFF)
private val UsageDialogIconBg = Color(0xFFF3F1FF)

private fun usageLimitHitMessage(alert: UsageAlertPrompt, wallet: WalletSnapshot): String {
    val percent = when (alert.scope) {
        "daily" -> wallet.dailyUsedPercent
        else -> wallet.monthlyUsedPercent
    }
    val displayPercent = when {
        percent > 0 -> percent
        alert.level == 1 -> 50
        alert.level == 2 -> 80
        alert.level == 3 -> 100
        else -> alert.level.coerceAtLeast(1) * 50
    }.coerceIn(1, 100)
    return "$displayPercent% usage limit hit."
}

@Composable
fun UsageAlertHandler(viewModel: cherryAiViewModel) {
    val alert = viewModel.pendingUsageAlert ?: return
    val context = LocalContext.current
    val message = usageLimitHitMessage(alert, viewModel.walletSnapshot)

    Dialog(
        onDismissRequest = { viewModel.dismissUsageAlert(context) },
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
                        .background(UsageDialogIconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = UsageDialogPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UsageDialogTitle,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.dismissUsageAlert(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UsageDialogPrimary),
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
