package com.example.veltrix.Screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.offline.Download
import com.example.veltrix.Instruction
import com.example.veltrix.Response
import com.example.veltrix.veltrixviewmodel
import com.google.api.Context

@Composable
fun ChatbotScreen(viewmodel : veltrixviewmodel) {

    val activeColor by animateColorAsState(
        if (viewmodel.OnlineMode) Color(0xFF5B4DFF)
        else Color(0xFF00C853),
        label = ""
    )
     var question by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf<String?>(null) }
    val modeColor = when (selectedMode) {
        "Brainstorm" -> Color(0xFF7C4DFF)
        "Learn" -> Color(0xFFFF9800)
        "Code" -> Color(0xFF009688)
        else -> Color(0xFFF5F5FA)
    }
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = modeColor.copy(alpha = 0.06f)
    ) {
        LaunchedEffect(Unit) {
            viewmodel.refreshModelStatus(context)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LaunchedEffect(Unit) {
                viewmodel.refreshModelStatus(context)

                if (viewmodel.isModelDownloaded) {
                    viewmodel.loadLocalModel(context)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(onClick = {
                    //meNU CONNECT
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = null,
                        tint = Color(0xFF1C1C3A)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = "Veltrix",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111133)
                        )

                        Spacer(modifier = Modifier.width(6.dp))


                    Text(
                        text = when(selectedMode) {
                            "Brainstorm" -> "Creative AI mode enabled"
                            "Learn" -> "Learning assistant active"
                            "Code" -> "Coding assistant active"
                            else -> "Connected or Not"
                        },
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            //ACCOUNT SECTION
                        },

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFF1C1C3A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(
                        1.dp,
                        Color(0xFFEAEAF5),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ModeButton(
                    title = "Online",
                    selected = viewmodel.OnlineMode,
                    icon = Icons.Outlined.Language,
                    activeColor = Color(0xFF5B4DFF)
                ) {
                    viewmodel.OnlineMode = true
                }

                ModeButton(
                    title = "Offline",
                    selected = !viewmodel.OnlineMode,
                    icon = Icons.Outlined.CloudOff,
                    activeColor = Color(0xFF00C853)
                ) {
                    viewmodel.OnlineMode = false
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically ,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (viewmodel.OnlineMode) Color(0xFF5B4DFF)
                            else Color(0xFF00C853)
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (viewmodel.OnlineMode)
                        "Connected • Responses may use internet"
                    else
                        "Offline mode enabled • Responses may take some time",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp)
            ) {
                MessageList(
                    messageList = viewmodel.messagelist,
                    isLoading = viewmodel.loading
                )
            }



            if (!viewmodel.OnlineMode) {
                if (!viewmodel.isModelDownloaded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFE9FFF0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(42.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                Column {
                                    Text(
                                        "Download Offline Model",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = Color(0xFFE8FFF1),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "Recommended",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = Color(0xFF00A651)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        "Get the model to use Veltrix offline.\nThis model only needs to be downloaded once.",
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                            Spacer(modifier = Modifier.height(20.dp))

                            if (viewmodel.isDownloading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Downloading model...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "${(viewmodel.downloadProgress * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00C853),
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { viewmodel.downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF00C853),
                                    trackColor = Color(0xFFE0E0E0)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "${(viewmodel.downloadProgress * 1500).toInt()} MB / 1500 MB",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )

                            } else {
                                // Download button UI
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "1.5 GB",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            )
                                        }
                                        Text("Estimated size", color = Color.Gray)
                                    }

                                    Button(
                                        onClick = { viewmodel.downloadModel(context) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00C853)
                                        )
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download (1.5GB)")
                                    }
                                }
                            }
                        }
                    }
                }
            }



            if (question == "" && viewmodel.messagelist.isEmpty() && viewmodel.OnlineMode)
            {
                Text(
                    text = "Suggestions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBBBBCC),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp))
                 {

                    item {

                        SuggestionCard(
                            icon = Icons.Outlined.Lightbulb,
                            title = "Brainstorm",
                            subtitle = "Generate fresh ideas",
                            color = Color(0xFF5B4DFF),
                            selected = selectedMode == "Brainstorm"
                        ) {
                            viewmodel.instruction = Instruction.brainstorm
                            selectedMode = "Brainstorm"
                        }
                    }

                    item {

                        SuggestionCard(
                            icon = Icons.Outlined.School,
                            title = "Learn",
                            subtitle = "Explain any concept",
                            color = Color(0xFFE09000),
                            selected = selectedMode == "Learn"
                        ) {

                            viewmodel.instruction = Instruction.learn
                            selectedMode = "Learn"

                        }
                    }

                    item {

                        SuggestionCard(
                            icon = Icons.Outlined.Code,
                            title = "Code",
                            subtitle = "Write or debug code",
                            color = Color(0xFF00897B),
                            selected = selectedMode == "Code"
                        ) {

                            viewmodel.instruction = Instruction.coding
                            selectedMode = "Code"


                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(
                        1.dp,
                        Color(0xFFEAEAF5),
                        RoundedCornerShape(32.dp))
            ) {

                IconButton(onClick = {
                    //add media button
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF111133)),
                    decorationBox = { inner ->
                        if (question.isEmpty()) Text("Ask anything…", color = Color(0xFFBBBBCC), fontSize = 16.sp)
                        inner()
                    }
                )

                IconButton(onClick = {
                    //Mic Input
                }) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(activeColor),
                    contentAlignment = Alignment.Center
                ) { IconButton(onClick = {
                    if(viewmodel.OnlineMode){
                    viewmodel.sendmessage(question)
                    question = ""}
                    else {
                        viewmodel.sendMessageOffline(question)
                        question = ""
                    }
                }, enabled = question != "" ) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.Send ,
                                contentDescription = null ,
                                tint = Color.White)
                        }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RowScope.ModeButton(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    activeColor: Color,
    onClick: () -> Unit
) {

    val bgColor =
        if (selected) activeColor.copy(alpha = 0.12f)
        else Color.Transparent

    val contentColor =
        if (selected) activeColor
        else Color.Gray

    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                onClick()
            }
            .background(bgColor)
            .padding(vertical = 14.dp),

        contentAlignment = Alignment.Center
    ) {

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SuggestionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .width(170.dp)
            .border(
                width = if(selected) 2.dp else 0.dp,
                color = if(selected) color else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF16162E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun MessageList(
    messageList: List<Response>,
    isLoading: Boolean = false
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        reverseLayout = true
    ) {
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 24.dp,
                                    topEnd = 24.dp,
                                    bottomStart = 6.dp,
                                    bottomEnd = 24.dp
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White, Color(0xFFF9F9FD))
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        TypingIndicator()
                    }
                }
            }
        }

        items(messageList.reversed()) { message ->
            val isUser = message.Role == "User"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement =
                    if (isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = if (isUser) 24.dp else 6.dp,
                                bottomEnd = if (isUser) 6.dp else 24.dp
                            )
                        )
                        .background(
                            if (isUser)
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF5B4DFF), Color(0xFF7B61FF))
                                )
                            else
                                Brush.verticalGradient(
                                    listOf(Color.White, Color(0xFFF9F9FD))
                                )
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = message.message,
                        color = if (isUser) Color.White else Color(0xFF111133),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val offsets = List(dotCount) { index ->
        val delay = index * 150
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at delay
                    6f at delay + 200
                    0f at delay + 400
                    0f at 900
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        offsets.forEach { offset ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offset.value.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5B4DFF).copy(alpha = 0.5f))
            )
        }
    }
}