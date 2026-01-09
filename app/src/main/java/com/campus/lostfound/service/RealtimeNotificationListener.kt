package com.campus.lostfound.service

import android.content.Context
import android.util.Log
import com.campus.lostfound.data.LocalNotificationRepository
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.model.ItemType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Real-time listener untuk Firestore changes
 * Mendeteksi laporan baru dan completion tanpa Cloud Functions
 * Menyimpan notifikasi ke local storage (SharedPreferences)
 */
class RealtimeNotificationListener(
    private val context: Context,
    private val localNotificationService: LocalNotificationService
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val localNotificationRepository = LocalNotificationRepository.getInstance(context)
    
    // OneSignal service for real-time push notifications
    private val oneSignalService = OneSignalNotificationService(context)
    
    // SharedPreferences untuk persist processed items (mencegah spam notifikasi)
    private val processedPrefs = context.getSharedPreferences("processed_notifications", Context.MODE_PRIVATE)
    
    // Track processed items to avoid duplicate notifications (in-memory + persistent)
    private val processedNewItems = mutableSetOf<String>()
    private val processedCompletedItems = mutableSetOf<String>()
    
    init {
        // Load previously processed items from SharedPreferences
        loadProcessedItems()
    }
    
    /**
     * Load processed item IDs from SharedPreferences to prevent duplicate notifications
     */
    private fun loadProcessedItems() {
        try {
            val newItems = processedPrefs.getStringSet("processed_new_items", emptySet()) ?: emptySet()
            val completedItems = processedPrefs.getStringSet("processed_completed_items", emptySet()) ?: emptySet()
            processedNewItems.addAll(newItems)
            processedCompletedItems.addAll(completedItems)
            Log.d("RealtimeNotification", "Loaded ${newItems.size} processed new items, ${completedItems.size} completed items")
        } catch (e: Exception) {
            Log.w("RealtimeNotification", "Failed to load processed items", e)
        }
    }
    
    /**
     * Save processed item ID to SharedPreferences
     */
    private fun saveProcessedItem(itemId: String, isCompleted: Boolean) {
        try {
            val key = if (isCompleted) "processed_completed_items" else "processed_new_items"
            val currentSet = processedPrefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            currentSet.add(itemId)
            
            // Limit to last 500 items to prevent infinite growth
            val limitedSet = if (currentSet.size > 500) {
                currentSet.toList().takeLast(500).toMutableSet()
            } else {
                currentSet
            }
            
            processedPrefs.edit().putStringSet(key, limitedSet).apply()
        } catch (e: Exception) {
            Log.w("RealtimeNotification", "Failed to save processed item", e)
        }
    }
    
    // State untuk UI
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private var itemsListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    /**
     * Start listening untuk perubahan di collection items
     */
    fun startListening() {
        if (_isListening.value) return
        
        Log.d("RealtimeNotification", "Starting realtime listener...")
        
        // Get current user's last seen timestamp to avoid showing old notifications
        val prefs = context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong("lastSeen", System.currentTimeMillis())
        
        // Update lastSeen to now for future notifications
        prefs.edit().putLong("lastSeen", System.currentTimeMillis()).apply()
        
        _isListening.value = true
        
        itemsListener = firestore.collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50) // Limit untuk performance
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("RealtimeNotification", "Listen failed", error)
                    return@addSnapshotListener
                }
                
                Log.d("RealtimeNotification", "Received ${snapshots?.documentChanges?.size ?: 0} document changes")
                
                snapshots?.documentChanges?.forEach { change ->
                    val item = change.document.toObject(LostFoundItem::class.java)
                    val itemWithId = item.copy(id = change.document.id)
                    
                    Log.d("RealtimeNotification", "Change type: ${change.type}, item: ${itemWithId.itemName}, userId: ${itemWithId.userId}")
                    
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            // New report added
                            handleNewReport(itemWithId, lastSeen)
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            // Report modified - check if completed
                            handleReportModified(itemWithId)
                        }
                        else -> {
                            // REMOVED - ignore for notifications
                        }
                    }
                }
            }
        
        Log.d("RealtimeNotification", "Realtime listener started successfully")
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        itemsListener?.remove()
        itemsListener = null
        _isListening.value = false
        Log.d("RealtimeNotification", "Realtime listener stopped")
    }
    
    private fun handleNewReport(item: LostFoundItem, userLastSeen: Long) {
        // Skip if already processed (check both memory and persistent storage)
        if (processedNewItems.contains(item.id)) {
            Log.d("RealtimeNotification", "Skipping already processed item: ${item.id}")
            return
        }
        
        // Get item creation time
        val itemCreatedTime = item.createdAt.seconds * 1000
        val now = System.currentTimeMillis()
        
        // Skip if item is older than 5 minutes
        val fiveMinutesAgo = now - (5 * 60 * 1000)
        if (itemCreatedTime < fiveMinutesAgo) {
            Log.d("RealtimeNotification", "Skipping old item: ${item.itemName}")
            processedNewItems.add(item.id)
            saveProcessedItem(item.id, false)
            return
        }
        
        Log.d("RealtimeNotification", "✅ New report detected: ${item.itemName}, showing notification!")
        
        // Show local notification (push notification in system tray)
        localNotificationService.showNewReportNotification(item)
        
        // Send OneSignal push notification to ALL users (even with app closed)
        oneSignalService.sendNewReportNotification(item)
        
        // Mark as processed (both memory and persistent)
        processedNewItems.add(item.id)
        saveProcessedItem(item.id, false)
        
        // Save notification to LOCAL storage (for in-app notification screen)
        localNotificationRepository.addNotification(
            title = "📦 Laporan Baru: ${if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"}",
            body = "\"${item.itemName}\" dilaporkan di ${item.location}. Tap untuk lihat detail.",
            type = "NEW_REPORT",
            itemId = item.id,
            itemName = item.itemName,
            itemType = item.type.name
        )
    }
    
    private fun handleReportModified(item: LostFoundItem) {
        // Check if item was marked as completed
        val isCompleted = item.isCompleted
        
        if (isCompleted && !processedCompletedItems.contains(item.id)) {
            Log.d("RealtimeNotification", "Report completed: ${item.itemName}")
            
            // Show local notification
            localNotificationService.showCompletedReportNotification(item)
            
            // Send OneSignal push notification to ALL users
            oneSignalService.sendCompletedReportNotification(item)
            
            // Mark as processed (both memory and persistent)
            processedCompletedItems.add(item.id)
            saveProcessedItem(item.id, true)
            
            // Save notification to LOCAL storage (device-specific)
            localNotificationRepository.addNotification(
                title = "✅ ${if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"} Selesai",
                body = "\"${item.itemName}\" telah dikembalikan ke pemiliknya!",
                type = "COMPLETED_REPORT", 
                itemId = item.id,
                itemName = item.itemName,
                itemType = item.type.name
            )
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        scope.cancel()
    }
}