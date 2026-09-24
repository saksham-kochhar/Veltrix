package com.example.veltrix.Screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.veltrix.AiModelOption
import com.example.veltrix.AiModels
import com.example.veltrix.AiProvider
import com.example.veltrix.Instruction
import com.example.veltrix.Navigation.Routes
import com.example.veltrix.Response
import com.example.veltrix.isLimitExhaustedMessage
import com.example.veltrix.chathistorry.ChatHistoryDrawerContent
import com.example.veltrix.veltrixviewmodel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(viewmodel : veltrixviewmodel , navController: NavHostController) {

     var question by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Normal") }
    var selectedModelId by remember { mutableStateOf(viewmodel.selectedModelId) }
    var showChatModeSheet by remember { mutableStateOf(false) }
    val chatModeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val modeColor = when (selectedMode) {
        "Brainstorm" -> Color(0xFF7C4DFF)
        "Learn" -> Color(0xFFFF9800)
        "Coding" -> Color(0xFF009688)
        else -> Color(0xFFF5F5FA)
    }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewmodel.initChatHistory(context)
        viewmodel.refreshModelStatus(context)
        if (viewmodel.OnlineMode) {
            viewmodel.refreshOnlineAccount()
        }
        if (viewmodel.isModelDownloaded) {
            viewmodel.loadLocalModel(context)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawerContent(
                sessions = viewmodel.sessionSummaries.toList(),
                currentSessionId = viewmodel.currentSessionId,
                statusMessage = viewmodel.historyStatusMessage,
                onNewChat = {
                    viewmodel.startNewChat(context)
                    scope.launch { drawerState.close() }
                },
                onOpenSession = { id ->
                    viewmodel.openSession(context, id)
                    scope.launch { drawerState.close() }
                },
                onClose = {
                    scope.launch { drawerState.close() }
                },
                onOpenAccount = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate(Routes.account)
                    }
                }
            )
        }
    ) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = modeColor.copy(alpha = 0.06f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            val selectedOnline = AiModels.find(selectedModelId)
            val displayModel = if (viewmodel.OnlineMode) {
                selectedOnline?.displayName ?: AiModels.displayName(selectedModelId)
            } else {
                AiModels.OFFLINE_DISPLAY
            }
            val modelIcon = when {
                !viewmodel.OnlineMode -> AiModels.offline.icon
                else -> selectedOnline?.icon ?: Icons.Outlined.AutoAwesome
            }
            val modelIconTint = when {
                !viewmodel.OnlineMode -> AiModels.offline.tint
                else -> selectedOnline?.tint ?: Color(0xFF5B4DFF)
            }
            val headerProviders = if (viewmodel.OnlineMode) {
                AiModels.providersForAllowlist(viewmodel.allowedOnlineModelIds)
            } else {
                listOf(AiModels.offlineProvider)
            }
            val headerSelectedModelId = if (viewmodel.OnlineMode) {
                selectedModelId
            } else {
                AiModels.offline.id
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { scope.launch { drawerState.open() } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Chat history",
                        tint = Color(0xFF1C1C3A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF0F1F5))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeButton(
                        title = "Online",
                        selected = viewmodel.OnlineMode,
                        icon = Icons.Outlined.Language,
                        activeColor = Color(0xFF5B4DFF),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewmodel.OnlineMode = true
                        viewmodel.refreshOnlineAccount()
                    }

                    ModeButton(
                        title = "Offline",
                        selected = !viewmodel.OnlineMode,
                        icon = Icons.Outlined.CloudOff,
                        activeColor = Color(0xFF00C853),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewmodel.OnlineMode = false
                    }
                }

                HeaderModelSelector(
                    displayName = displayModel,
                    icon = modelIcon,
                    iconTint = modelIconTint,
                    providers = headerProviders,
                    selectedId = headerSelectedModelId,
                    onSelect = { modelId ->
                        if (viewmodel.OnlineMode) {
                            selectedModelId = modelId
                            viewmodel.selectedModelId = modelId
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .defaultMinSize(minHeight = 0.dp)
                    .padding(vertical = 14.dp)
            ) {
                MessageList(
                    messageList = viewmodel.messagelist,
                    isLoading = viewmodel.loading,
                    isModelDownloaded = viewmodel.isModelDownloaded,
                    isOfflineMode = !viewmodel.OnlineMode,
                    onSwitchToOffline = { viewmodel.switchToOfflineMode(context) }
                )
            }



            if (!viewmodel.OnlineMode && !viewmodel.isModelDownloaded) {
                OfflineDownloadBanner(
                    isDownloading = viewmodel.isDownloading,
                    downloadProgress = viewmodel.downloadProgress,
                    onDownload = { viewmodel.downloadModel(context) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val composerBg = Color.White
            val composerControlBg = Color(0xFFF0F1F5)
            val composerIconTint = Color(0xFF6B6B7A)
            val composerBorder = Color(0xFFD8D4F0)
            val sendColor by animateColorAsState(
                if (viewmodel.OnlineMode) Color(0xFF5B4DFF) else Color(0xFF00C853),
                label = "sendColor"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color(0xFF5B4DFF).copy(alpha = 0.14f),
                        ambientColor = Color(0xFF5B4DFF).copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(composerBg)
                    .border(1.5.dp, composerBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                if (viewmodel.OnlineMode) {
                    val w = viewmodel.walletSnapshot
                    Text(
                        text = "Credits: ${w.creditsUsed} / ${w.creditsLimit}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B6B7A),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 28.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF111133),
                        lineHeight = 22.sp
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (question.isEmpty()) {
                                Text(
                                    "Ask anything…",
                                    color = Color(0xFF9A9AAB),
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(composerControlBg)
                                .clickable {
                                    // add media
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add",
                                tint = Color(0xFF1C1C3A),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        ComposerModeChip(
                            mode = selectedMode,
                            backgroundColor = composerControlBg,
                            onClick = { showChatModeSheet = true }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(composerControlBg)
                                .clickable {
                                    // mic input
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Voice input",
                                tint = composerIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (question.isNotBlank()) sendColor
                                    else sendColor.copy(alpha = 0.45f)
                                )
                                .clickable(enabled = question.isNotBlank()) {
                                    viewmodel.initChatHistory(context)
                                    if (viewmodel.OnlineMode) {
                                        viewmodel.sendmessage(question)
                                        question = ""
                                    } else {
                                        viewmodel.sendMessageOffline(question)
                                        question = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showChatModeSheet) {
                ChatModeBottomSheet(
                    sheetState = chatModeSheetState,
                    selectedMode = selectedMode,
                    onDismiss = { showChatModeSheet = false },
                    onSelectMode = { mode, instruction ->
                        selectedMode = mode
                        viewmodel.instruction = instruction
                        showChatModeSheet = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatModeBottomSheet(
    sheetState: SheetState,
    selectedMode: String,
    onDismiss: () -> Unit,
    onSelectMode: (mode: String, instruction: String) -> Unit
) {
    val accent = Color(0xFF5B4DFF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1A1A3A)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeGridCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Normal",
                    subtitle = "Balanced assistant",
                    color = accent,
                    selected = selectedMode == "Normal",
                    onClick = { onSelectMode("Normal", Instruction.normal) }
                )
                ModeGridCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Lightbulb,
                    title = "Brainstorm",
                    subtitle = "Generate ideas",
                    color = Color(0xFF7C4DFF),
                    selected = selectedMode == "Brainstorm",
                    onClick = { onSelectMode("Brainstorm", Instruction.brainstorm) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeGridCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.School,
                    title = "Learn",
                    subtitle = "Explain concepts",
                    color = Color(0xFFFF9800),
                    selected = selectedMode == "Learn",
                    onClick = { onSelectMode("Learn", Instruction.learn) }
                )
                ModeGridCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Code,
                    title = "Coding",
                    subtitle = "Write or debug code",
                    color = Color(0xFF009688),
                    selected = selectedMode == "Coding",
                    onClick = { onSelectMode("Coding", Instruction.coding) }
                )
            }
        }
    }
}

@Composable
fun ModelLogoBadge(
    icon: ImageVector,
    tint: Color,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderModelSelector(
    displayName: String,
    icon: ImageVector,
    iconTint: Color,
    providers: List<AiProvider>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModelLogoBadge(
        icon = icon,
        tint = iconTint,
        name = displayName,
        modifier = Modifier.clickable { showPicker = true }
    )

    if (showPicker) {
        ModelProviderSheet(
            sheetState = sheetState,
            providers = providers,
            selectedId = selectedId,
            onDismiss = { showPicker = false },
            onSelect = { modelId ->
                onSelect(modelId)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelProviderSheet(
    sheetState: SheetState,
    providers: List<AiProvider>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val selectedProviderId = remember(selectedId, providers) {
        providers.find { provider -> provider.models.any { it.id == selectedId } }?.id
    }
    var expandedProviderId by remember { mutableStateOf(selectedProviderId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Choose a model",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF1A1A3A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pick a provider, then a model",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF8A8A98)
            )
            Spacer(modifier = Modifier.height(20.dp))

            providers.forEach { provider ->
                ProviderAccordion(
                    provider = provider,
                    selectedId = selectedId,
                    expanded = expandedProviderId == provider.id,
                    onToggle = {
                        expandedProviderId =
                            if (expandedProviderId == provider.id) null else provider.id
                    },
                    onSelect = onSelect
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ProviderAccordion(
    provider: AiProvider,
    selectedId: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit
) {
    val accent = Color(0xFF5B4DFF)
    val containsSelected = provider.models.any { it.id == selectedId }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "providerChevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (containsSelected) accent.copy(alpha = 0.06f) else Color(0xFFF5F5FA))
            .border(
                width = if (containsSelected) 1.5.dp else 0.dp,
                color = if (containsSelected) accent else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModelLogoBadge(
                icon = provider.icon,
                tint = provider.tint,
                name = provider.displayName,
                size = 36.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A3A)
                )
                Text(
                    text = "${provider.models.size} ${if (provider.models.size == 1) "model" else "models"}",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A98)
                )
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color(0xFF8A8A98),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                provider.models.forEach { model ->
                    ProviderModelRow(
                        model = model,
                        selected = model.id == selectedId,
                        onClick = { onSelect(model.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderModelRow(
    model: AiModelOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF5B4DFF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.7f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else Color(0xFFE8E8F0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 15.sp,
                color = Color(0xFF1A1A3A)
            )
            Text(
                text = AiModels.tierLabel(model.minTier),
                fontSize = 12.sp,
                color = Color(0xFF8A8A98)
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) accent else Color(0xFFC8C8D4),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
fun ComposerModeChip(
    mode: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = when (mode) {
        "Learn" -> "Learning"
        else -> mode
    }

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1C1C3A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Change mode",
            tint = Color(0xFF6B6B7A),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ModeGridCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF5B4DFF)
    val borderColor = if (selected) accent else Color.Transparent
    val bgColor = if (selected) accent.copy(alpha = 0.08f) else Color(0xFFF5F5FA)

    Box(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF1A1A3A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8A8A98),
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun ModelOption(
    icon: ImageVector,
    title: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF5B4DFF)
    val bgColor = if (selected) accent.copy(alpha = 0.08f) else Color(0xFFF5F5FA)
    val borderColor = if (selected) accent else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF1A1A3A),
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) accent else Color(0xFFC8C8D4),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor =
        if (selected) Color.White
        else Color.Transparent

    val contentColor =
        if (selected) activeColor
        else Color(0xFF8A8A98)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = title,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
fun OfflineDownloadBanner(
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerScroll = rememberScrollState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val reservedVerticalSpaceDp = 300
    val maxBannerHeight = (screenHeightDp - reservedVerticalSpaceDp).coerceAtLeast(180).dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxBannerHeight),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(bannerScroll)
                .padding(20.dp)
        ) {
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
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(20.dp))

            if (isDownloading) {
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
                        "${(downloadProgress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF00C853),
                    trackColor = Color(0xFFE0E0E0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${(downloadProgress * 1500).toInt()} MB / 1500 MB",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
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
                        onClick = onDownload,
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

@Composable
fun MessageList(
    messageList: List<Response>,
    isLoading: Boolean = false,
    isModelDownloaded: Boolean = false,
    isOfflineMode: Boolean = false,
    onSwitchToOffline: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messageList.size, isLoading) {
        val lastIndex = messageList.lastIndex + if (isLoading) 1 else 0
        if (lastIndex < 0) return@LaunchedEffect

        val layout = listState.layoutInfo
        val allItemsFitInViewport = layout.totalItemsCount > 0 &&
            layout.visibleItemsInfo.size >= layout.totalItemsCount

        if (allItemsFitInViewport) {
            // Avoid scroll-to-last padding that spreads messages apart when the list is short.
            listState.scrollToItem(0)
        } else {
            listState.animateScrollToItem(lastIndex)
        }
    }

    val assistantBubbleBorder = Color(0xFFD5D3E6)
    val bubbleTextStyle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(messageList) { message ->
            val isUser = message.Role == "User"
            val isLimitMessage = message.isLimitExhaustedMessage()
            val textColor = if (isUser) Color.White else Color(0xFF111133)
            val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.82f).dp
            val bubbleShape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            )
            val displayText = message.message.trim()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = maxBubbleWidth)
                            .then(
                                if (!isUser) {
                                    Modifier.border(1.dp, assistantBubbleBorder, bubbleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clip(bubbleShape)
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
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isUser) {
                                AnnotatedString(displayText)
                            } else {
                                parseChatMarkdown(
                                    raw = displayText,
                                    textColor = textColor,
                                    codeBackgroundHint = Color(0xFFE0E0EA)
                                )
                            },
                            color = textColor,
                            style = bubbleTextStyle,
                            softWrap = true
                        )
                    }
                }

                if (isLimitMessage && !isOfflineMode) {
                    OfflineLimitCard(
                        isModelDownloaded = isModelDownloaded,
                        onUseOffline = onSwitchToOffline
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    val typingShape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 6.dp,
                        bottomEnd = 20.dp
                    )
                    Box(
                        modifier = Modifier
                            .widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.82f).dp)
                            .border(1.dp, assistantBubbleBorder, typingShape)
                            .clip(typingShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White, Color(0xFFF9F9FD))
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineLimitCard(
    isModelDownloaded: Boolean,
    onUseOffline: () -> Unit
) {
    val offlineGreen = Color(0xFF00C853)
    val offlineGreenLight = Color(0xFFE8FFF1)
    val offlineGreenBanner = Color(0xFFE9FFF0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(offlineGreenBanner),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = offlineGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Switch to Offline Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF111133)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Continue chatting without internet. Responses may take more time.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = offlineGreenLight,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint = offlineGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isModelDownloaded) {
                            "Offline mode is enabled on your device. No internet required."
                        } else {
                            "Download the model to chat without internet."
                        },
                        color = Color(0xFF007A3D),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUseOffline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = offlineGreen)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Use Offline Model",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
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