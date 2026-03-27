package com.betterfly.app.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterfly.app.data.EventType
import com.betterfly.app.data.Session
import com.betterfly.app.util.formatDuration
import com.betterfly.app.util.formatDurationShort

enum class CdwiMetric(val label: String, val color: Color) {
    DURATION("持续", Color(0xFF34A853)),
    WAIT("等待", Color(0xFFEA4335)),
    INTERVAL("间隔", Color(0xFF4285F4)),
    CYCLE("周期", Color(0xFFFBBC05))
}

@Composable
internal fun EventStatsCard(
    event: EventType, color: Color,
    weekCount: Int, weekDur: Long, monthCount: Int, monthDur: Long,
    overview: OverviewStats, heatmapData: Map<String, Int>,
    heatmapMetric: String, trend: List<Long>, rangeDays: Int,
    hourSpectrum: Map<Int, Long>,
    allSessions: List<Session>
) {
    var expanded by remember { mutableStateOf(true) }
    var cdwiMetric by remember { mutableStateOf(CdwiMetric.DURATION) }
    val cdwiSeries = remember(allSessions, event.id, cdwiMetric) {
        when (cdwiMetric) {
            CdwiMetric.DURATION -> allSessions.buildDurationSeries(event.id)
            CdwiMetric.WAIT -> allSessions.buildWaitSeries(event.id)
            CdwiMetric.INTERVAL -> allSessions.buildIntervalSeries(event.id)
            CdwiMetric.CYCLE -> allSessions.buildCycleSeries(event.id)
        }
    }
    val cdwiSummary = remember(cdwiSeries) { statSummary(cdwiSeries) }

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Spacer(Modifier.width(8.dp))
                Text(event.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (event.tags.isNotEmpty()) {
                    Text(event.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = color)
                }
                IconButton({ expanded = !expanded }, Modifier.size(28.dp)) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StreakPill("连胜", "${overview.currentStreak}天", color, Modifier.weight(1f))
                StreakPill("未做", "${overview.currentGap}天",
                    if (overview.currentGap > 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    Modifier.weight(1f))
                StatPill("周次", "$weekCount", color, Modifier.weight(1f))
                StatPill("月次", "$monthCount", color, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatPill("周时长", formatDurationShort(weekDur), color, Modifier.weight(1f))
                StatPill("月时长", formatDurationShort(monthDur), color, Modifier.weight(1f))
                StatPill("最大连胜", "${overview.maxStreak}天", color.copy(alpha = 0.7f), Modifier.weight(1f))
                StatPill("总次数", "${overview.totalCount}", color.copy(alpha = 0.7f), Modifier.weight(1f))
            }
            if (expanded) {
                SectionLabel("数据分析")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CdwiMetric.entries.forEach { m ->
                        val sel = cdwiMetric == m
                        Surface(onClick = { cdwiMetric = m }, shape = RoundedCornerShape(6.dp),
                            color = if (sel) m.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant) {
                            Text(m.label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) m.color else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (cdwiSummary != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CdwiPill("min", formatDuration(cdwiSummary.min), cdwiMetric.color, Modifier.weight(1f))
                        CdwiPill("max", formatDuration(cdwiSummary.max), cdwiMetric.color, Modifier.weight(1f))
                        CdwiPill("avg", formatDuration(cdwiSummary.avg.toLong()), cdwiMetric.color, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CdwiPill("p50", formatDuration(cdwiSummary.p50.toLong()), cdwiMetric.color, Modifier.weight(1f))
                        CdwiPill("p99", formatDuration(cdwiSummary.p99), cdwiMetric.color, Modifier.weight(1f))
                        CdwiPill("n", "${cdwiSeries.size}", cdwiMetric.color, Modifier.weight(1f))
                    }
                } else {
                    Text("暂无数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (cdwiSeries.size >= 2) {
                    SectionLabel("趋势序列")
                    CdwiLineChart(cdwiSeries, cdwiMetric.color)
                }
                SectionLabel("趋势（近${rangeDays}天时长/天）")
                TrendSparkline(trend, color)
                SectionLabel("24h 分布（按开始小时）")
                HourSpectrum(hourSpectrum, color)
                SectionLabel(if (heatmapMetric == "count") "近12周热力（次数）" else "近12周热力（时长）")
                Heatmap12Weeks(heatmapData, color)
                SectionLabel("总览统计")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverviewPill("总时长", formatDurationShort(overview.totalDurationSec), color, Modifier.weight(1f))
                        OverviewPill("均值", formatDurationShort(overview.avgDurationSec.toLong()), color, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverviewPill("最长", formatDurationShort(overview.maxDurationSec), color, Modifier.weight(1f))
                        OverviewPill("最短", formatDurationShort(overview.minDurationSec), color, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverviewPill("中位数", formatDurationShort(overview.medianDurationSec.toLong()), color, Modifier.weight(1f))
                        OverviewPill("P90", formatDurationShort(overview.p90DurationSec.toLong()), color, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OverviewPill("最短间隔", formatDurationShort(overview.minGapSec), color, Modifier.weight(1f))
                        OverviewPill("平均间隔", formatDurationShort(overview.avgGapSec.toLong()), color, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
}

@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f))
        .padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StreakPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.10f))
        .padding(vertical = 6.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun CdwiPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 8.dp, vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OverviewPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 10.dp, vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun CdwiLineChart(values: List<Long>, color: Color) {
    Card(colors = CardDefaults.cardColors(color.copy(alpha = 0.07f)), shape = RoundedCornerShape(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(80.dp).padding(8.dp)) {
            if (values.size < 2) return@Canvas
            val w = size.width; val h = size.height
            val min = values.minOrNull()?.toFloat() ?: 0f
            val max = values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i.toFloat() / (values.size - 1) * w
                val y = h - ((v - min) / (max - min + 0.001f)) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 2f))
            values.forEachIndexed { i, v ->
                val x = i.toFloat() / (values.size - 1) * w
                val y = h - ((v - min) / (max - min + 0.001f)) * h
                drawCircle(color = color, radius = 3f, center = Offset(x, y))
            }
        }
    }
}
