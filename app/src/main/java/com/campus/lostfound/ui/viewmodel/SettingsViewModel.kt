package com.campus.lostfound.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.SettingsRepository
import com.campus.lostfound.ui.theme.ThemeColor
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val themeMode: String = "system", // "system", "light", "dark"
    val themeColor: ThemeColor = ThemeColor.TEAL
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application.applicationContext)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.notificationsEnabledFlow,
        repository.soundEnabledFlow,
        repository.themeModeFlow,
        repository.themeColorFlow
    ) { notifications, sound, theme, colorName ->
        val themeColor = try {
            ThemeColor.valueOf(colorName)
        } catch (e: Exception) {
            ThemeColor.TEAL
        }
        SettingsUiState(
            notificationsEnabled = notifications,
            soundEnabled = sound,
            themeMode = theme,
            themeColor = themeColor
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)

            // Manage FCM topic subscription for app-level broadcasts
            try {
                if (enabled) {
                    FirebaseMessaging.getInstance().subscribeToTopic("campus_reports")
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("campus_reports")
                }
            } catch (e: Exception) {
                // swallow - optional: log
            }
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }
    
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }
    
    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            repository.setThemeColor(color.name)
        }
    }
}

