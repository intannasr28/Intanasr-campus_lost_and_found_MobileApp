package com.campus.lostfound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {
    private val CHANNEL_ID = "campus_lostfound_notifications"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        createChannelIfNeeded()

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Campus Lost & Found"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Anda memiliki notifikasi baru"

        // Extract data for navigation
        val notificationType = remoteMessage.data["type"] ?: ""
        val itemId = remoteMessage.data["itemId"] ?: ""

        // Create pending intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (notificationType) {
                "NEW_REPORT", "COMPLETED_REPORT" -> {
                    // Navigate to detail screen if itemId available, otherwise home
                    if (itemId.isNotEmpty()) {
                        putExtra("navigate_to", "detail")
                        putExtra("item_id", itemId)
                    } else {
                        putExtra("navigate_to", "home")
                    }
                }
                else -> {
                    putExtra("navigate_to", "notifications")
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            itemId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        with(NotificationManagerCompat.from(this)) {
            notify(itemId.hashCode().takeIf { it != 0 } ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        }
    }

    override fun onNewToken(token: String) {
        // Optional: send token to server if you later implement targeted messaging
        super.onNewToken(token)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Campus Lost & Found"
            val descriptionText = "Channel untuk notifikasi aplikasi"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
