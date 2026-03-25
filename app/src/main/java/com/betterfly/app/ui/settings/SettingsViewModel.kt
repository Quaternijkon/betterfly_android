package com.betterfly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betterfly.app.data.BackupPayload
import com.betterfly.app.data.UserSettings
import com.betterfly.app.data.repository.BetterFlyRepository
import com.betterfly.app.data.toWire
import com.betterfly.app.data.toDomain
import com.betterfly.app.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val repo: BetterFlyRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val currentUserEmail: StateFlow<String?> = MutableStateFlow(repo.getCurrentUser()?.email)

    fun clearMessage() { _message.value = null }

    fun updateThemeColor(color: String) {
        viewModelScope.launch {
            val next = settings.value.copy(themeColor = color)
            settingsStore.save(next)
            repo.saveSettings(next)
        }
    }

    fun updateDarkMode(dark: Boolean) {
        viewModelScope.launch {
            val next = settings.value.copy(darkMode = dark)
            settingsStore.save(next)
            repo.saveSettings(next)
        }
    }

    fun updateWeekStart(v: Int) {
        viewModelScope.launch {
            val next = settings.value.copy(weekStart = v)
            settingsStore.save(next)
            repo.saveSettings(next)
        }
    }

    fun updateStopMode(mode: String) {
        viewModelScope.launch {
            val next = settings.value.copy(stopMode = mode)
            settingsStore.save(next)
            repo.saveSettings(next)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val ok = repo.syncFromCloud()
            _message.value = if (ok) "同步完成" else "同步失败（请先登录并联网）"
        }
    }

    fun deduplicate() {
        viewModelScope.launch {
            val removed = repo.deduplicateLocalSessions()
            _message.value = "已去除重复记录 $removed 条"
        }
    }

    fun overwriteCloud() {
        viewModelScope.launch {
            val ok = repo.overwriteCloudWithLocal()
            _message.value = if (ok) "已用本地数据覆盖云端" else "覆盖失败（请先登录并联网）"
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            runCatching { repo.signInAnonymously() }
                .onSuccess { _message.value = "已游客登录" }
                .onFailure { _message.value = "游客登录失败：${it.message}" }
        }
    }

    fun signInWithEmailOrRegister(email: String, password: String) {
        viewModelScope.launch {
            runCatching { repo.signInWithEmailOrRegister(email, password) }
                .onSuccess { _message.value = "登录/注册成功" }
                .onFailure { _message.value = "登录失败：${it.message}" }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            runCatching { repo.signInWithGoogle() }
                .onSuccess { _message.value = "Google 登录成功" }
                .onFailure { _message.value = "Google 登录待接入（需要 ActivityResult + provider 配置）" }
        }
    }

    fun signInWithGithub() {
        viewModelScope.launch {
            runCatching { repo.signInWithGithub() }
                .onSuccess { _message.value = "GitHub 登录成功" }
                .onFailure { _message.value = "GitHub 登录待接入（需要 OAuth provider 流程）" }
        }
    }

    fun signInWithMicrosoft() {
        viewModelScope.launch {
            runCatching { repo.signInWithMicrosoft() }
                .onSuccess { _message.value = "Microsoft 登录成功" }
                .onFailure { _message.value = "Microsoft 登录待接入（需要 OAuth provider 流程）" }
        }
    }

    fun signOut() {
        repo.signOut()
        _message.value = "已退出登录"
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repo.clearLocalData()
            _message.value = "本地数据已清空"
        }
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val payload = BackupPayload(
                events = repo.getAllEventsOnce().map { it.toWire() },
                sessions = repo.getAllSessionsOnce().map { it.toWire() },
                settings = settings.value.toWire()
            )
            onReady(Json { encodeDefaults = true }.encodeToString(payload))
        }
    }

    fun importBackupJson(raw: String) {
        viewModelScope.launch {
            runCatching {
                val payload = Json { ignoreUnknownKeys = true }.decodeFromString(BackupPayload.serializer(), raw)
                repo.clearLocalData()
                payload.events.forEach { repo.saveEventType(it.toDomain()) }
                payload.sessions.forEach { repo.saveSession(it.toDomain()) }
                repo.saveSettings(payload.settings.toDomain())
            }.onSuccess {
                _message.value = "导入成功"
            }.onFailure {
                _message.value = "导入失败：JSON 格式不正确或数据无效"
            }
        }
    }
}
