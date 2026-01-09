package com.campus.lostfound.service

import android.content.Context
import android.util.Log
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * OneSignal Integration Service
 * Mengirim push notifications ke semua users via OneSignal
 * GRATIS sampai 10,000 notifications/month
 */
class OneSignalNotificationService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "OneSignalService"
        
        // OneSignal App ID from BuildConfig
        private const val ONESIGNAL_APP_ID = "bff31d8e-34a5-4b4a-860b-3b7798604916"
        
        // REST API Key from OneSignal Dashboard
        // Get from: OneSignal → Settings → Keys & IDs → Add Key → REST API Key
        private const val ONESIGNAL_REST_API_KEY = "os_v2_app_x7zr3druuvfuvbqlhn3zqycjc3beitky2ywuqffukmoap5xvwqfwuw6ggozhhqmmussxostprzn3th4try6iemwej7dzf6tk6ww2c5a"
        
        private const val ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications"
    }
    
    /**
     * Initialize OneSignal SDK
     * Call this from MainActivity.onCreate()
     */
    fun initialize() {
        try {
            // Verbose Logging (untuk debugging)
            OneSignal.Debug.logLevel = com.onesignal.debug.LogLevel.VERBOSE
            
            // Initialize OneSignal with App ID
            OneSignal.initWithContext(context, ONESIGNAL_APP_ID)
            
            // Request notification permission (Android 13+)
            CoroutineScope(Dispatchers.Main).launch {
                OneSignal.Notifications.requestPermission(true)
            }
            
            Log.d(TAG, "✅ OneSignal initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize OneSignal", e)
        }
    }
    
    /**
     * Send notification untuk laporan baru
     */
    fun sendNewReportNotification(item: LostFoundItem) {
        scope.launch {
            try {
                val itemType = if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"
                val title = "📦 Laporan Baru: $itemType"
                val message = "\"${item.itemName}\" dilaporkan di ${item.location}. Tap untuk lihat detail."
                
                // Send notification via OneSignal REST API
                sendNotification(
                    title = title,
                    message = message,
                    data = mapOf(
                        "type" to "new_report",
                        "itemId" to item.id,
                        "itemName" to item.itemName
                    )
                )
                
                Log.d(TAG, "✅ New report notification sent: ${item.itemName}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send new report notification", e)
            }
        }
    }
    
    /**
     * Send notification untuk laporan selesai
     */
    fun sendCompletedReportNotification(item: LostFoundItem) {
        scope.launch {
            try {
                val itemType = if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"
                val title = "✅ $itemType Selesai"
                val message = "\"${item.itemName}\" telah dikembalikan ke pemiliknya!"
                
                sendNotification(
                    title = title,
                    message = message,
                    data = mapOf(
                        "type" to "completed_report",
                        "itemId" to item.id
                    )
                )
                
                Log.d(TAG, "✅ Completed report notification sent: ${item.itemName}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to send completed notification", e)
            }
        }
    }
    
    /**
     * Core function to send notification via OneSignal REST API
     */
    private suspend fun sendNotification(
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Check if REST API key is configured
                if (ONESIGNAL_REST_API_KEY == "YOUR_REST_API_KEY_HERE") {
                    Log.w(TAG, "⚠️ REST API Key not configured. Get it from OneSignal Dashboard → Settings → Keys & IDs")
                    return@withContext
                }
                
                // Build notification payload
                val notification = JSONObject().apply {
                    put("app_id", ONESIGNAL_APP_ID)
                    
                    // Target: All subscribed users
                    put("included_segments", org.json.JSONArray().put("All"))
                    
                    // Content
                    put("headings", JSONObject().put("en", title))
                    put("contents", JSONObject().put("en", message))
                    
                    // Android specific
                    put("android_channel_id", "campus_lostfound_notifications")
                    put("priority", 10)
                    put("ttl", 43200) // 12 hours
                    
                    // Custom data for navigation
                    if (data.isNotEmpty()) {
                        val dataJson = JSONObject()
                        data.forEach { (key, value) -> dataJson.put(key, value) }
                        put("data", dataJson)
                    }
                }
                
                // Send HTTP POST to OneSignal
                val url = URL(ONESIGNAL_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Basic $ONESIGNAL_REST_API_KEY")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                // Write payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(notification.toString())
                    writer.flush()
                }
                
                // Read response
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                }
                
                connection.disconnect()
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "✅ Notification sent successfully: $response")
                } else {
                    Log.e(TAG, "❌ Failed to send notification. Code: $responseCode, Response: $response")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ HTTP request failed", e)
                throw e
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}
