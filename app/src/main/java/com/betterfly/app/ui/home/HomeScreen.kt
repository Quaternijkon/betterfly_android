package com.betterfly.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.ui.theme.LocalThemeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal val PRESET_COLORS = listOf(
    "#4285F4", "#EA4335", "#FBBC05", "#34A853",
    "#8b5cf6", "#ec4899", "#6366f1", "#14b8a6",
    "#f43f5e", "#a855f7", "#06b6d4", "#84cc16"
)

data class GoalProgress(
    val current: Double,
    val target: Double,
    val type: String,
    val metric: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val eventTypes by viewModel.eventTypes.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val allSessions by viewModel.sessions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val themeColor = LocalThemeColor.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showArchivedSection by remember { mutableStateOf(false) }
    var stopTargetSession by remember { mutableStateOf<Session?>(null) }
    var showNoteStopSession by remember { mutableStateOf<Session?>(null) }
    var editEvent by remember { mutableStateOf<EventType?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) { delay(1000); now = System.currentTimeMillis() }
    }

    val visible = eventTypes.filterNot { it.archived }
    val archived = eventTypes.filter { it.archived }
    val hasActive = activeSessions.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(themeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("betterfly", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = themeColor)
                    }
                },
                actions = {
                    if (hasActive) {
                        Surface(
                            shape = CircleShape,
                            color = themeColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "${activeSessions.size} 进行中",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = themeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "新建事件", tint = themeColor)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (visible.isEmpty() && archived.isEmpty()) {
            EmptyState(
                themeColor = themeColor,
                onCreateClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { event ->
                    val active = activeSessions[event.id]
                    val evSessions = allSessions.filter { it.eventId == event.id && it.endTime != null }
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        EventCard(
                            event = event,
                            activeSession = active,
                            sessions = evSessions,
                            now = now,
                            settings = settings,
                            onEdit = { editEvent = event },
                            onStart = { viewModel.startSession(event) },
                            onStop = { s ->
                                when (settings.stopMode) {
                                    "quick" -> viewModel.quickStop(s)
                                    "note" -> showNoteStopSession = s
                                    else -> stopTargetSession = s
                                }
                            }
                        )
                    }
                }

                if (archived.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { showArchivedSection = !showArchivedSection },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (showArchivedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showArchivedSection) "隐藏已归档 (${archived.size})"
                                else "显示已归档 (${archived.size})",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (showArchivedSection) {
                        items(archived, key = { "arc-" + it.id }) { event ->
                            EventCard(
                                event = event,
                                activeSession = null,
                                sessions = allSessions.filter { it.eventId == event.id && it.endTime != null },
                                now = now,
                                settings = settings,
                                onEdit = { editEvent = event },
                                onStart = {},
                                onStop = {}
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color, tags ->
                viewModel.createEvent(name, color, tags)
                showCreateDialog = false
            }
        )
    }
    stopTargetSession?.let { s ->
        InteractiveStopDialog(
            session = s,
            onDismiss = { stopTargetSession = null },
            onQuickStop = { viewModel.quickStop(s); stopTargetSession = null },
            onSave = { note, rating, incomplete ->
                viewModel.stopSession(s, note, rating, incomplete)
                stopTargetSession = null
            }
        )
    }
    showNoteStopSession?.let { s ->
        NoteStopDialog(
            onDismiss = { showNoteStopSession = null },
            onSave = { note -> viewModel.stopSession(s, note = note); showNoteStopSession = null }
        )
    }
    editEvent?.let { e ->
        EditEventDialog(
            event = e,
            onDismiss = { editEvent = null },
            onSave = { updated, goal, tags ->
                viewModel.updateEvent(updated)
                viewModel.updateGoalAndTags(updated.id, goal, tags)
                editEvent = null
            },
            onArchive = { viewModel.toggleArchive(e); editEvent = null },
            onDelete = { viewModel.deleteEvent(e); editEvent = null }
        )
    }
}

@Composable
private fun EmptyState(
    themeColor: Color,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AddCircle, null,
                    Modifier.size(40.dp),
                    tint = themeColor.copy(alpha = 0.7f)
                )
            }
            Text(
                "开始追踪你的习惯",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "创建事件来记录你每天的活动、习惯和目标",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("创建第一个事件")
            }
        }
    }
}
