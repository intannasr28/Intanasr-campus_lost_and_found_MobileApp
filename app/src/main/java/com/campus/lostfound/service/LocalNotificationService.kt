package com.campus.lostfound.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.campus.lostfound.MainActivity
import com.campus.lostfound.R
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service untuk mengelola local notifications tanpa Cloud Functions
 * Berjalan di Spark Plan Firebase
 */
class LocalNotificationService(private val context: Context) {
    
    private val CHANNEL_ID = "campus_lostfound_notifications"
    private val CHANNEL_NAME = "Campus Lost & Found Notifications"
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk laporan barang hilang dan ditemukan"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Tampilkan notifikasi untuk laporan baru
     */
    fun showNewReportNotification(item: LostFoundItem) {
        Log.d("LocalNotificationService", "🔔 Attempting to show notification for: ${item.itemName}")
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("LocalNotificationService", "❌ POST_NOTIFICATIONS permission not granted!")
                return
            }
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            val itemType = if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"
            val title = "📦 Laporan Baru: $itemType"
            val body = "\"${item.itemName}\" dilaporkan di ${item.location}. Tap untuk lihat detail."
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "detail")
                putExtra("item_id", item.id)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Custom app notification icon
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setColor(context.getColor(android.R.color.holo_green_dark)) // Teal accent color
                .build()
            
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
                Log.d("LocalNotificationService", "✅ Notification shown successfully for: ${item.itemName}")
            } catch (e: SecurityException) {
                // Handle permission denial gracefully
                Log.w("LocalNotificationService", "Notification permission denied", e)
            }
        }
    }
    
    /**
     * Tampilkan notifikasi untuk laporan selesai
     */
    fun showCompletedReportNotification(item: LostFoundItem) {
        Log.d("LocalNotificationService", "🔔 Attempting to show completed notification for: ${item.itemName}")
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("LocalNotificationService", "❌ POST_NOTIFICATIONS permission not granted!")
                return
            }
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            val itemType = if (item.type == ItemType.LOST) "Barang Hilang" else "Barang Ditemukan"
            val title = "✅ $itemType Selesai"
            val body = "\"${item.itemName}\" telah dikembalikan ke pemiliknya!"
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "home")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                ("completed_" + item.id).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Custom app notification icon
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setColor(context.getColor(android.R.color.holo_green_dark)) // Teal accent color
                .build()
            
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
                Log.d("LocalNotificationService", "✅ Completed notification shown for: ${item.itemName}")
            } catch (e: SecurityException) {
                Log.w("LocalNotificationService", "Notification permission denied", e)
            }
        }
    }
}