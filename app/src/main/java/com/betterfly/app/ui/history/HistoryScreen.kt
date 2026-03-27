package com.betterfly.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.parseHexColor
import com.betterfly.app.util.toDayKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val eventTypes by viewModel.eventTypes.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    var eventFilter by remember { mutableStateOf("all") }
    var onlyIncomplete by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Session?>(null) }
    var editTarget by remember { mutableStateOf<Session?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val eventMap = eventTypes.associateBy { it.id }

    val filtered = sessions
        .filter { it.endTime != null }
        .filter { if (eventFilter == "all") true else it.eventId == eventFilter }
        .filter { if (onlyIncomplete) it.incomplete else true }
        .filter {
            if (searchQuery.isBlank()) true
            else (it.note?.contains(searchQuery, ignoreCase = true) == true ||
                it.tags.any { t -> t.contains(searchQuery, ignoreCase = true) } ||
                eventMap[it.eventId]?.name?.contains(searchQuery, ignoreCase = true) == true)
        }
        .sortedByDescending { it.startTime }

    val grouped = filtered
        .groupBy { it.startTime.toDayKey() }
        .entries
        .sortedByDescending { it.key }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onlyIncomplete = !onlyIncomplete }) {
                        Icon(
                            if (onlyIncomplete) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            "筛选未完成",
                            tint = if (onlyIncomplete) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("搜索备注 / 标签 / 事件名", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp)
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HFilterChip("全部", eventFilter == "all") { eventFilter = "all" }
                eventTypes.forEach { e ->
                    HFilterChip(e.name, eventFilter == e.id, parseHexColor(e.color)) { eventFilter = e.id }
                }
            }
            if (filtered.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("共 ${filtered.size} 条", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val totalSec = filtered.sumOf { it.durationSeconds ?: 0L }
                        if (totalSec > 0) Text("总时长 ${formatDuration(totalSec)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, null, Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        Text(if (searchQuery.isNotEmpty() || eventFilter != "all" || onlyIncomplete)
                            "没有匹配的记录" else "暂无记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (dayKey, daySessions) ->
                        item(key = "hdr-$dayKey") {
                            Row(
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(formatDayHeader(dayKey),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant)
                                Text("${daySessions.size} 条",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                        items(daySessions, key = { it.id }) { s ->
                            eventMap[s.eventId]?.let { event ->
                                SessionHistoryItem(
                                    session = s, event = event,
                                    color = parseHexColor(event.color),
                                    onDelete = { deleteTarget = s },
                                    onEdit = { editTarget = s }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = { Text("确认删除这条记录？此操作不可撤销。") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSession(s); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("取消") } }
        )
    }
    editTarget?.let { s ->
        EditSessionDialog(
            session = s, onDismiss = { editTarget = null },
            onSave = { updated -> viewModel.updateSession(updated); editTarget = null }
        )
    }
}

@Composable
private fun HFilterChip(label: String, selected: Boolean, color: Color? = null, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (color != null) ({
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }) else null)
}
