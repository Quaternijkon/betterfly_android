package com.betterfly.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDurationShort
import com.betterfly.app.util.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    var selectedEventId by remember { mutableStateOf("all") }
    var rangeDays by remember { mutableIntStateOf(30) }

    val filteredSessions = state.sessions
        .filter { it.endTime != null }
        .filter { if (selectedEventId == "all") true else it.eventId == selectedEventId }

    Scaffold(topBar = { TopAppBar(title = { Text("统计", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip("全部", selectedEventId == "all") { selectedEventId = "all" }
                    state.events.forEach { e ->
                        FilterChip(e.name, selectedEventId == e.id) { selectedEventId = e.id }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip("7天", rangeDays == 7) { rangeDays = 7 }
                    FilterChip("30天", rangeDays == 30) { rangeDays = 30 }
                    FilterChip("90天", rangeDays == 90) { rangeDays = 90 }
                }
            }

            items(state.events.filter { selectedEventId == "all" || it.id == selectedEventId }, key = { it.id }) { event ->
                val color = parseHexColor(event.color)
                val weekFrom = weeklyStart(state.weekStart)
                val monthFrom = monthlyStart()
                val weekCount = filteredSessions.countFrom(event.id, weekFrom)
                val weekDur = filteredSessions.durationFrom(event.id, weekFrom)
                val monthCount = filteredSessions.countFrom(event.id, monthFrom)
                val monthDur = filteredSessions.durationFrom(event.id, monthFrom)
                val heatmap = filteredSessions.dailyCountMap(event.id)
                val trend = dailyDurationSeries(filteredSessions, event.id, rangeDays)
                val hourSpectrum = hourlyDurationMap(filteredSessions, event.id)

                EventStatsCard(
                    event = event,
                    color = color,
                    weekCount = weekCount,
                    weekDur = weekDur,
                    monthCount = monthCount,
                    monthDur = monthDur,
                    heatmap = heatmap,
                    trend = trend,
                    hourSpectrum = hourSpectrum
                )
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (selected) "$label ✓" else label)
    }
}

@Composable
private fun EventStatsCard(
    event: EventType,
    color: Color,
    weekCount: Int,
    weekDur: Long,
    monthCount: Int,
    monthDur: Long,
    heatmap: Map<String, Int>,
    trend: List<Long>,
    hourSpectrum: Map<Int, Long>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(Modifier.size(8.dp))
                Text(event.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (event.tags.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(event.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = color)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("周次数", "$weekCount", color, Modifier.weight(1f))
                StatPill("周时长", formatDurationShort(weekDur), color, Modifier.weight(1f))
                StatPill("月次数", "$monthCount", color, Modifier.weight(1f))
                StatPill("月时长", formatDurationShort(monthDur), color, Modifier.weight(1f))
            }

            Text("趋势（按天时长）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TrendSparkline(trend, color)

            Text("24h 光谱（按开始小时）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HourSpectrum(hourSpectrum, color)

            Text("近12周热力", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Heatmap12Weeks(heatmap, color)
        }
    }
}

@Composable
private fun TrendSparkline(values: List<Long>, color: Color) {
    val max = (values.maxOrNull() ?: 1L).toFloat().coerceAtLeast(1f)
    val min = (values.minOrNull() ?: 0L).toFloat()
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Canvas(modifier = Modifier.fillMaxWidth().height(70.dp).padding(8.dp)) {
            if (values.isEmpty()) return@Canvas
            val path = Path()
            val w = size.width
            val h = size.height
            values.forEachIndexed { i, v ->
                val x = if (values.size == 1) 0f else i.toFloat() / (values.size - 1) * w
                val y = h - ((v - min) / (max - min + 0.0001f)) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun HourSpectrum(map: Map<Int, Long>, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        val max = (map.values.maxOrNull() ?: 1L).toFloat().coerceAtLeast(1f)
        for (h in 0..23) {
            val v = map[h]?.toFloat() ?: 0f
            val alpha = (v / max).coerceIn(0.08f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun Heatmap12Weeks(data: Map<String, Int>, color: Color) {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -83)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val days = mutableListOf<String>()
    repeat(84) {
        days.add(sdf.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until 7) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 12) {
                    val idx = col * 7 + row
                    val key = days.getOrNull(idx)
                    val value = key?.let { data[it] } ?: 0
                    val alpha = when {
                        value <= 0 -> 0.08f
                        value == 1 -> 0.35f
                        value <= 3 -> 0.6f
                        else -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun dailyDurationSeries(sessions: List<Session>, eventId: String, days: Int): List<Long> {
    val end = System.currentTimeMillis()
    val start = end - days * 24L * 3600_000L
    val buckets = LongArray(days)
    sessions.filter { it.eventId == eventId && it.endTime != null && it.startTime in start..end }.forEach { s ->
        val idx = ((s.startTime - start) / (24L * 3600_000L)).toInt().coerceIn(0, days - 1)
        buckets[idx] += (s.durationSeconds ?: 0L)
    }
    return buckets.toList()
}

private fun hourlyDurationMap(sessions: List<Session>, eventId: String): Map<Int, Long> {
    val cal = Calendar.getInstance()
    val out = mutableMapOf<Int, Long>()
    sessions.filter { it.eventId == eventId && it.endTime != null }.forEach { s ->
        cal.timeInMillis = s.startTime
        val h = cal.get(Calendar.HOUR_OF_DAY)
        out[h] = (out[h] ?: 0L) + (s.durationSeconds ?: 0L)
    }
    return out
}
