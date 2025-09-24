package com.example.demantiaclockx.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateDownloader"
        private const val NOTIFICATION_CHANNEL_ID = "update_download"
        private const val NOTIFICATION_ID = 1001
        private const val DOWNLOAD_TIMEOUT_MINUTES = 10L
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .build()
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * APK dosyasını indirir ve kurulum için hazırlar
     */
    suspend fun downloadAndInstall(
        downloadUrl: String,
        version: String,
        onProgress: (progress: Int) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Starting download from: $downloadUrl")
            
            // İndirme bildirimi göster
            showDownloadNotification(0, version)
            
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext DownloadResult.Error("İndirme başarısız: ${response.code}")
            }
            
            val body = response.body
            if (body == null) {
                Log.e(TAG, "Response body is null")
                return@withContext DownloadResult.Error("İndirme verisi alınamadı")
            }
            
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            
            // APK dosyasını kaydet
            val apkFile = getApkFile(version)
            val outputStream = FileOutputStream(apkFile)
            
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    onProgress(progress)
                    showDownloadNotification(progress, version)
                }
            }
            
            outputStream.close()
            inputStream.close()
            
            Log.d(TAG, "Download completed: ${apkFile.absolutePath}")
            
            // İndirme tamamlandı, kurulum başlat
            showInstallNotification(apkFile, version)
            installApk(apkFile)
            
            return@withContext DownloadResult.Success(apkFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            notificationManager.cancel(NOTIFICATION_ID)
            return@withContext DownloadResult.Error("İndirme hatası: ${e.message}")
        }
    }
    
    private fun getApkFile(version: String): File {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        return File(downloadsDir, "DemantiaClockX_v${version}.apk")
    }
    
    fun installApk(apkFile: File) {
        try {
            Log.d(TAG, "Preparing APK installation for: ${apkFile.absolutePath}")
            
            // Kurulum öncesi sistem kaynaklarını temizle
            System.gc()
            
            val intent = Intent(Intent.ACTION_VIEW)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ için FileProvider kullan
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "Using FileProvider URI: $apkUri")
            } else {
                // Android 6.0 ve altı için doğrudan URI
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                Log.d(TAG, "Using direct file URI")
            }
            
            // Kurulum intent'ini başlat
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Kurulum öncesi kısa bir bekleme ekle (grafik bağlamının temizlenmesi için)
            Thread.sleep(500)
            
            context.startActivity(intent)
            
            Log.d(TAG, "Install intent started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting install", e)
            // Hata durumunda kullanıcıya bildirim göster
            showInstallErrorNotification(e.message ?: "Bilinmeyen hata")
        }
    }
    
    private fun showInstallErrorNotification(errorMessage: String) {
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Kurulum Hatası")
                .setContentText("APK kurulumu başarısız: $errorMessage")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
             Log.e(TAG, "Error showing install error notification", e)
         }
     }
     
     private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Uygulama Güncellemeleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Uygulama güncelleme indirme durumu"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showDownloadNotification(progress: Int, version: String) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("DemantiaClockX Güncelleniyor")
            .setContentText("Versiyon $version indiriliyor...")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showInstallNotification(apkFile: File, version: String) {
        val installIntent = Intent(Intent.ACTION_VIEW)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
        }
        
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Güncelleme Hazır")
            .setContentText("Versiyon $version kurulmaya hazır. Dokunarak yükleyin.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_input_add,
                "Yükle",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

sealed class DownloadResult {
    data class Success(val apkFile: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}