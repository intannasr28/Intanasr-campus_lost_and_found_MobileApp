package com.campus.lostfound.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import org.json.JSONObject
import org.json.JSONException
import java.util.Date

data class NotificationItem(
    @DocumentId
    val id: String = "",
    val type: NotificationType = NotificationType.NEW_REPORT,
    val title: String = "",
    val description: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val itemId: String? = null // Optional, untuk navigate ke detail
) {
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val created = timestamp.toDate().time
        val diff = now - created
        
        return when {
            diff < 60000 -> "Baru saja"
            diff < 3600000 -> "${diff / 60000} menit lalu"
            diff < 86400000 -> "${diff / 3600000} jam lalu"
            diff < 604800000 -> "${diff / 86400000} hari lalu"
            else -> "${diff / 604800000} minggu lalu"
        }
    }

    fun getFormattedDate(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
            sdf.format(timestamp.toDate())
        } catch (e: Exception) {
            ""
        }
    }

    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("id", id)
            obj.put("type", type.name)
            obj.put("title", title)
            obj.put("description", description)
            obj.put("timestamp", timestamp.toDate().time)
            obj.put("read", read)
            obj.put("itemId", itemId ?: JSONObject.NULL)
        } catch (e: JSONException) {
            // ignore
        }
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): NotificationItem? {
            return try {
                val id = obj.optString("id", "")
                val typeName = obj.optString("type", NotificationType.NEW_REPORT.name)
                val type = try { NotificationType.valueOf(typeName) } catch (e: Exception) { NotificationType.NEW_REPORT }
                val title = obj.optString("title", "")
                val description = obj.optString("description", "")
                val tsMillis = obj.optLong("timestamp", System.currentTimeMillis())
                val read = obj.optBoolean("read", false)
                val itemId = if (obj.isNull("itemId")) null else obj.optString("itemId", null)
                NotificationItem(id = id, type = type, title = title, description = description, timestamp = Timestamp(Date(tsMillis)), read = read, itemId = itemId)
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class NotificationType(val displayName: String, val iconName: String) {
    NEW_REPORT("Laporan Baru", "description"),
    CONTACTED("Dihubungi", "phone"),
    STATUS_CHANGED("Status Berubah", "info"),
    ITEM_FOUND("Barang Ditemukan", "check_circle"),
    ITEM_LOST("Barang Hilang", "search"),
    ITEM_COMPLETED("Selesai", "verified"),
    ITEM_RETURNED("Dikembalikan", "handshake"),
    MATCH_FOUND("Kecocokan Ditemukan", "connect_without_contact"),
    REMINDER("Pengingat", "alarm"),
    OTHER("Lainnya", "notifications")
}

