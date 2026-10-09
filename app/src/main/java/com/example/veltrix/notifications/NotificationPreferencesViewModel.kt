package com.example.veltrix.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class NotificationPreferencesViewModel(
    private val repository: NotificationPreferencesRepository = NotificationPreferencesRepository(),
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Ready(val prefs: NotificationPreferences) : UiState()
        data class Error(val message: String, val lastKnown: NotificationPreferences?) : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { repository.fetchPreferences() }
                .onSuccess {
                    NotificationPrefsCache.update(it)
                    _state.value = UiState.Ready(it)
                }
                .onFailure { e ->
                    val fallback = NotificationPrefsCache.prefs
                    NotificationPrefsCache.update(fallback)
                    _state.value = UiState.Ready(fallback)
                    _snackbar.value = e.message ?: "Could not sync notification settings"
                }
        }
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    private fun currentPrefs(): NotificationPreferences? = when (val s = _state.value) {
        is UiState.Ready -> s.prefs
        is UiState.Error -> s.lastKnown
        else -> null
    }

    fun setPushEnabled(enabled: Boolean, onRequestPermission: () -> Unit = {}) {
        if (enabled) onRequestPermission()
        updateChannels { it.copy(pushEnabled = enabled) }
    }

    fun setEmailEnabled(enabled: Boolean) {
        updateChannels { it.copy(emailEnabled = enabled) }
    }

    fun setInAppEnabled(enabled: Boolean) {
        updateChannels { it.copy(inAppEnabled = enabled) }
    }

    fun setCategory(
        key: String,
        enabled: Boolean,
        onSecurityDisabled: () -> Unit = {},
    ) {
        if (key == "security_alerts" && !enabled) {
            onSecurityDisabled()
        }
        val prefs = currentPrefs() ?: return
        val categories = prefs.categories
        val updated = when (key) {
            "chat_updates" -> categories.copy(chatUpdates = enabled)
            "plan_updates" -> categories.copy(planUpdates = enabled)
            "promotional" -> categories.copy(promotional = enabled)
            "usage_alerts" -> categories.copy(usageAlerts = enabled)
            "security_alerts" -> categories.copy(securityAlerts = enabled)
            "offline_downloads" -> categories.copy(offlineDownloads = enabled)
            else -> return
        }
        optimisticUpdate(prefs.copy(categories = updated)) {
            JSONObject().put(
                "categories",
                JSONObject()
                    .put("chat_updates", updated.chatUpdates)
                    .put("plan_updates", updated.planUpdates)
                    .put("promotional", updated.promotional)
                    .put("usage_alerts", updated.usageAlerts)
                    .put("security_alerts", updated.securityAlerts)
                    .put("offline_downloads", updated.offlineDownloads),
            )
        }
    }

    private fun updateChannels(transform: (NotificationChannelSettings) -> NotificationChannelSettings) {
        val prefs = currentPrefs() ?: return
        val channels = transform(prefs.channels)
        optimisticUpdate(prefs.copy(channels = channels)) {
            JSONObject().put(
                "channels",
                JSONObject()
                    .put("pushEnabled", channels.pushEnabled)
                    .put("emailEnabled", channels.emailEnabled)
                    .put("inAppEnabled", channels.inAppEnabled),
            )
        }
    }

    private fun optimisticUpdate(
        optimistic: NotificationPreferences,
        patch: () -> JSONObject,
    ) {
        val previous = currentPrefs()
        NotificationPrefsCache.update(optimistic)
        _state.value = UiState.Ready(optimistic)
        viewModelScope.launch {
            runCatching { repository.updatePreferences(patch()) }
                .onSuccess {
                    NotificationPrefsCache.update(it)
                    _state.value = UiState.Ready(it)
                }
                .onFailure { e ->
                    if (previous != null) {
                        _state.value = UiState.Ready(previous)
                    }
                    _snackbar.value = e.message ?: "Update failed"
                }
        }
    }
}
