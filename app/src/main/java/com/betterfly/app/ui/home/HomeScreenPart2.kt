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
import com.betterfly.app.ui.theme.LocalThemeColor
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.parseHexColor
import java.util.*

internal fun getPeriodStart(period: String, weekStart: Int): Long {
    val cal = Calendar.getInstance()
    when (period) {
        "week" -> {
            val firstDay = if (weekStart == 1) Calendar.MONDAY else Calendar.SUNDAY
            while (cal.get(Calendar.DAY_OF_WEEK) != firstDay) cal.add(Calendar.DAY_OF_YEAR, -1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        }
        else -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        }
    }
    return cal.timeInMillis
}

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
            else -> periodSessions.sumOf { (it.durationSeconds ?: 0L) }.toDouble() / 3600.0
        }
        GoalProgress(current = current, target = goal.targetValue, type = goal.type, metric = goal.metric)
    }

    val lastSession = sessions.maxByOrNull { it.startTime }
    val lastText = lastSession?.let {
        val ago = (now - it.startTime) / 1000L
        when {
            ago < 3600 -> "${ago / 60}分钟前"
            ago < 86400 -> "${ago / 3600}小时前"
            else -> "${ago / 86400}天前"
        }
    }

    // Streak / gap calculation
    val completedSessions = sessions.filter { it.endTime != null && !it.incomplete }
    val uniqueDays = completedSessions.map {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.startTime))
    }.toSortedSet().toList()
    val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now))
    val yesterdayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(now - 86400_000L))
    val currentStreak = if (uniqueDays.isEmpty()) 0 else {
        val last = uniqueDays.last()
        if (last == todayKey || last == yesterdayKey) {
            var s = 1; var check = java.util.Date(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(last)!!.time - 86400_000L)
            val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            while (uniqueDays.contains(sdf2.format(check))) { s++; check = java.util.Date(check.time - 86400_000L) }
            s
        } else 0
    }
    val currentGap = if (uniqueDays.isEmpty() || currentStreak > 0) 0 else {
        val sdf3 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val lastDate = sdf3.parse(uniqueDays.last())?.time ?: now
        ((now - lastDate) / 86400_000L).toInt()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp),
        border = if (isActive) BorderStroke(1.5.dp, color.copy(alpha = 0.4f)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        val transition = rememberInfiniteTransition(label = "pulse")
                        val scale by transition.animateFloat(
                            initialValue = 1f, targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                            label = "s"
                        )
                        Box(Modifier.size((10 * scale).dp).clip(CircleShape).background(color))
                    } else {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            event.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (event.archived) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "已归档",
                                    Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (isActive) {
                        Text(
                            formatDuration(elapsed),
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            lastText?.let { "上次: $it" } ?: "点击开始记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (event.tags.isNotEmpty()) {
                        Text(
                            event.tags.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.8f)
                        )
                    }
                    if (!isActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentStreak > 0) {
                                Text(
                                    "连胜 ${currentStreak}天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (currentGap > 0) {
                                Text(
                                    "未做 ${currentGap}天",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentGap > 3) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(
                    onClick = { if (isActive) onStop(activeSession!!) else onStart() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)
                ) {
                    Icon(
                        if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        if (isActive) "停止" else "开始",
                        tint = Color.White
                    )
                }
            }

            goalProgress?.let { gp ->
                Spacer(Modifier.height(10.dp))
                val rawProgress = (gp.current / gp.target).toFloat()
                val progress = rawProgress.coerceIn(0f, 1f)
                val isGoalMet = if (gp.type == "positive") gp.current >= gp.target
                                else gp.current <= gp.target
                val progressColor = when {
                    isGoalMet -> Color(0xFF34A853)
                    gp.type == "negative" && rawProgress > 0.8f -> Color(0xFFEA4335)
                    else -> color
                }
                val label = when (gp.metric) {
                    "count" -> "${gp.current.toInt()} / ${gp.target.toInt()} 次"
                    else -> "${"%.1f".format(gp.current)} / ${gp.target} h"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (gp.type == "positive") "目标" else "限制",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = progressColor.copy(alpha = 0.15f)
                    )
                    Text(
                        label + if (isGoalMet) " ✓" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
