package com.campus.lostfound.data

import android.content.Context
import android.content.SharedPreferences
import com.campus.lostfound.data.model.NotificationItem
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository untuk menyimpan notifikasi secara lokal di device
 * Tidak bergantung pada Firebase, tersimpan di SharedPreferences
 * Uses SINGLETON pattern to ensure all components share the same state
 */
class LocalNotificationRepository private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("local_notifications", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
    
    // StateFlow untuk reactive UI updates
    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<NotificationItem>> = _notificationsFlow
    
    companion object {
        @Volatile
        private var INSTANCE: LocalNotificationRepository? = null
        
        fun getInstance(context: Context): LocalNotificationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalNotificationRepository(context.applicationContext).also { 
                    INSTANCE = it 
                    android.util.Log.d("LocalNotificationRepo", "Singleton instance created")
                }
            }
        }
    }
    
    init {
        // Load existing notifications on init
        refreshNotifications()
    }
    
    /**
     * Tambah notifikasi baru ke local storage
     */
    fun addNotification(
        title: String,
        body: String,
        type: String = "GENERAL",
        itemId: String? = null,
        itemName: String? = null,
        itemType: String? = null
    ): Boolean {
        return try {
            val currentNotifications = getNotifications().toMutableList()
            
            val newNotification = NotificationItem(
                id = "local_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
                title = title,
                description = body,
                timestamp = Timestamp(System.currentTimeMillis() / 1000, 0),
                read = false,
                itemId = itemId
            )
            
            // Add to beginning of list (newest first)
            currentNotifications.add(0, newNotification)
            
            // Keep only last 50 notifications to prevent storage bloat
            val limitedNotifications = currentNotifications.take(50)
            
            // Save to SharedPreferences
            val json = gson.toJson(limitedNotifications)
            prefs.edit().putString("notifications", json).apply()
            
            // Update StateFlow
            _notificationsFlow.value = limitedNotifications
            
            android.util.Log.d("LocalNotificationRepo", "Added local notification: $title")
            true
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to add notification", e)
            false
        }
    }
    
    /**
     * Mark notifikasi sebagai sudah dibaca
     */
    fun markAsRead(notificationId: String): Boolean {
        return try {
            val currentNotifications = getNotifications().toMutableList()
            val index = currentNotifications.indexOfFirst { it.id == notificationId }
            
            if (index >= 0) {
                currentNotifications[index] = currentNotifications[index].copy(read = true)
                
                val json = gson.toJson(currentNotifications)
                prefs.edit().putString("notifications", json).apply()
                
                _notificationsFlow.value = currentNotifications
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to mark as read", e)
            false
        }
    }
    
    /**
     * Mark semua notifikasi sebagai sudah dibaca
     */
    fun markAllAsRead(): Boolean {
        return try {
            val currentNotifications = getNotifications().map { it.copy(read = true) }
            
            val json = gson.toJson(currentNotifications)
            prefs.edit().putString("notifications", json).apply()
            
            _notificationsFlow.value = currentNotifications
            android.util.Log.d("LocalNotificationRepo", "Marked all notifications as read")
            true
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to mark all as read", e)
            false
        }
    }
    
    /**
     * Hapus notifikasi tertentu
     */
    fun deleteNotification(notificationId: String): Boolean {
        return try {
            val currentNotifications = getNotifications().toMutableList()
            val removed = currentNotifications.removeAll { it.id == notificationId }
            
            if (removed) {
                val json = gson.toJson(currentNotifications)
                prefs.edit().putString("notifications", json).apply()
                
                _notificationsFlow.value = currentNotifications
                android.util.Log.d("LocalNotificationRepo", "Deleted notification: $notificationId")
            }
            
            removed
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to delete notification", e)
            false
        }
    }
    
    /**
     * Hapus semua notifikasi lokal
     */
    fun clearAllNotifications(): Boolean {
        return try {
            prefs.edit().remove("notifications").apply()
            _notificationsFlow.value = emptyList()
            android.util.Log.d("LocalNotificationRepo", "Cleared all local notifications")
            true
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to clear notifications", e)
            false
        }
    }
    
    /**
     * Get semua notifikasi dari local storage
     */
    private fun getNotifications(): List<NotificationItem> {
        return try {
            val json = prefs.getString("notifications", null)
            if (json != null) {
                val type = object : TypeToken<List<NotificationItem>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to get notifications", e)
            emptyList()
        }
    }
    
    /**
     * Refresh StateFlow dengan data terbaru
     */
    private fun refreshNotifications() {
        _notificationsFlow.value = getNotifications()
    }
    
    /**
     * Get unread count untuk badge
     */
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.read }
    }
    
    /**
     * Clean up expired notifications (older than 30 days)
     */
    fun cleanupExpiredNotifications(): Boolean {
        return try {
            val thirtyDaysAgo = Timestamp(System.currentTimeMillis() / 1000 - (30L * 24 * 60 * 60), 0)
            val currentNotifications = getNotifications()
            val activeNotifications = currentNotifications.filter { 
                it.timestamp.seconds > thirtyDaysAgo.seconds 
            }
            
            if (activeNotifications.size != currentNotifications.size) {
                val json = gson.toJson(activeNotifications)
                prefs.edit().putString("notifications", json).apply()
                _notificationsFlow.value = activeNotifications
                
                val removedCount = currentNotifications.size - activeNotifications.size
                android.util.Log.d("LocalNotificationRepo", "Cleaned up $removedCount expired notifications")
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("LocalNotificationRepo", "Failed to cleanup expired notifications", e)
            false
        }
    }
}