package com.betterfly.app.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.betterfly.app.util.parseHexColor
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

private val THEME_COLORS = listOf(
    "#4285F4", "#EA4335", "#FBBC05", "#34A853",
    "#8b5cf6", "#ec4899", "#6366f1", "#14b8a6",
    "#f43f5e", "#a855f7", "#06b6d4", "#84cc16"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val msg by viewModel.message.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    var showAuthDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.signInWithGoogleCredential(
                    GoogleAuthProvider.getCredential(account.idToken, null)
                )
            } catch (e: Exception) {
                viewModel.setMessage("Google 登录失败: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Account status banner
                Surface(shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (currentUser != null) {
                                    when {
                                        currentUser.isAnonymous -> "游客模式"
                                        else -> currentUser.email ?: currentUser.displayName ?: "已登录"
                                    }
                                } else "未登录",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (currentUser != null) "数据将同步至云端" else "登录后可跨设备同步",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        if (currentUser != null) {
                            TextButton(onClick = { viewModel.signOut() }) {
                                Text("退出", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                SettingsCard("外观") {
                    SettingsRow("深色模式") {
                        Switch(checked = settings.darkMode, onCheckedChange = { viewModel.updateDarkMode(it) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("主题色", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    ThemeColorPicker(settings.themeColor) { viewModel.updateThemeColor(it) }
                }
            }

            item {
                SettingsCard("偏好设置") {
                    SettingsRow("周起始日") {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1 to "周一", 0 to "周日").forEach { (v, label) ->
                                FilterChip(selected = settings.weekStart == v,
                                    onClick = { viewModel.updateWeekStart(v) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("停止方式", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("quick" to "快速", "note" to "备注", "interactive" to "详细").forEach { (mode, label) ->
                            FilterChip(selected = settings.stopMode == mode,
                                onClick = { viewModel.updateStopMode(mode) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Text(
                        when (settings.stopMode) {
                            "note" -> "停止时弹出备注输入框"
                            "interactive" -> "停止时填写备注、强度和完成状态"
                            else -> "点击停止按钮立即结束记录"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            item {
                SettingsCard("账号与同步") {
                    SettingsButton("邮箱登录 / 注册") { showAuthDialog = true }
                    SettingsButton("游客登录") { viewModel.signInAnonymously() }
                    SettingsButton("Google 登录") {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(com.betterfly.app.R.string.default_web_client_id))
                            .requestEmail().build()
                        googleLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    SettingsButton("立即双向同步") { viewModel.syncNow() }
                    SettingsButton("用本地覆盖云端") { viewModel.overwriteCloud() }
                    SettingsButton("去重会话") { viewModel.deduplicate() }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    SettingsButton("清空本地数据", isDestructive = true) { viewModel.clearLocalData() }
                    SettingsButton("退出登录", isDestructive = true) { viewModel.signOut() }
                }
            }

            item {
                SettingsCard("导入 / 导出") {
                    Text("支持 JSON 格式备份，导入会覆盖本地数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    SettingsButton("导出 JSON 到剪贴板") {
                        viewModel.exportBackupJson { json -> clipboard.setText(AnnotatedString(json)) }
                    }
                    SettingsButton("从 JSON 导入") { showImportDialog = true; importJson = "" }
                }
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(onDismissRequest = { showAuthDialog = false },
            title = { Text("邮箱登录 / 注册", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                        label = { Text("邮箱") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                        label = { Text("密码（至少6位）") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation())
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.signInWithEmailOrRegister(email.trim(), password); showAuthDialog = false },
                    enabled = email.isNotBlank() && password.length >= 6) { Text("继续") }
            },
            dismissButton = { TextButton({ showAuthDialog = false }) { Text("取消") } })
    }

    if (showImportDialog) {
        AlertDialog(onDismissRequest = { showImportDialog = false },
            title = { Text("从 JSON 导入", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("会覆盖当前本地数据，请先确认已备份。",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(importJson, { importJson = it }, Modifier.fillMaxWidth(),
                        label = { Text("粘贴 JSON") }, minLines = 4)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.importBackupJson(importJson); showImportDialog = false },
                    enabled = importJson.isNotBlank()) { Text("导入") }
            },
            dismissButton = { TextButton({ showImportDialog = false }) { Text("取消") } })
    }

    if (!msg.isNullOrBlank()) {
        LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); viewModel.clearMessage() }
        AlertDialog(onDismissRequest = { viewModel.clearMessage() },
            title = { Text("提示") }, text = { Text(msg ?: "") },
            confirmButton = { TextButton({ viewModel.clearMessage() }) { Text("知道了") } })
    }
}
