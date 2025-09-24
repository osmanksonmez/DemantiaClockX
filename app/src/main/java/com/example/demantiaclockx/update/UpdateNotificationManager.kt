package com.example.demantiaclockx.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.demantiaclockx.R
import com.example.demantiaclockx.UpdateInfo

class UpdateNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateNotificationManager"
        private const val CHANNEL_ID = "update_notifications"
        private const val UPDATE_AVAILABLE_ID = 2001
        private const val UPDATE_CHECK_FAILED_ID = 2002
        private const val UPDATE_REQUEST_CODE = 1001
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Güncelleme mevcut bildirimi gösterir
     */
    fun showUpdateAvailableNotification(updateInfo: UpdateInfo, onUpdateClick: () -> Unit) {
        Log.d(TAG, "showUpdateAvailableNotification called for version: ${updateInfo.version}")
        Log.d(TAG, "Download URL: ${updateInfo.downloadUrl}")
        
        // Check if notifications are enabled
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled for this app")
            return
        }
        
        try {
            val updateIntent = Intent(context, UpdateBroadcastReceiver::class.java).apply {
                action = UpdateBroadcastReceiver.ACTION_UPDATE
                putExtra("version", updateInfo.version)
                putExtra("downloadUrl", updateInfo.downloadUrl)
                putExtra("releaseNotes", updateInfo.releaseNotes)
            }
            
            Log.d(TAG, "Created broadcast intent with action: ${UpdateBroadcastReceiver.ACTION_UPDATE}")
            
            val updatePendingIntent = PendingIntent.getBroadcast(
                context,
                UPDATE_REQUEST_CODE,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            Log.d(TAG, "Created PendingIntent for broadcast")
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_settings) // Mevcut icon'u kullanıyoruz
                .setContentTitle("Yeni Güncelleme Mevcut!")
                .setContentText("DemantiaClockX v${updateInfo.version} sürümü hazır")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Versiyon ${updateInfo.version}\n\nYenilikler:\n${updateInfo.releaseNotes}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(updatePendingIntent)
                .addAction(
                    android.R.drawable.stat_sys_download,
                    "Güncelle",
                    updatePendingIntent
                )
                .build()
            
            Log.d(TAG, "Built notification, now displaying...")
            notificationManager.notify(UPDATE_AVAILABLE_ID, notification)
            Log.d(TAG, "Notification displayed with ID: $UPDATE_AVAILABLE_ID")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating update notification", e)
        }
    }
    
    /**
     * Güncelleme kontrolü başarısız bildirimi gösterir
     */
    fun showUpdateCheckFailedNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Güncelleme Kontrolü Başarısız")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(UPDATE_CHECK_FAILED_ID, notification)
    }
    
    /**
     * Güncelleme başarılı bildirimi gösterir
     */
    fun showUpdateSuccessNotification(version: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Güncelleme Tamamlandı")
            .setContentText("DemantiaClockX v$version başarıyla güncellendi")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(UPDATE_AVAILABLE_ID, notification)
    }
    
    /**
     * Tüm güncelleme bildirimlerini temizler
     */
    fun clearAllNotifications() {
        notificationManager.cancel(UPDATE_AVAILABLE_ID)
        notificationManager.cancel(UPDATE_CHECK_FAILED_ID)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uygulama Güncellemeleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yeni uygulama sürümleri hakkında bildirimler"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}