package com.example.demantiaclockx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AutoStartService : Service() {
    
    companion object {
        private const val TAG = "AutoStartService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_start_channel"
        private const val CHANNEL_NAME = "DemantiaClockX Auto Start"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoStartService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AutoStartService started")
        
        // Foreground service olarak başlat
        startForeground(NOTIFICATION_ID, createNotification())
        
        // MainActivity'yi başlat
        startMainActivity()
        
        // Service'i durdur (görevini tamamladı)
        stopSelf()
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AutoStartService destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "DemantiaClockX otomatik başlatma bildirimi"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DemantiaClockX")
            .setContentText("Uygulama başlatılıyor...")
            .setSmallIcon(R.drawable.ic_settings)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
    }
    
    private fun startMainActivity() {
        try {
            val startIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            startActivity(startIntent)
            Log.d(TAG, "MainActivity started successfully from AutoStartService")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity from AutoStartService: ${e.message}", e)
        }
    }
}