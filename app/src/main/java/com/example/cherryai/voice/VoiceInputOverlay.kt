package com.example.cherryai.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cherryai.ui.theme.CherryUiColors

@Composable
fun ChatGptVoiceBar(
    state: VoiceState,
    waveformBands: FloatArray,
    accentColor: Color,
    ui: CherryUiColors,
    onCancel: () -> Unit,
    onStopPaste: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.showVoiceBar()) return

    val busy = state is VoiceState.Transcribing
    val iconTint = if (busy) ui.iconMuted else ui.iconPrimary
    val controlBg = ui.composerControl
    val displayBands = if (busy) {
        FloatArray(VoiceConfig.waveformBarCount) { 0.12f }
    } else {
        waveformBands
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(controlBg)
                .clickable(enabled = !busy, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Cancel voice input",
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        VoiceWaveform(
            bands = displayBands,
            accentColor = accentColor,
            idleColor = ui.iconMuted.copy(alpha = 0.35f),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(controlBg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(controlBg)
                .clickable(enabled = !busy, onClick = onStopPaste),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Stop,
                contentDescription = "Stop and paste to input",
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (busy) accentColor.copy(alpha = 0.45f) else accentColor)
                .clickable(enabled = !busy, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ArrowUpward,
                contentDescription = "Stop and send",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun VoiceWaveform(
    bands: FloatArray,
    accentColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        val barCount = minOf(bands.size, VoiceConfig.waveformBarCount)
        repeat(barCount) { index ->
            val level = bands[index].coerceIn(0f, 1f)
            val heightFraction = (0.14f + level * 0.86f).coerceIn(0.12f, 1f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((22f * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (level > 0.1f) {
                            accentColor.copy(alpha = 0.35f + level * 0.65f)
                        } else {
                            idleColor
                        },
                    ),
            )
        }
    }
}
