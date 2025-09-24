package com.example.demantiaclockx.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.demantiaclockx.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "UpdateBroadcastReceiver"
        const val ACTION_UPDATE = "com.example.demantiaclockx.UPDATE_ACTION"
        const val EXTRA_UPDATE_INFO = "update_info"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "UpdateBroadcastReceiver.onReceive() called")
        Log.d(TAG, "Intent action: ${intent?.action}")
        Log.d(TAG, "Expected action: $ACTION_UPDATE")
        Log.d(TAG, "Context is null: ${context == null}")
        
        if (intent?.action == ACTION_UPDATE && context != null) {
            Log.d(TAG, "Action matches, processing update request...")
            
            val version = intent.getStringExtra("version")
            val downloadUrl = intent.getStringExtra("downloadUrl")
            val releaseNotes = intent.getStringExtra("releaseNotes")
            
            Log.d(TAG, "Extracted data - Version: $version, URL: $downloadUrl")
            
            if (version != null && downloadUrl != null && releaseNotes != null) {
                Log.d(TAG, "All required data present, creating UpdateInfo...")
                val updateInfo = UpdateInfo(version, downloadUrl, releaseNotes, true)
                
                // Start the update process
                Log.d(TAG, "Starting update process...")
                val updateManager = UpdateManager.getInstance(context)
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "Calling downloadAndInstallUpdate...")
                    updateManager.downloadAndInstallUpdate(updateInfo)
                }
                
                // Clear the notification
                Log.d(TAG, "Clearing notification...")
                updateManager.clearNotifications()
                Log.d(TAG, "Notification cleared")
            } else {
                Log.e(TAG, "Missing required data in intent")
            }
        } else {
            Log.w(TAG, "Intent action doesn't match or context is null")
        }
    }
    
    private fun handleUpdateAction(context: Context, intent: Intent) {
        try {
            // UpdateManager'ı al
            val updateManager = UpdateManager.getInstance(context)
            
            // Güncelleme bilgilerini intent'ten al
            val version = intent.getStringExtra("version")
            val downloadUrl = intent.getStringExtra("downloadUrl")
            val releaseNotes = intent.getStringExtra("releaseNotes")
            
            if (version != null && downloadUrl != null && releaseNotes != null) {
                Log.d(TAG, "Starting update download for version: $version")
                
                val updateInfo = UpdateInfo(
                    version = version,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = true
                )
                
                // Güncelleme işlemini başlat
                CoroutineScope(Dispatchers.IO).launch {
                    val success = updateManager.downloadAndInstallUpdate(updateInfo)
                    Log.d(TAG, "Update process result: $success")
                }
                
                // Bildirimi temizle
                updateManager.clearNotifications()
                
            } else {
                Log.e(TAG, "Missing update information in intent")
                Log.e(TAG, "Version: $version, DownloadUrl: $downloadUrl, ReleaseNotes: $releaseNotes")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling update action", e)
        }
    }
}