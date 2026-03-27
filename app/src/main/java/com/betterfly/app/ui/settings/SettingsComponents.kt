package com.betterfly.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.betterfly.app.util.parseHexColor

@Composable
internal fun ThemeColorPicker(currentColor: String, onSelect: (String) -> Unit) {
    val colors = listOf(
        "#4285F4", "#EA4335", "#FBBC05", "#34A853",
        "#8b5cf6", "#ec4899", "#6366f1", "#14b8a6",
        "#f43f5e", "#a855f7", "#06b6d4", "#84cc16"
    )
    val rows = colors.chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hex ->
                    val c = parseHexColor(hex)
                    val selected = hex.equals(currentColor, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                            .clickable { onSelect(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(
                            Icons.Default.Check, null,
                            Modifier.size(14.dp), tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
internal fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

@Composable
internal fun SettingsButton(
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isDestructive) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
        )
    ) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}
