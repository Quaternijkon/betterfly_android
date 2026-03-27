package com.betterfly.app.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.betterfly.app.util.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedEventId by remember { mutableStateOf("all") }
    var rangeDays by remember { mutableIntStateOf(30) }
    var heatmapMetric by remember { mutableStateOf("count") }

    val completedSessions = state.sessions.filter { it.endTime != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据分析", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (state.events.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BarChart, null,
                        Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "暂无数据",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "先在首页创建事件并开始记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Event filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatsFilterChip("全部", selectedEventId == "all") { selectedEventId = "all" }
                    state.events.forEach { e ->
                        StatsFilterChip(
                            e.name, selectedEventId == e.id,
                            parseHexColor(e.color)
                        ) { selectedEventId = e.id }
                    }
                }
            }

            // Range filter
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(7 to "7天", 30 to "30天", 90 to "90天", 365 to "1年").forEach { (days, label) ->
                        StatsFilterChip(label, rangeDays == days) { rangeDays = days }
                    }
                }
            }

            // Heatmap metric toggle
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatsFilterChip("热力: 次数", heatmapMetric == "count") { heatmapMetric = "count" }
                    StatsFilterChip("热力: 时长", heatmapMetric == "duration") { heatmapMetric = "duration" }
                }
            }

            val displayEvents = if (selectedEventId == "all") state.events
                                else state.events.filter { it.id == selectedEventId }

            items(displayEvents, key = { it.id }) { event ->
                val color = parseHexColor(event.color)
                val weekFrom = weeklyStart(state.weekStart)
                val monthFrom = monthlyStart()
                val evSessions = completedSessions.filter { it.eventId == event.id }
                val overview = evSessions.overviewStats(event.id)
                val heatmapData = if (heatmapMetric == "duration")
                    evSessions.dailyDurationMap(event.id).mapValues { it.value.toInt() }
                else
                    evSessions.dailyCountMap(event.id)
                val trend = completedSessions.dailyDurationSeries(event.id, rangeDays)
                val hourSpectrum = completedSessions.hourlyDurationMap(event.id)

                EventStatsCard(
                    event = event,
                    color = color,
                    weekCount = completedSessions.countFrom(event.id, weekFrom),
                    weekDur = completedSessions.durationFrom(event.id, weekFrom),
                    monthCount = completedSessions.countFrom(event.id, monthFrom),
                    monthDur = completedSessions.durationFrom(event.id, monthFrom),
                    overview = overview,
                    heatmapData = heatmapData,
                    heatmapMetric = heatmapMetric,
                    trend = trend,
                    rangeDays = rangeDays,
                    hourSpectrum = hourSpectrum
                )
            }
        }
    }
}

@Composable
internal fun StatsFilterChip(
    label: String,
    selected: Boolean,
    color: Color? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (color != null) ({
            Box(
                Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color)
            )
        }) else null
    )
}
