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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterfly.app.data.EventType
import com.betterfly.app.util.formatDurationShort
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun EventStatsCard(
    event: EventType,
    color: Color,
    weekCount: Int,
    weekDur: Long,
    monthCount: Int,
    monthDur: Long,
    overview: OverviewStats,
    heatmapData: Map<String, Int>,
    heatmapMetric: String,
    trend: List<Long>,
    rangeDays: Int,
    hourSpectrum: Map<Int, Long>
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        event.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (event.tags.isNotEmpty()) {
                        Text(
                            event.tags.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        null, Modifier.size(20.dp)
                    )
                }
            }

            // Period summary pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatPill("周次数", "${weekCount}次", color, Modifier.weight(1f))
                StatPill("周时长", formatDurationShort(weekDur), color, Modifier.weight(1f))
                StatPill("月次数", "${monthCount}次", color, Modifier.weight(1f))
                StatPill("月时长", formatDurationShort(monthDur), color, Modifier.weight(1f))
            }

            if (expanded) {
                // Overview stats
                SectionLabel("总览统计")
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    OverviewRow(
                        left = Pair("总次数", "${overview.totalCount}次"),
                        right = Pair("总时长", formatDurationShort(overview.totalDurationSec)),
                        color = color
                    )
                    OverviewRow(
                        left = Pair("最长", formatDurationShort(overview.maxDurationSec)),
                        right = Pair("最短", formatDurationShort(overview.minDurationSec)),
                        color = color
                    )
                    OverviewRow(
                        left = Pair("均值", formatDurationShort(overview.avgDurationSec.toLong())),
                        right = Pair("中位数", formatDurationShort(overview.medianDurationSec.toLong())),
                        color = color
                    )
                    OverviewRow(
                        left = Pair("P90", formatDurationShort(overview.p90DurationSec.toLong())),
                        right = Pair("最短间隔", formatDurationShort(overview.minGapSec)),
                        color = color
                    )
                }

                // Trend sparkline
                SectionLabel("近${rangeDays}天趋势")
                TrendSparkline(trend, color)

                // Hour spectrum
                SectionLabel("24h 时段分布")
                HourSpectrum(hourSpectrum, color)

                // Heatmap
                SectionLabel(if (heatmapMetric == "count") "近12周热力图（次数）" else "近12周热力图（时长）")
                Heatmap12Weeks(heatmapData, color)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun OverviewRow(
    left: Pair<String, String>,
    right: Pair<String, String>,
    color: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OverviewPill(left.first, left.second, color, Modifier.weight(1f))
        OverviewPill(right.first, right.second, color, Modifier.weight(1f))
    }
}

@Composable
private fun OverviewPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TrendSparkline(values: List<Long>, color: Color) {
    if (values.all { it == 0L }) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        return
    }
    val max = values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val min = values.minOrNull()?.toFloat() ?: 0f
    Card(
        colors = CardDefaults.cardColors(color.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Canvas(Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp, vertical = 10.dp)) {
            if (values.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val range = (max - min).coerceAtLeast(1f)

            // Fill area under line
            val fillPath = Path()
            values.forEachIndexed { i, v ->
                val x = i.toFloat() / (values.size - 1) * w
                val y = h - ((v - min) / range) * h
                if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
            }
            fillPath.lineTo(w, h)
            fillPath.lineTo(0f, h)
            fillPath.close()
            drawPath(fillPath, color = color.copy(alpha = 0.15f))

            // Line
            val linePath = Path()
            values.forEachIndexed { i, v ->
                val x = i.toFloat() / (values.size - 1) * w
                val y = h - ((v - min) / range) * h
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, color = color, style = Stroke(width = 2.5f))

            // Dots for non-zero
            values.forEachIndexed { i, v ->
                if (v > 0) {
                    val x = i.toFloat() / (values.size - 1) * w
                    val y = h - ((v - min) / range) * h
                    drawCircle(color, radius = 3f, center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun HourSpectrum(map: Map<Int, Long>, color: Color) {
    val max = (map.values.maxOrNull() ?: 1L).toFloat().coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (h in 0..23) {
                val v = map[h]?.toFloat() ?: 0f
                val alpha = if (v > 0) (v / max).coerceIn(0.15f, 1f) else 0.06f
                Box(
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = alpha))
                )
            }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun Heatmap12Weeks(data: Map<String, Int>, color: Color) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -83)
    val days = mutableListOf<String>()
    repeat(84) { days.add(sdf.format(cal.time)); cal.add(Calendar.DAY_OF_YEAR, 1) }
    val maxVal = (data.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Day-of-week labels
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            dayLabels.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        // Grid columns (weeks)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            for (col in 0 until 12) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (row in 0 until 7) {
                        val value = days.getOrNull(col * 7 + row)?.let { data[it] } ?: 0
                        val alpha = if (value <= 0) 0.07f
                                   else (value.toFloat() / maxVal).coerceIn(0.2f, 1f)
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}
 