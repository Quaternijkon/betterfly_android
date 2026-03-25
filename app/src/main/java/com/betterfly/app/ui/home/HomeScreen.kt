package com.betterfly.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Goal
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.parseHexColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val presetColors = listOf("#4285F4", "#EA4335", "#FBBC05", "#34A853", "#8b5cf6", "#ec4899")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val eventTypes by viewModel.eventTypes.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var stopTargetSession by remember { mutableStateOf<Session?>(null) }
    var showNoteStopSession by remember { mutableStateOf<Session?>(null) }
    var editEvent by remember { mutableStateOf<EventType?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { while (isActive) { delay(1000); now = System.currentTimeMillis() } }

    Scaffold(topBar = {
        TopAppBar(title = { Text("betterfly", fontWeight = FontWeight.Bold, fontSize = 22.sp) }, actions = {
            IconButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "新建") }
            IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "设置") }
        })
    }) { padding ->
        val visibleEvents = eventTypes.filterNot { it.archived }
        if (visibleEvents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有事件"); Spacer(Modifier.height(8.dp)); Button({ showCreateDialog = true }) { Text("创建第一个事件") }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleEvents, key = { it.id }) { event ->
                    val active = activeSessions[event.id]
                    EventCard(event, active, now, onEdit = { editEvent = event }, onStart = { viewModel.startSession(event) }, onStop = { s ->
                        when (settings.stopMode) {
                            "quick" -> viewModel.quickStop(s)
                            "note" -> showNoteStopSession = s
                            else -> stopTargetSession = s
                        }
                    })
                }
            }
        }
    }

    if (showCreateDialog) CreateEventDialog({ showCreateDialog = false }) { name, color -> viewModel.createEvent(name, color); showCreateDialog = false }
    stopTargetSession?.let { s -> StopSessionDialog({ stopTargetSession = null }, { viewModel.quickStop(s); stopTargetSession = null }) { n, r, i -> viewModel.stopSession(s, n, r, i); stopTargetSession = null } }
    showNoteStopSession?.let { s -> NoteStopDialog({ showNoteStopSession = null }) { n -> viewModel.stopSession(s, note = n); showNoteStopSession = null } }
    editEvent?.let { e -> EditEventDialog(e, onDismiss = { editEvent = null }, onSave = { updated, goal, tags -> viewModel.updateEvent(updated); viewModel.updateGoalAndTags(updated.id, goal, tags); editEvent = null }, onArchive = { viewModel.toggleArchive(e); editEvent = null }, onDelete = { viewModel.deleteEvent(e); editEvent = null }) }
}

@Composable
private fun EventCard(event: EventType, activeSession: Session?, now: Long, onEdit: () -> Unit, onStart: () -> Unit, onStop: (Session) -> Unit) {
    val color = parseHexColor(event.color)
    val isActive = activeSession != null
    val elapsed = activeSession?.let { (now - it.startTime) / 1000L } ?: 0L
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (isActive) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Box(Modifier.size(14.dp).clip(CircleShape).background(color)) }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.SemiBold)
                Text(if (isActive) formatDuration(elapsed) else "点击开始记录", color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (event.tags.isNotEmpty()) Text(event.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = color)
                event.goal?.let { g -> Text("目标:${g.type}/${g.metric}/${g.period}/${g.targetValue}", style = MaterialTheme.typography.labelSmall) }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
            FilledIconButton(onClick = { if (isActive) onStop(activeSession!!) else onStart() }) { Icon(if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow, if (isActive) "停止" else "开始") }
        }
    }
}

@Composable
private fun NoteStopDialog(onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("结束记录") }, text = { OutlinedTextField(note, { note = it }, label = { Text("备注") }) }, confirmButton = { TextButton({ onSave(note.ifBlank { null }) }) { Text("保存") } }, dismissButton = { TextButton(onDismiss) { Text("取消") } })
}

@Composable
private fun StopSessionDialog(onDismiss: () -> Unit, onQuickStop: () -> Unit, onSave: (String?, Int?, Boolean) -> Unit) {
    var note by remember { mutableStateOf("") }; var rating by remember { mutableStateOf(0) }; var incomplete by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("结束记录") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") })
            OutlinedTextField(if (rating == 0) "" else rating.toString(), { rating = it.toIntOrNull()?.coerceIn(1, 5) ?: 0 }, label = { Text("强度 1-5（可选）") })
            TextButton({ incomplete = !incomplete }) { Text(if (incomplete) "标记未完成 ✓" else "标记未完成") }
        }
    }, confirmButton = { Button({ onSave(note.ifBlank { null }, rating.takeIf { it in 1..5 }, incomplete) }) { Text("保存") } }, dismissButton = { TextButton(onQuickStop) { Text("快速停止") } })
}

@Composable
private fun CreateEventDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var selectedColor by remember { mutableStateOf(presetColors.first()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("新建事件") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("事件名称") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { presetColors.forEach { hex -> val c = parseHexColor(hex); val s = hex.equals(selectedColor, true); Box(Modifier.size(if (s) 30.dp else 24.dp).clip(CircleShape).background(c).clickable { selectedColor = hex }) } }
            Text("已选颜色：$selectedColor", style = MaterialTheme.typography.labelSmall)
        }
    }, confirmButton = { Button({ onConfirm(name, selectedColor) }, enabled = name.isNotBlank()) { Text("创建") } }, dismissButton = { TextButton(onDismiss) { Text("取消") } })
}

@Composable
private fun EditEventDialog(event: EventType, onDismiss: () -> Unit, onSave: (EventType, Goal?, List<String>) -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(event.name) }; var color by remember { mutableStateOf(event.color) }; var tagsText by remember { mutableStateOf(event.tags.joinToString(",")) }
    var goalType by remember { mutableStateOf(event.goal?.type ?: "positive") }; var goalMetric by remember { mutableStateOf(event.goal?.metric ?: "count") }
    var goalPeriod by remember { mutableStateOf(event.goal?.period ?: "week") }; var goalTarget by remember { mutableStateOf(event.goal?.targetValue?.toString() ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑事件") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("名称") }); OutlinedTextField(color, { color = it }, label = { Text("颜色 #HEX") }); OutlinedTextField(tagsText, { tagsText = it }, label = { Text("标签（逗号分隔）") })
            Text("目标"); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton({ goalType = "positive" }) { Text(if (goalType == "positive") "正向 ✓" else "正向") }
                TextButton({ goalType = "negative" }) { Text(if (goalType == "negative") "反向 ✓" else "反向") }
                TextButton({ goalMetric = if (goalMetric == "count") "duration" else "count" }) { Text("metric:$goalMetric") }
                TextButton({ goalPeriod = if (goalPeriod == "week") "month" else "week" }) { Text("period:$goalPeriod") }
            }
            OutlinedTextField(goalTarget, { goalTarget = it }, label = { Text("目标值") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onArchive) { Text(if (event.archived) "取消归档" else "归档") }; TextButton(onDelete) { Text("删除") } }
        }
    }, confirmButton = { Button({
        val goal = goalTarget.toDoubleOrNull()?.let { Goal(goalType, goalMetric, goalPeriod, it) }
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        onSave(event.copy(name = name.trim(), color = color.trim()), goal, tags)
    }) { Text("保存") } }, dismissButton = { TextButton(onDismiss) { Text("取消") } })
}
