package com.betterfly.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.parseHexColor
import com.betterfly.app.util.toDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val eventTypes by viewModel.eventTypes.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var eventFilter by remember { mutableStateOf("all") }
    var onlyIncomplete by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Session?>(null) }
    var editTarget by remember { mutableStateOf<Session?>(null) }

    val eventMap = eventTypes.associateBy { it.id }
    val sortedSessions = sessions.filter { it.endTime != null }
        .filter { if (eventFilter == "all") true else it.eventId == eventFilter }
        .filter { if (onlyIncomplete) it.incomplete else true }
        .sortedByDescending { it.startTime }

    Scaffold(topBar = {
        TopAppBar(title = { Text("历史记录", fontWeight = FontWeight.Bold) }, actions = {
            TextButton(onClick = { onlyIncomplete = !onlyIncomplete }) { Text(if (onlyIncomplete) "仅未完成 ✓" else "仅未完成") }
        })
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip("全部", eventFilter == "all") { eventFilter = "all" }
                    eventTypes.forEach { e -> FilterChip(e.name, eventFilter == e.id) { eventFilter = e.id } }
                }
            }
            if (sortedSessions.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { Text("暂无记录") } }
            } else {
                items(sortedSessions, key = { it.id }) { s ->
                    eventMap[s.eventId]?.let { event ->
                        SessionHistoryItem(s, event, parseHexColor(event.color), onDelete = { deleteTarget = s }, onEdit = { editTarget = s })
                    }
                }
            }
        }
    }

    deleteTarget?.let { s ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("删除会话") }, text = { Text("确认删除这条记录？") }, confirmButton = {
            TextButton(onClick = { viewModel.deleteSession(s); deleteTarget = null }) { Text("删除") }
        }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } })
    }

    editTarget?.let { s ->
        EditSessionDialog(session = s, onDismiss = { editTarget = null }, onSave = { updated ->
            viewModel.updateSession(updated)
            editTarget = null
        })
    }
}

@Composable private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) { TextButton(onClick = onClick) { Text(if (selected) "$label ✓" else label) } }

@Composable
fun SessionHistoryItem(session: Session, event: EventType, color: Color, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Text(session.startTime.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!session.note.isNullOrBlank()) Text(session.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (session.tags.isNotEmpty()) Text(session.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = color)
            }
            Text(formatDuration(session.durationSeconds ?: 0L), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = color)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
        }
    }
}

@Composable
private fun EditSessionDialog(session: Session, onDismiss: () -> Unit, onSave: (Session) -> Unit) {
    var note by remember { mutableStateOf(session.note ?: "") }
    var rating by remember { mutableStateOf(session.rating ?: 0) }
    var incomplete by remember { mutableStateOf(session.incomplete) }
    var tags by remember { mutableStateOf(session.tags.joinToString(",")) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑会话") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") })
            OutlinedTextField(value = if (rating == 0) "" else rating.toString(), onValueChange = { rating = it.toIntOrNull()?.coerceIn(1, 5) ?: 0 }, label = { Text("强度 1-5") })
            OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签(逗号分隔)") })
            TextButton(onClick = { incomplete = !incomplete }) { Text(if (incomplete) "标记未完成 ✓" else "标记未完成") }
        }
    }, confirmButton = {
        TextButton(onClick = {
            onSave(session.copy(note = note.ifBlank { null }, rating = rating.takeIf { it in 1..5 }, incomplete = incomplete, tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }))
        }) { Text("保存") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
