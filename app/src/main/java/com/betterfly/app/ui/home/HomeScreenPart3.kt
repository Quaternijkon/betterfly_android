package com.betterfly.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.betterfly.app.data.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteStopDialog(onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("结束记录", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false, minLines = 2
            )
        },
        confirmButton = { Button(onClick = { onSave(note.ifBlank { null }) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InteractiveStopDialog(
    session: Session,
    onDismiss: () -> Unit,
    onQuickStop: () -> Unit,
    onSave: (String?, Int?, Boolean) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var incomplete by remember { mutableStateOf(false) }
    val ratingLabels = listOf("轻松", "有点", "吃力", "接近", "力竭")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("结束记录", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false, minLines = 2
                )
                Text("力竭程度", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("轻松", "有点", "吃力", "接近", "力竭").forEachIndexed { idx, label ->
                        val level = idx + 1
                        val selected = rating == level
                        FilterChip(
                            selected = selected,
                            onClick = { rating = if (selected) 0 else level },
                            label = { Text("$level $label", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("标记未完成", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = incomplete, onCheckedChange = { incomplete = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(note.ifBlank { null }, rating.takeIf { it in 1..5 }, incomplete) }) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onQuickStop) { Text("快速停止") } }
    )
}
