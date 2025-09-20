package com.example.demantiaclockx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.REBOOT",
            "com.samsung.android.action.BOOT_COMPLETED",
            "com.huawei.android.launcher.action.BOOT_COMPLETED" -> {
                Log.d(TAG, "Boot completed detected, starting AutoStartService")
                startAutoStartService(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced detected, starting AutoStartService")
                startAutoStartService(context)
            }
            else -> {
                Log.d(TAG, "Unknown action: $action")
            }
        }
    }
    
    private fun startAutoStartService(context: Context) {
        try {
            val serviceIntent = Intent(context, AutoStartService::class.java)
            
            // Android 8.0+ için startForegroundService kullan
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "AutoStartService started as foreground service")
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "AutoStartService started as regular service")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AutoStartService: ${e.message}", e)
        }
    }
}