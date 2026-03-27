package com.betterfly.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.betterfly.app.data.Goal
import com.betterfly.app.util.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PRESET_COLORS.first()) }
    var tagsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建事件", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("事件名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("主题色", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                ColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                OutlinedTextField(
                    value = tagsText, onValueChange = { tagsText = it },
                    label = { Text("标签（逗号分隔，可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onConfirm(name.trim(), selectedColor, tags)
                },
                enabled = name.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    val rows = PRESET_COLORS.chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hex ->
                    val c = parseHexColor(hex)
                    val selected = hex.equals(selectedColor, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selected) Modifier.border(2.5.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorSelected(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check, null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditEventDialog(
    event: EventType,
    onDismiss: () -> Unit,
    onSave: (EventType, Goal?, List<String>) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(event.name) }
    var selectedColor by remember { mutableStateOf(event.color) }
    var tagsText by remember { mutableStateOf(event.tags.joinToString(", ")) }
    var hasGoal by remember { mutableStateOf(event.goal != null) }
    var goalType by remember { mutableStateOf(event.goal?.type ?: "positive") }
    var goalMetric by remember { mutableStateOf(event.goal?.metric ?: "count") }
    var goalPeriod by remember { mutableStateOf(event.goal?.period ?: "week") }
    var goalTargetText by remember { mutableStateOf(event.goal?.targetValue?.toString() ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑事件", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("事件名称") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text("主题色", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                ColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                OutlinedTextField(
                    value = tagsText, onValueChange = { tagsText = it },
                    label = { Text("标签（逗号分隔，可选）") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("设定目标", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("追踪周期内的完成情况", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hasGoal, onCheckedChange = { hasGoal = it })
                }
                if (hasGoal) {
                    Text("目标类型", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = goalType == "positive",
                            onClick = { goalType = "positive" },
                            label = { Text("正向（越多越好）") }
                        )
                        FilterChip(
                            selected = goalType == "negative",
                            onClick = { goalType = "negative" },
                            label = { Text("反向（越少越好）") }
                        )
                    }
                    Text("统计方式", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = goalMetric == "count",
                            onClick = { goalMetric = "count" },
                            label = { Text("次数") }
                        )
                        FilterChip(
                            selected = goalMetric == "duration",
                            onClick = { goalMetric = "duration" },
                            label = { Text("时长（小时）") }
                        )
                    }
                    Text("周期", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = goalPeriod == "week",
                            onClick = { goalPeriod = "week" },
                            label = { Text("每周") }
                        )
                        FilterChip(
                            selected = goalPeriod == "month",
                            onClick = { goalPeriod = "month" },
                            label = { Text("每月") }
                        )
                    }
                    OutlinedTextField(
                        value = goalTargetText,
                        onValueChange = { goalTargetText = it },
                        label = { Text(if (goalMetric == "count") "目标次数" else "目标小时数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onArchive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (event.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                            null, Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (event.archived) "取消归档" else "归档")
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = if (hasGoal) {
                        goalTargetText.toDoubleOrNull()?.let {
                            Goal(type = goalType, metric = goalMetric, period = goalPeriod, targetValue = it)
                        }
                    } else null
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(event.copy(name = name.trim(), color = selectedColor), goal, tags)
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除事件") },
            text = { Text("确认删除事件「${event.name}」及其所有记录？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton({ showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}
