package com.betterfly.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.data.UserSettings
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.formatDurationShort
import com.betterfly.app.util.parseHexColor

@Composable
internal fun EventCard(
    event: EventType,
    activeSession: Session?,
    sessions: List<Session>,
    now: Long,
    settings: UserSettings,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onStop: (Session) -> Unit
) {
    val color = parseHexColor(event.color)
    val isActive = activeSession != null
    val elapsed = activeSession?.let { (now - it.startTime) / 1000L } ?: 0L

    val goalProgress = event.goal?.let { goal ->
        val periodStart = getPeriodStart(goal.period, settings.weekStart)
        val periodSessions = sessions.filter { it.startTime >= periodStart }
        val current = when (goal.metric) {
            "count" -> periodSessions.size.toDouble()
            else -> periodSessions.sumOf { it.durationSeconds ?: 0L }.toDouble() / 3600.0
        }
        GoalProgress(current, goal.targetValue, goal.type, goal.metric)
    }

    val lastSession = sessions.maxByOrNull { it.startTime }
    val lastText = lastSession?.let {
        val ago = (now - it.startTime) / 1000L
        when {
            ago < 60 -> "刚刚"
            ago < 3600 -> "${ago / 60}分钟前"
            ago < 86400 -> "${ago / 3600}小时前"
            else -> "${ago / 86400}天前"
        }
    }
    val weekStart = getPeriodStart("week", settings.weekStart)
    val weekCount = sessions.count { it.startTime >= weekStart }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isActive) 6.dp else 2.dp),
        border = if (isActive) BorderStroke(1.5.dp, color.copy(alpha = 0.5f)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            EventCardHeader(event, isActive, elapsed, lastText, color, activeSession, onEdit, onStart, onStop)
            if (!isActive && sessions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniStatChip("共", "${sessions.size}次", color, Modifier.weight(1f))
                    MiniStatChip("本周", "${weekCount}次", color, Modifier.weight(1f))
                    lastSession?.durationSeconds?.let { dur ->
                        MiniStatChip("上次", formatDurationShort(dur), color, Modifier.weight(1f))
                    }
                }
            }
            goalProgress?.let { gp ->
                Spacer(Modifier.height(10.dp))
                GoalProgressBar(gp, color)
            }
        }
    }
}

@Composable
private fun EventCardHeader(
    event: EventType,
    isActive: Boolean,
    elapsed: Long,
    lastText: String?,
    color: Color,
    activeSession: Session?,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onStop: (Session) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 2.dp).size(44.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                val t = rememberInfiniteTransition(label = "pulse")
                val scale by t.animateFloat(
                    0.7f, 1.3f,
                    infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "s"
                )
                Box(Modifier.size((12 * scale).dp).clip(CircleShape).background(color))
            } else {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(event.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isActive) {
                Text(formatDuration(elapsed), color = color,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                Text("进行中…", style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f))
            } else {
                Text(lastText?.let { "上次: $it" } ?: "点击开始记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.tags.isNotEmpty()) {
                    Text(event.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "编辑", Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(
                onClick = { if (isActive) onStop(activeSession!!) else onStart() },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)
            ) {
                Icon(
                    if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    if (isActive) "停止" else "开始",
                    modifier = Modifier.size(22.dp), tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun GoalProgressBar(gp: GoalProgress, color: Color) {
    val raw = (gp.current / gp.target).toFloat()
    val progress = raw.coerceIn(0f, 1f)
    val met = if (gp.type == "positive") gp.current >= gp.target else gp.current <= gp.target
    val pc = when {
        met -> Color(0xFF34A853)
        gp.type == "negative" && raw > 0.8f -> Color(0xFFEA4335)
        else -> color
    }
    val label = when (gp.metric) {
        "count" -> "${gp.current.toInt()} / ${gp.target.toInt()} 次"
        else -> "${"%,.1f".format(gp.current)} / ${gp.target} h"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (gp.type == "positive") "目标: $label" else "限制: $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (met) "✓ 达成" else "${(raw * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = pc, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = pc, trackColor = pc.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun MiniStatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.08f)) {
        Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.labelMedium,
                color = color, fontWeight = FontWeight.Bold)
        }
    }
}
