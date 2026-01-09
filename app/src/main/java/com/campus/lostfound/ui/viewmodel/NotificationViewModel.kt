package com.campus.lostfound.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.LocalNotificationRepository
import com.campus.lostfound.data.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(private val context: Context) : ViewModel() {
    private val localNotificationRepository = LocalNotificationRepository.getInstance(context)
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Use local repository's StateFlow directly - no Firebase dependency
    val notifications: StateFlow<List<NotificationItem>> = localNotificationRepository.notificationsFlow
    
    init {
        // Clean up expired notifications on init
        viewModelScope.launch {
            localNotificationRepository.cleanupExpiredNotifications()
        }
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            localNotificationRepository.markAsRead(notificationId)
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            localNotificationRepository.markAllAsRead()
        }
    }
    
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            localNotificationRepository.deleteNotification(notificationId)
        }
    }
    
    fun clearAllNotifications() {
        viewModelScope.launch {
            localNotificationRepository.clearAllNotifications()
        }
    }
    
    fun getUnreadCount(): Int {
        return localNotificationRepository.getUnreadCount()
    }
}

