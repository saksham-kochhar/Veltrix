package com.example.veltrix.chathistorry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HistoryPurple = Color(0xFF635BFF)
private val HistoryNavy = Color(0xFF1A1C2E)
private val HistoryMuted = Color(0xFF9E9E9E)
private val HistorySearchBg = Color(0xFFF3F4F9)
private val HistorySelectedBg = Color(0xFFEBEBFF)
private val HistoryNewChatBg = Color(0xFFF0EEFF)
private val HistoryIconGrayBg = Color(0xFFF0F0F5)
private val HistoryIconGray = Color(0xFFB0B0BC)

@Composable
fun ChatHistoryDrawerContent(
    sessions: List<ChatSessionSummary>,
    currentSessionId: String,
    statusMessage: String?,
    onNewChat: () -> Unit,
    onOpenSession: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else {
            val q = searchQuery.trim().lowercase()
            sessions.filter {
                it.title.lowercase().contains(q) ||
                    it.summary.lowercase().contains(q)
            }
        }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight()
            .padding(end = 10.dp, top = 10.dp, bottom = 10.dp)
            .shadow(12.dp, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)),
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = HistoryNavy
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HistoryNewChatBg)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close history",
                        tint = HistoryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50)),
                placeholder = {
                    Text(
                        text = "Search chats...",
                        color = HistoryMuted,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = HistoryMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = HistorySearchBg,
                    unfocusedContainerColor = HistorySearchBg,
                    disabledContainerColor = HistorySearchBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = HistoryPurple
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(HistoryNewChatBg)
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HistoryPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "New chat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HistoryNavy
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F5))

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredSessions.isEmpty()) {
                Text(
                    text = if (searchQuery.isBlank()) "No chats yet" else "No matching chats",
                    fontSize = 14.sp,
                    color = HistoryMuted,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        ChatHistoryRow(
                            session = session,
                            selected = session.id == currentSessionId,
                            onClick = { onOpenSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryRow(
    session: ChatSessionSummary,
    selected: Boolean,
    onClick: () -> Unit
) {
    val timeLabel = formatChatTime(session.updatedAt)
    val rowBg = if (selected) HistorySelectedBg else Color.Transparent
    val iconBg = if (selected) HistoryPurple else HistoryIconGrayBg
    val iconTint = if (selected) Color.White else HistoryIconGray
    val titleColor = if (selected) HistoryPurple else HistoryNavy

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.title.ifBlank { "New chat" },
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!session.cachedLocally) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = "Cloud only",
                        tint = HistoryMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (timeLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeLabel,
                    fontSize = 12.sp,
                    color = HistoryMuted
                )
            }
        }

        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "Chat options",
            tint = HistoryMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatChatTime(updatedAt: Long): String {
    if (updatedAt <= 0L) return ""
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(updatedAt))
}
