package com.example.demantiaclockx

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoStartWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    companion object {
        private const val TAG = "AutoStartWorker"
    }
    
    override fun doWork(): Result {
        return try {
            Log.d(TAG, "AutoStartWorker: Device booted, starting MainActivity")
            
            // MainActivity'yi başlat
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            applicationContext.startActivity(intent)
            
            Log.d(TAG, "AutoStartWorker: MainActivity started successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "AutoStartWorker: Error starting MainActivity", e)
            Result.failure()
        }
    }
}