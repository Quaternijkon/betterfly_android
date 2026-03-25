package com.betterfly.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.util.parseHexColor

private val colors = listOf("#4285F4", "#EA4335", "#FBBC05", "#34A853", "#8b5cf6", "#ec4899")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val msg by viewModel.message.collectAsState()
    val clipboard = LocalClipboardManager.current

    var showAuthDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("界面外观", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("深色模式")
                            Switch(checked = settings.darkMode, onCheckedChange = { viewModel.updateDarkMode(it) })
                        }
                        Text("主题色")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            colors.forEach { hex ->
                                val c = parseHexColor(hex)
                                val selected = settings.themeColor.equals(hex, ignoreCase = true)
                                IconButton(onClick = { viewModel.updateThemeColor(hex) }) {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .size(if (selected) 30.dp else 24.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("偏好", style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("周起始")
                            Row {
                                TextButton(onClick = { viewModel.updateWeekStart(1) }) { Text(if (settings.weekStart == 1) "周一 ✓" else "周一") }
                                TextButton(onClick = { viewModel.updateWeekStart(0) }) { Text(if (settings.weekStart == 0) "周日 ✓" else "周日") }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("停止模式")
                            Row {
                                TextButton(onClick = { viewModel.updateStopMode("quick") }) { Text(if (settings.stopMode == "quick") "快速 ✓" else "快速") }
                                TextButton(onClick = { viewModel.updateStopMode("note") }) { Text(if (settings.stopMode == "note") "备注 ✓" else "备注") }
                                TextButton(onClick = { viewModel.updateStopMode("interactive") }) { Text(if (settings.stopMode == "interactive") "详细 ✓" else "详细") }
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("账号与同步", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { showAuthDialog = true }) { Text("邮箱登录/注册") }
                        TextButton(onClick = { viewModel.signInAnonymously() }) { Text("游客登录") }
                        TextButton(onClick = { viewModel.signInWithGoogle() }) { Text("Google 登录") }
                        TextButton(onClick = { viewModel.signInWithGithub() }) { Text("GitHub 登录") }
                        TextButton(onClick = { viewModel.signInWithMicrosoft() }) { Text("Microsoft 登录") }
                        TextButton(onClick = { viewModel.syncNow() }) { Text("立即双向同步") }
                        TextButton(onClick = { viewModel.overwriteCloud() }) { Text("用本地覆盖云端") }
                        TextButton(onClick = { viewModel.deduplicate() }) { Text("去重会话") }
                        TextButton(onClick = { viewModel.clearLocalData() }) { Text("清空本地数据") }
                        TextButton(onClick = { viewModel.signOut() }) { Text("退出登录") }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("导入导出", style = MaterialTheme.typography.titleSmall)
                        Text("支持 JSON 导出到剪贴板，也可从 JSON 导入覆盖本地。", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            viewModel.exportBackupJson { json ->
                                clipboard.setText(AnnotatedString(json))
                            }
                        }) { Text("导出 JSON 到剪贴板") }
                        TextButton(onClick = {
                            showImportDialog = true
                            importJson = ""
                        }) { Text("从 JSON 导入") }
                    }
                }
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("邮箱登录 / 注册") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signInWithEmailOrRegister(email.trim(), password)
                    showAuthDialog = false
                }) { Text("继续") }
            },
            dismissButton = { TextButton(onClick = { showAuthDialog = false }) { Text("取消") } }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("从 JSON 导入") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("会覆盖当前本地数据，请先确认已备份。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        label = { Text("粘贴 JSON") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importBackupJson(importJson)
                    showImportDialog = false
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }

    if (!msg.isNullOrBlank()) {
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(1800)
            viewModel.clearMessage()
        }
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("提示") },
            text = { Text(msg ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.clearMessage() }) { Text("知道了") } }
        )
    }
}
