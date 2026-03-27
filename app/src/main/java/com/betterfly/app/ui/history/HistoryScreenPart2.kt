package com.betterfly.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.toDisplayDate
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionHistoryItem(
    session: Session, event: EventType, color: Color,
    onDelete: () -> Unit, onEdit: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(4.dp).height(52.dp)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(event.name, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium)
                    if (session.incomplete) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)) {
                            Text("未完成", Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    session.rating?.let { r ->
                        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("强度 $r", Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
                Text(session.startTime.toDisplayDate(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!session.note.isNullOrBlank()) {
                    Text(session.note, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (session.tags.isNotEmpty()) {
                    Text(session.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.8f))
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(formatDuration(session.durationSeconds ?: 0L),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium, color = color)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "编辑", Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "删除", Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditSessionDialog(session: Session, onDismiss: () -> Unit, onSave: (Session) -> Unit) {
    var note by remember { mutableStateOf(session.note ?: "") }
    var rating by remember { mutableStateOf(session.rating ?: 0) }
    var incomplete by remember { mutableStateOf(session.incomplete) }
    var tags by remember { mutableStateOf(session.tags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(),
                    label = { Text("备注") }, singleLine = false, minLines = 2)
                Text("力竭程度 (1-5)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3, 4, 5).forEach { level ->
                        FilterChip(selected = rating == level,
                            onClick = { rating = if (rating == level) 0 else level },
                            label = { Text("$level") })
                    }
                }
                OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(),
                    label = { Text("标签（逗号分隔）") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("标记未完成")
                    Switch(checked = incomplete, onCheckedChange = { incomplete = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(session.copy(
                    note = note.ifBlank { null },
                    rating = rating.takeIf { it in 1..5 },
                    incomplete = incomplete,
                    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                ))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

fun formatDayHeader(dayKey: String): String = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val display = SimpleDateFormat("M月d日 EEE", Locale.CHINESE)
    val date = sdf.parse(dayKey) ?: return dayKey
    val todayStr = sdf.format(Calendar.getInstance().time)
    val yesterdayStr = sdf.format(
        Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -1) }.time
    )
    when (dayKey) { todayStr -> "今天"; yesterdayStr -> "昨天"; else -> display.format(date) }
} catch (e: Exception) { dayKey }
