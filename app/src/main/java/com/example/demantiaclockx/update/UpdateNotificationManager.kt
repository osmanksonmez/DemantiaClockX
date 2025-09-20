package com.example.demantiaclockx.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.demantiaclockx.R

class UpdateNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "update_notifications"
        private const val UPDATE_AVAILABLE_ID = 2001
        private const val UPDATE_CHECK_FAILED_ID = 2002
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Güncelleme mevcut bildirimi gösterir
     */
    fun showUpdateAvailableNotification(
        version: String,
        releaseNotes: String,
        onUpdateClick: () -> Unit
    ) {
        // Update intent oluştur
        val updateIntent = Intent().apply {
            action = "UPDATE_ACTION"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_settings) // Mevcut icon'u kullanıyoruz
            .setContentTitle("Yeni Güncelleme Mevcut!")
            .setContentText("DemantiaClockX v$version sürümü hazır")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Versiyon $version\n\nYenilikler:\n$releaseNotes")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Güncelle",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(UPDATE_AVAILABLE_ID, notification)
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