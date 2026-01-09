package com.campus.lostfound.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore
    
    private object PreferenceKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val THEME_COLOR = stringPreferencesKey("theme_color") // ThemeColor enum name
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode") // Guest mode flag
    }
    
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true
        }
    
    val soundEnabledFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.SOUND_ENABLED] ?: true
        }
    
    val themeModeFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.THEME_MODE] ?: "light"
        }
    
    val themeColorFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.THEME_COLOR] ?: "TEAL"
        }
    
    val isGuestModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.IS_GUEST_MODE] ?: false
        }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SOUND_ENABLED] = enabled
        }
    }
    
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode
        }
    }
    
    suspend fun setThemeColor(colorName: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_COLOR] = colorName
        }
    }
    
    suspend fun setGuestMode(isGuest: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_GUEST_MODE] = isGuest
        }
    }
}