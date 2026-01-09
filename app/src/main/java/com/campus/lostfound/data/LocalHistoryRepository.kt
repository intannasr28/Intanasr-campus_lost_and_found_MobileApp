package com.campus.lostfound.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.campus.lostfound.data.model.Category
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import com.google.firebase.Timestamp
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Repository untuk menyimpan riwayat laporan yang sudah selesai secara lokal di device.
 * Data akan hilang jika user menghapus data aplikasi atau uninstall.
 */
class LocalHistoryRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    private val _historyFlow = MutableStateFlow<List<CompletedReport>>(emptyList())
    val historyFlow: Flow<List<CompletedReport>> = _historyFlow.asStateFlow()
    
    init {
        // Load history on init
        loadHistory()
    }
    
    /**
     * Data class untuk laporan yang sudah selesai dengan info tambahan
     */
    data class CompletedReport(
        val item: LostFoundItem,
        val completedAt: Long, // Timestamp saat diselesaikan
        val completedAtFormatted: String // String format tanggal
    )
    
    /**
     * Menyimpan laporan yang sudah selesai ke local storage
     */
    fun saveCompletedReport(item: LostFoundItem): Boolean {
        return try {
            val existingJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val jsonArray = JSONArray(existingJson)
            
            // Check if already exists
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == item.id) {
                    Log.d(TAG, "Report already in history: ${item.id}")
                    return true
                }
            }
            
            // Create new entry
            val completedAt = System.currentTimeMillis()
            val completedAtFormatted = formatDate(completedAt)
            
            val reportJson = JSONObject().apply {
                put("id", item.id)
                put("userId", item.userId)
                put("type", item.type.name)
                put("itemName", item.itemName)
                put("category", item.category.name)
                put("location", item.location)
                put("description", item.description)
                put("imageUrl", item.imageUrl)
                put("whatsappNumber", item.whatsappNumber)
                put("createdAt", item.createdAt.toDate().time)
                put("completedAt", completedAt)
                put("completedAtFormatted", completedAtFormatted)
            }
            
            jsonArray.put(reportJson)
            
            prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
            
            Log.d(TAG, "Report saved to local history: ${item.itemName}")
            
            // Reload to update flow
            loadHistory()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save completed report: ${e.message}")
            false
        }
    }
    
    /**
     * Get completed report by item ID from local history
     */
    fun getHistoryById(itemId: String): CompletedReport? {
        return try {
            val json = prefs.getString(KEY_HISTORY, null)
            if (json != null) {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val reportJson = jsonArray.getJSONObject(i)
                    val completedReport = gson.fromJson(reportJson.toString(), CompletedReport::class.java)
                    if (completedReport.item.id == itemId) {
                        return completedReport
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get history by ID: ${e.message}")
            null
        }
    }

    /**
     * Menghapus laporan dari riwayat lokal
     */
    fun deleteFromHistory(itemId: String): Boolean {
        return try {
            val existingJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val jsonArray = JSONArray(existingJson)
            val newArray = JSONArray()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") != itemId) {
                    newArray.put(obj)
                }
            }
            
            prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply()
            
            Log.d(TAG, "Report deleted from local history: $itemId")
            
            // Reload to update flow
            loadHistory()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from history: ${e.message}")
            false
        }
    }
    
    /**
     * Menghapus semua riwayat lokal
     */
    fun clearAllHistory(): Boolean {
        return try {
            prefs.edit().putString(KEY_HISTORY, "[]").apply()
            loadHistory()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history: ${e.message}")
            false
        }
    }
    
    /**
     * Load history dari SharedPreferences
     */
    private fun loadHistory() {
        try {
            val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val jsonArray = JSONArray(json)
            val reports = mutableListOf<CompletedReport>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val item = LostFoundItem(
                    id = obj.optString("id", ""),
                    userId = obj.optString("userId", ""),
                    type = try { 
                        ItemType.valueOf(obj.optString("type", "LOST")) 
                    } catch (e: Exception) { 
                        ItemType.LOST 
                    },
                    itemName = obj.optString("itemName", ""),
                    category = try { 
                        Category.valueOf(obj.optString("category", "OTHER")) 
                    } catch (e: Exception) { 
                        Category.OTHER 
                    },
                    location = obj.optString("location", ""),
                    description = obj.optString("description", ""),
                    imageUrl = obj.optString("imageUrl", ""),
                    whatsappNumber = obj.optString("whatsappNumber", ""),
                    isCompleted = true,
                    createdAt = Timestamp(Date(obj.optLong("createdAt", System.currentTimeMillis()))),
                    imageStoragePath = ""
                )
                
                val completedAt = obj.optLong("completedAt", System.currentTimeMillis())
                val completedAtFormatted = obj.optString("completedAtFormatted", formatDate(completedAt))
                
                reports.add(CompletedReport(item, completedAt, completedAtFormatted))
            }
            
            // Sort by completedAt descending (terbaru di atas)
            _historyFlow.value = reports.sortedByDescending { it.completedAt }
            
            Log.d(TAG, "Loaded ${reports.size} reports from local history")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history: ${e.message}")
            _historyFlow.value = emptyList()
        }
    }
    
    /**
     * Get history count
     */
    fun getHistoryCount(): Int {
        return _historyFlow.value.size
    }
    
    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    companion object {
        private const val TAG = "LocalHistoryRepo"
        private const val PREFS_NAME = "local_history_prefs"
        private const val KEY_HISTORY = "completed_reports_history"
    }
}
