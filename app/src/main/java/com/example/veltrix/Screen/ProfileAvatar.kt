package com.example.veltrix.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val avatarGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
    listOf(Color(0xFFDA4453), Color(0xFF89216B)),
    listOf(Color(0xFF56AB2F), Color(0xFFA8E063)),
    listOf(Color(0xFF614385), Color(0xFF516395)),
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))
)

fun profileInitials(firstName: String?, lastName: String?): String {
    val first = firstName?.trim()?.firstOrNull()?.uppercaseChar()
    val last = lastName?.trim()?.firstOrNull()?.uppercaseChar()
    return buildString {
        if (first != null) append(first)
        if (last != null) append(last)
    }.ifEmpty { "?" }
}

fun profileGradient(seed: String): Brush {
    val index = seed.hashCode().and(Int.MAX_VALUE) % avatarGradients.size
    val colors = avatarGradients[index]
    return Brush.linearGradient(colors)
}

@Composable
fun ProfileAvatar(
    firstName: String?,
    lastName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    fontSize: TextUnit = 32.sp
) {
    val initials = remember(firstName, lastName) { profileInitials(firstName, lastName) }
    val seed = remember(firstName, lastName) {
        "${firstName.orEmpty()}${lastName.orEmpty()}".ifBlank { "veltrix" }
    }
    val gradient = remember(seed) { profileGradient(seed) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}
