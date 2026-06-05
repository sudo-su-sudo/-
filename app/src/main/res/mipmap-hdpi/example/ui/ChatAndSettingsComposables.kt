package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Styling Palette Constants ---
val CosmosBackgroundStart = Color(0xFF0F1019)
val CosmosBackgroundEnd = Color(0xFF141727)
val GeminiBlue = Color(0xFF3B82F6)
val GlowPurple = Color(0xFF8B5CF6)
val TechGreen = Color(0xFF10B981)
val WarmOrange = Color(0xFFF59E0B)
val LightSlate = Color(0xFF94A3B8)
val DarkSurface = Color(0xFF1E2136)

enum class AppTab {
    CHAT, DEVELOPER_SETTINGS, WORKSPACE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSandboxApp(viewModel: ChatViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.CHAT) }
    
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedAttachmentUri.collectAsStateWithLifecycle()
    val selectedMime by viewModel.selectedAttachmentMimeType.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val actionLogs by viewModel.actionLogs.collectAsStateWithLifecycle()

    var showSessionMenu by remember { mutableStateOf(false) }

    val containerGradient = Brush.verticalGradient(
        colors = listOf(CosmosBackgroundStart, CosmosBackgroundEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.clickable { showSessionMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSession?.title ?: "No Active Session",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 240.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch session dropdown",
                                    tint = GeminiBlue,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            currentSession?.let {
                                Text(
                                    text = "${it.modelName.uppercase()}  |  T: ${it.temperature}",
                                    fontSize = 11.sp,
                                    color = LightSlate,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.startNewSession() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create new session",
                                tint = GeminiBlue
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                currentSessionId?.let { viewModel.deleteSession(it) }
                            },
                            enabled = currentSessionId != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete this active session",
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                ) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.CHAT,
                        onClick = { selectedTab = AppTab.CHAT },
                        label = { Text("Chat Sandbox", fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == AppTab.CHAT) Icons.Filled.Send else Icons.Outlined.Send,
                                contentDescription = "Chat Sandbox tab descriptor"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeminiBlue,
                            unselectedIconColor = LightSlate,
                            selectedTextColor = GeminiBlue,
                            unselectedTextColor = LightSlate,
                            indicatorColor = GeminiBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_chat_tab")
                    )

                    NavigationBarItem(
                        selected = selectedTab == AppTab.DEVELOPER_SETTINGS,
                        onClick = { selectedTab = AppTab.DEVELOPER_SETTINGS },
                        label = { Text("AI Controls", fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == AppTab.DEVELOPER_SETTINGS) Icons.Filled.Build else Icons.Outlined.Build,
                                contentDescription = "Developer Settings parameters"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlowPurple,
                            unselectedIconColor = LightSlate,
                            selectedTextColor = GlowPurple,
                            unselectedTextColor = LightSlate,
                            indicatorColor = GlowPurple.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_settings_tab")
                    )

                    NavigationBarItem(
                        selected = selectedTab == AppTab.WORKSPACE,
                        onClick = { selectedTab = AppTab.WORKSPACE },
                        label = { Text("Outputs Workspace", fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == AppTab.WORKSPACE) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "Direct file database outputs"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TechGreen,
                            unselectedIconColor = LightSlate,
                            selectedTextColor = TechGreen,
                            unselectedTextColor = LightSlate,
                            indicatorColor = TechGreen.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_workspace_tab")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.CHAT -> {
                        ChatTab(
                            messages = messages,
                            promptInput = promptInput,
                            selectedUri = selectedUri,
                            selectedMime = selectedMime,
                            isThinking = isThinking,
                            actionLogs = actionLogs,
                            onPromptChange = { viewModel.setPromptInput(it) },
                            onSelectFile = { uri, mime -> viewModel.setAttachment(uri, mime) },
                            onClearAttachment = { viewModel.clearAttachment() },
                            onSend = { viewModel.sendMessage() }
                        )
                    }
                    AppTab.DEVELOPER_SETTINGS -> {
                        currentSession?.let { session ->
                            val dynamicSettings by viewModel.allDynamicSettings.collectAsStateWithLifecycle()
                            val customModels by viewModel.allAggregatedModels.collectAsStateWithLifecycle()
                            val settingState by viewModel.settingConversationState.collectAsStateWithLifecycle()
                            
                            DeveloperSettingsTab(
                                session = session,
                                dynamicSettings = dynamicSettings,
                                customModels = customModels,
                                settingState = settingState,
                                onUpdateParameters = { model, temp, system, maxTokens, topP, topK, envOn ->
                                    viewModel.updateSessionParameters(model, temp, system, maxTokens, topP, topK, envOn)
                                },
                                onUpdateTitle = { title ->
                                    viewModel.updateSessionTitle(title)
                                },
                                onInstantScan = { viewModel.scanHardwareSnapshotNow() },
                                onUpdateDynamicSettingValue = { setting, value ->
                                    viewModel.updateDynamicSettingValue(setting, value)
                                },
                                onDeleteDynamicSetting = { id ->
                                    viewModel.deleteDynamicSetting(id)
                                },
                                onSubmitSettingRequest = { req ->
                                    viewModel.submitCustomSettingRequest(req)
                                },
                                onDeploySetting = {
                                    viewModel.confirmAndDeploySetting()
                                },
                                onCancelSetting = {
                                    viewModel.cancelSettingNegotiation()
                                },
                                onAddCustomModel = { name, provider, endpoint, desc ->
                                    viewModel.insertAggregatedModel(
                                        com.example.data.db.AggregatedModel(
                                            modelName = name,
                                            provider = provider,
                                            endpointUrl = endpoint,
                                            description = desc
                                        )
                                    )
                                },
                                onDeleteCustomModel = { id ->
                                    viewModel.deleteAggregatedModel(id)
                                }
                            )
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Please select or start a session.", color = LightSlate)
                        }
                    }
                    AppTab.WORKSPACE -> {
                        val customTools by viewModel.allTools.collectAsStateWithLifecycle()
                        WorkspaceTab(
                            customTools = customTools,
                            onDeleteTool = { id -> viewModel.deleteTool(id) }
                        )
                    }
                }
            }
        }

        // Dropdown Menu overlay to select session index
        if (showSessionMenu) {
            AlertDialog(
                onDismissRequest = { showSessionMenu = false },
                containerColor = DarkSurface,
                titleContentColor = Color.White,
                textContentColor = LightSlate,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Switch Saved Sessions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.startNewSession(); showSessionMenu = false }) {
                            Icon(Icons.Default.Add, contentDescription = "Add new session", tint = GeminiBlue)
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (s.id == currentSessionId) GeminiBlue.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.02f)
                                    )
                                    .clickable {
                                        viewModel.selectSession(s.id)
                                        showSessionMenu = false
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = if (s.id == currentSessionId) GeminiBlue.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${s.modelName.uppercase()} • T:${s.temperature}",
                                        color = LightSlate,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                if (s.id == currentSessionId) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active target selected",
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSessionMenu = false }) {
                        Text("Close", color = GeminiBlue)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatTab(
    messages: List<ChatMessage>,
    promptInput: String,
    selectedUri: String?,
    selectedMime: String?,
    isThinking: Boolean,
    actionLogs: List<String>,
    onPromptChange: (String) -> Unit,
    onSelectFile: (uri: String, mimeType: String) -> Unit,
    onClearAttachment: () -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // File attachments handler: open native system storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            // Get content description / resolve Uri
            onSelectFile(uri.toString(), mimeType)
        }
    }

    // Dynamic list safety: target scrolls down when message size scales
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    val transition = rememberInfiniteTransition()
                    val pulseScale by transition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(64.dp)
                            .background(
                                Brush.radialGradient(listOf(GeminiBlue.copy(alpha = 0.4f), Color.Transparent)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "No messages indicator",
                            tint = GeminiBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Gemini Developer Sandbox",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Type a physical scan request, prompt the AI tools, share files natively, or adjust AI creativity sliders in the controls tab.",
                        fontSize = 13.sp,
                        color = LightSlate,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg = msg)
                }

                if (isThinking) {
                    item {
                        ThinkingPlaceholderIndicator()
                    }
                }
            }
        }

        // Live Executed direct action system feedback monitor panel (for developers)
        AnimatedVisibility(
            visible = actionLogs.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, TechGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Active executing debugger summary",
                        tint = TechGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLogs.firstOrNull() ?: "Standby",
                        fontSize = 11.sp,
                        color = TechGreen,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Input Tray Area
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                // Showing selected attachments inside input bar
                if (selectedUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, GeminiBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilePresent,
                            contentDescription = "File Loaded attachment",
                            tint = GeminiBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Intake Payload Linked",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mime: $selectedMime",
                                color = LightSlate,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onClearAttachment) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Discard attached payload",
                                tint = Color.Red.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Load files/images attachment asset",
                            tint = GeminiBlue
                        )
                    }

                    TextField(
                        value = promptInput,
                        onValueChange = onPromptChange,
                        placeholder = { Text("Message Gemini, share errors...", color = LightSlate.copy(alpha = 0.8f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = onSend,
                        enabled = promptInput.isNotEmpty() || selectedUri != null,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = GeminiBlue,
                            disabledContentColor = LightSlate.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Query Gemini model API key payload"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingPlaceholderIndicator() {
    val transition = rememberInfiniteTransition()
    val alphaAnim by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(GeminiBlue.copy(alpha = 0.2f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = "Calculating custom actions",
                tint = GeminiBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .width(180.dp)
                .scale(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(LightSlate.copy(alpha = alphaAnim), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(LightSlate.copy(alpha = alphaAnim), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(LightSlate.copy(alpha = alphaAnim), CircleShape))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thinking & scanning...",
                    fontSize = 11.sp,
                    color = LightSlate,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val bubbleBrush = if (isUser) {
        Brush.horizontalGradient(listOf(GeminiBlue, GlowPurple))
    } else {
        Brush.linearGradient(listOf(DarkSurface, DarkSurface))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GlowPurple.copy(alpha = 0.15f), shape = CircleShape)
                    .border(1.dp, GlowPurple.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini custom agent profile avatar",
                    tint = GlowPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = bubbleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .background(bubbleBrush, bubbleShape)
                    .border(
                        width = 1.dp,
                        color = if (isUser) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                        shape = bubbleShape
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = msg.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Displays action logs embedded inside database message body
                    msg.executedAction?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Active action ran indicator",
                                tint = TechGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = status,
                                color = TechGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (msg.filePath != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = "Payload attachment",
                        tint = LightSlate,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Linked media attachment asset",
                        color = LightSlate,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GeminiBlue.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DEV",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlue
                )
            }
        }
    }
}

// --- Dynamic descriptions expander custom element ---
@Composable
fun InfoDescriptionRow(
    title: String,
    description: String,
    tintColor: Color = GeminiBlue
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.Info else Icons.Outlined.Info,
                contentDescription = "View detail variable explanations",
                tint = tintColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = description,
                fontSize = 12.sp,
                color = LightSlate,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun DeveloperSettingsTab(
    session: ChatSession,
    dynamicSettings: List<com.example.data.db.DynamicSetting>,
    customModels: List<com.example.data.db.AggregatedModel>,
    settingState: ChatViewModel.SettingConversationalState,
    onUpdateParameters: (model: String, temp: Float, system: String, maxTokens: Int, topP: Float, topK: Int, envScanner: Boolean) -> Unit,
    onUpdateTitle: (title: String) -> Unit,
    onInstantScan: () -> String,
    onUpdateDynamicSettingValue: (com.example.data.db.DynamicSetting, String) -> Unit,
    onDeleteDynamicSetting: (Long) -> Unit,
    onSubmitSettingRequest: (String) -> Unit,
    onDeploySetting: () -> Unit,
    onCancelSetting: () -> Unit,
    onAddCustomModel: (name: String, provider: String, url: String, desc: String) -> Unit,
    onDeleteCustomModel: (Long) -> Unit
) {
    var titleInput by remember(session.title) { mutableStateOf(session.title) }
    var scaleTemp by remember(session.temperature) { mutableStateOf(session.temperature) }
    var maxTokensLimit by remember(session.maxTokens) { mutableStateOf(session.maxTokens.toFloat()) }
    var topPInput by remember(session.topP) { mutableStateOf(session.topP) }
    var topKInput by remember(session.topK) { mutableStateOf(session.topK.toFloat()) }
    var isEnvEnabled by remember(session.hardwareScannerEnabled) { mutableStateOf(session.hardwareScannerEnabled) }
    var systemPromptInput by remember(session.systemInstructions) { mutableStateOf(session.systemInstructions) }
    
    var selectedModel by remember(session.modelName) { mutableStateOf(session.modelName) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var instantScannerFeedbackText by remember { mutableStateOf("") }
    
    val modelsAvailable = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-2.5-flash-image",
        "gemini-3.1-flash-image-preview",
        "gemini-2.5-flash-preview-tts"
    )

    // Merge default models with dynamically aggregated custom models!
    val allModelsList = modelsAvailable + customModels.map { it.modelName }

    // Dialog controllers for adding aggregated models
    var showAddModelDialog by remember { mutableStateOf(false) }
    var newModelName by remember { mutableStateOf("") }
    var newModelProvider by remember { mutableStateOf("Custom Adapter") }
    var newModelEndpoint by remember { mutableStateOf("https://") }
    var newModelDesc by remember { mutableStateOf("") }

    // Text box state for custom setting configurations negotiation
    var conversationalStepInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Model selection card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowPurple.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Generation Model Selector",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports aggregation of standard Gemini API targets and registered custom gateways.",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { modelDropdownExpanded = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedModel.uppercase(),
                                    color = GlowPurple,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown indicator", tint = GlowPurple)
                            }
                        }

                        IconButton(
                            onClick = { showAddModelDialog = true },
                            modifier = Modifier
                                .background(GlowPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aggregate custom model", tint = GlowPurple)
                        }
                    }

                    // Render Custom Registered Models deletion list
                    if (customModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Aggregated Custom Models list", color = LightSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        customModels.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(model.modelName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${model.provider} • ${model.endpointUrl}", color = LightSlate, fontSize = 10.sp, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { onDeleteCustomModel(model.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete custom model", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(DarkSurface)
                    ) {
                        allModelsList.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.uppercase(), color = Color.White, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedModel = m
                                    modelDropdownExpanded = false
                                    onUpdateParameters(m, scaleTemp, systemPromptInput, maxTokensLimit.toInt(), topPInput, topKInput.toInt(), isEnvEnabled)
                                },
                                modifier = Modifier.background(
                                    if (m == selectedModel) Color.White.copy(alpha = 0.05f) else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        // Live Environment Scanner snapshot telemetry card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoDescriptionRow(
                        title = "Hardware Ambiance Sniffer Telemetry",
                        description = "Feeds real ambient light metrics, direct location times, battery, and scanner values straight to generative reasoning prompts constraints."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inject Ambiance Telemetry", color = LightSlate, fontSize = 13.sp)
                        Switch(
                            checked = isEnvEnabled,
                            onCheckedChange = {
                                isEnvEnabled = it
                                onUpdateParameters(selectedModel, scaleTemp, systemPromptInput, maxTokensLimit.toInt(), topPInput, topKInput.toInt(), it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TechGreen,
                                checkedTrackColor = TechGreen.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            instantScannerFeedbackText = onInstantScan()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TechGreen.copy(alpha = 0.15f), contentColor = TechGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TechGreen.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.Default.SensorWindow, contentDescription = "Scanner snapshot status", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Measure Environment Snapshot", fontSize = 13.sp)
                    }

                    if (instantScannerFeedbackText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = instantScannerFeedbackText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TechGreen
                            )
                        }
                    }
                }
            }
        }

        // Sliders card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Developer Param Controls", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Column {
                        Text("Conversational Session Title Label", color = LightSlate, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = titleInput,
                            onValueChange = {
                                titleInput = it
                                onUpdateTitle(it)
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 1. Temperature Sliders
                    Column {
                        InfoDescriptionRow(
                            title = "Creativity Engine (Temperature): ${String.format("%.2f", scaleTemp)}",
                            description = "Controls response structural random entropy. High levels favor speculative creativity; low values enforce factual determinism."
                        )
                        Slider(
                            value = scaleTemp,
                            onValueChange = {
                                scaleTemp = it
                                onUpdateParameters(selectedModel, it, systemPromptInput, maxTokensLimit.toInt(), topPInput, topKInput.toInt(), isEnvEnabled)
                            },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GeminiBlue,
                                activeTrackColor = GeminiBlue
                            )
                        )
                    }

                    // 2. Max output tokens
                    Column {
                        InfoDescriptionRow(
                            title = "Response Length Scope: ${maxTokensLimit.toInt()} Tokens",
                            description = "Controls memory budget allocated to the output sequence text structures."
                        )
                        Slider(
                            value = maxTokensLimit,
                            onValueChange = {
                                maxTokensLimit = it
                                onUpdateParameters(selectedModel, scaleTemp, systemPromptInput, it.toInt(), topPInput, topKInput.toInt(), isEnvEnabled)
                            },
                            valueRange = 128f..8192f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlowPurple,
                                activeTrackColor = GlowPurple
                            )
                        )
                    }

                    // 3. Top P
                    Column {
                        InfoDescriptionRow(
                            title = "Confidence Filter (Top-P): ${String.format("%.2f", topPInput)}",
                            description = "The engine samples words belonging to top probability ranges summation parameters."
                        )
                        Slider(
                            value = topPInput,
                            onValueChange = {
                                topPInput = it
                                onUpdateParameters(selectedModel, scaleTemp, systemPromptInput, maxTokensLimit.toInt(), it, topKInput.toInt(), isEnvEnabled)
                            },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = WarmOrange,
                                activeTrackColor = WarmOrange
                            )
                        )
                    }
                }
            }
        }

        // Custom underlying instructions card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoDescriptionRow(
                        title = "Underlying System Instructions blueprint",
                        description = "Direct contextual templates injected to outline conversational rules, response formatting, and layout styles."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = systemPromptInput,
                        onValueChange = {
                            systemPromptInput = it
                            onUpdateParameters(selectedModel, scaleTemp, it, maxTokensLimit.toInt(), topPInput, topKInput.toInt(), isEnvEnabled)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("Enter prompt blueprints...") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 6
                    )
                }
            }
        }

        // Dynamic Settings Canvas (Renders active custom settings!)
        if (dynamicSettings.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TechGreen.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Deployed Dynamic Settings Canvas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TechGreen
                        )
                        Text(
                            text = "Functional controls injected dynamically via the conversational settings co-pilot or directly generated by the AI.",
                            fontSize = 11.sp,
                            color = LightSlate
                        )

                        dynamicSettings.forEach { setting ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(setting.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(setting.description, color = LightSlate, fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { onDeleteDynamicSetting(setting.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete custom parameter", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))

                                when (setting.type.lowercase().trim()) {
                                    "switch" -> {
                                        val isChecked = setting.currentValue.toBoolean()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Active Switch Toggle state: ${if (isChecked) "ON" else "OFF"}", color = LightSlate, fontSize = 12.sp)
                                            Switch(
                                                checked = isChecked,
                                                onCheckedChange = { onUpdateDynamicSettingValue(setting, it.toString()) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = TechGreen,
                                                    checkedTrackColor = TechGreen.copy(alpha = 0.4f)
                                                )
                                            )
                                        }
                                    }
                                    "slider" -> {
                                        val floatVal = setting.currentValue.toFloatOrNull() ?: setting.rangeStart
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Current Value: ${String.format("%.1f", floatVal)}", color = LightSlate, fontSize = 12.sp)
                                                Text("Range: ${setting.rangeStart}..${setting.rangeEnd}", color = LightSlate, fontSize = 10.sp)
                                            }
                                            Slider(
                                                value = floatVal,
                                                valueRange = setting.rangeStart..setting.rangeEnd,
                                                onValueChange = { onUpdateDynamicSettingValue(setting, it.toString()) },
                                                colors = SliderDefaults.colors(
                                                    thumbColor = TechGreen,
                                                    activeTrackColor = TechGreen
                                                )
                                            )
                                        }
                                    }
                                    else -> { // text/string injection
                                        TextField(
                                            value = setting.currentValue,
                                            onValueChange = { onUpdateDynamicSettingValue(setting, it) },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Conversational Custom Setting negotiation drawer card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (settingState.currentStep > 0) GlowPurple else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = "Active conversational designer icon", tint = GlowPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Co-Create Custom Settings Panel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Negotiate and deploy custom sliders, switches, or text options to the dashboard using standard natural language (2-3 rounds prompt approval).",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                    
                    if (settingState.currentStep > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Display co-pilot speech bubble
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlowPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, GlowPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Designer agent avatar", tint = GlowPurple, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI ARCHITECT AGENT:", color = GlowPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settingState.lastModelReply,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = conversationalStepInput,
                        onValueChange = { conversationalStepInput = it },
                        placeholder = {
                            Text(
                                text = when (settingState.currentStep) {
                                    0 -> "e.g., Add light-bias slider from 1 to 10..."
                                    1 -> "e.g., Match boundary values to 1..50 instead..."
                                    else -> "Refinement/Feedback..."
                                },
                                color = LightSlate.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (settingState.currentStep == 0) {
                            Button(
                                onClick = {
                                    onSubmitSettingRequest(conversationalStepInput)
                                    conversationalStepInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowPurple, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Propose Setting", fontSize = 13.sp)
                            }
                        } else if (settingState.currentStep == 1) {
                            Button(
                                onClick = {
                                    onSubmitSettingRequest(conversationalStepInput)
                                    conversationalStepInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowPurple, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit Refinement", fontSize = 13.sp)
                            }
                            
                            Button(
                                onClick = {
                                    onDeploySetting()
                                    conversationalStepInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechGreen, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Deploy Asset", fontSize = 13.sp)
                            }
                        } else if (settingState.currentStep >= 2) {
                            Button(
                                onClick = {
                                    onDeploySetting()
                                    conversationalStepInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TechGreen, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm & Deploy", fontSize = 13.sp)
                            }
                        }

                        if (settingState.currentStep > 0) {
                            TextButton(
                                onClick = {
                                    onCancelSetting()
                                    conversationalStepInput = ""
                                }
                            ) {
                                Text("Clear", color = Color.Red.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to compile raw models
    if (showAddModelDialog) {
        AlertDialog(
            onDismissRequest = { showAddModelDialog = false },
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = LightSlate,
            title = {
                Text("Aggregate Custom model API", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Register alternative API models or gateways so that the app acts as a single model aggregator.", fontSize = 11.sp, color = LightSlate)
                    
                    TextField(
                        value = newModelName,
                        onValueChange = { newModelName = it },
                        placeholder = { Text("Model Name (e.g. custom-pro-agent)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    TextField(
                        value = newModelProvider,
                        onValueChange = { newModelProvider = it },
                        placeholder = { Text("Provider Gateway Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    TextField(
                        value = newModelEndpoint,
                        onValueChange = { newModelEndpoint = it },
                        placeholder = { Text("API Endpoint Service URL") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    TextField(
                        value = newModelDesc,
                        onValueChange = { newModelDesc = it },
                        placeholder = { Text("Description of model scope...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newModelName.isNotEmpty()) {
                            onAddCustomModel(newModelName, newModelProvider, newModelEndpoint, newModelDesc)
                            showAddModelDialog = false
                            newModelName = ""
                            newModelDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowPurple)
                ) {
                    Text("Aggregate Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModelDialog = false }) {
                    Text("Dismiss", color = LightSlate)
                }
            }
        )
    }
}

// --- Live Document Workspace Explorer & Tools Indexer Composable ---
@Composable
fun WorkspaceTab(
    customTools: List<com.example.data.db.CustomTool>,
    onDeleteTool: (Long) -> Unit
) {
    val context = LocalContext.current
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    // Read stored workspace files from folder directory filesDir
    fun reloadFiles() {
        val folder = context.filesDir
        fileList = folder.listFiles()?.filter { it.isFile && (it.name.endsWith(".txt") || it.name.endsWith(".json") || it.name.endsWith(".log")) } ?: emptyList()
    }

    LaunchedEffect(Unit) {
        reloadFiles()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Sandbox Outputs Workspace",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Examine files written by AI instructions and browse active custom-built tools.",
                    fontSize = 11.sp,
                    color = LightSlate
                )
            }
        }

        // Section 1: Written Local Files Directory
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Written Local Files Map", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = { reloadFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh output log files", tint = GeminiBlue)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (fileList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No local documents written yet.\nTrigger a <local_action type=\"write_file\"> block in chat.",
                                color = LightSlate,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            fileList.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedFileName = file.name
                                            selectedFileContent = try {
                                                file.readText()
                                            } catch (e: Exception) {
                                                "Error reading file: ${e.localizedMessage}"
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Description, contentDescription = "Text file model output", tint = GeminiBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(file.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${file.length()} Bytes • Text File", color = LightSlate, fontSize = 10.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = { file.delete(); reloadFiles() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete file asset", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Exhaustive Interactive Custom Tools Index
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlowPurple.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛠️ Dynamic AI Tools Library", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Exhaustive library index where custom compiled tools built by agents can be accessed in combinations.", fontSize = 11.sp, color = LightSlate)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (customTools.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom tools compiled yet.\nAsk Gemini to create a dynamic custom tool to solve an automated query!",
                                color = LightSlate,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customTools.forEach { tool ->
                                var isExpanded by remember { mutableStateOf(false) }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                        .border(1.dp, GlowPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(GlowPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(tool.targetCategory.uppercase(), color = GlowPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(tool.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(tool.description, color = LightSlate, fontSize = 11.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { isExpanded = !isExpanded },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand details",
                                                    tint = LightSlate
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteTool(tool.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete custom tool", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text("INPUT SCHEMA:", color = GlowPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(tool.inputSchemaJson, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text("PROCEDURAL BLUEPRINT SPECIFICATION:", color = GlowPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(tool.codeOrSpecification, color = LightSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Viewing tool description overlay dialog
    if (selectedFileContent != null) {
        AlertDialog(
            onDismissRequest = { selectedFileContent = null },
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = LightSlate,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedFileName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { selectedFileContent = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss explorer dialog", tint = Color.White)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .heightIn(max = 280.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = selectedFileContent ?: "",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFileContent = null }) {
                    Text("Done", color = GeminiBlue)
                }
            }
        )
    }
}
