package com.example.demantiaclockx.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.demantiaclockx.UpdateChecker
import com.example.demantiaclockx.UpdateResult
import com.example.demantiaclockx.UpdateInfo
import com.example.demantiaclockx.SettingsActivity
import kotlinx.coroutines.*
import java.io.File

class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val PREFS_NAME = "update_preferences"
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val CHECK_INTERVAL_HOURS = 24 // 24 saatte bir kontrol et
        
        @Volatile
        private var INSTANCE: UpdateManager? = null
        
        fun getInstance(context: Context): UpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val updateChecker = UpdateChecker(context)
    private val updateDownloader = UpdateDownloader(context)
    private val notificationManager = UpdateNotificationManager(context)
    private val securityManager = UpdateSecurityManager(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var updateJob: Job? = null
    
    /**
     * Manuel güncelleme kontrolü yapar
     */
    suspend fun checkForUpdatesManually(): UpdateResult {
        Log.d(TAG, "Manuel güncelleme kontrolü başlatıldı")
        SettingsActivity.writeDebugLog(context, "Manuel güncelleme kontrolü başlatıldı")
        
        return try {
            val result = updateChecker.checkForUpdates()
            SettingsActivity.writeDebugLog(context, "Güncelleme kontrolü sonucu: $result")
            handleUpdateResult(result, isManual = true)
            result
        } catch (e: Exception) {
            Log.e(TAG, "Manuel güncelleme kontrolü hatası", e)
            SettingsActivity.writeDebugLog(context, "Manuel güncelleme kontrolü hatası: ${e.message}")
            val errorResult = UpdateResult.Error("Güncelleme kontrolü başarısız: ${e.message}")
            notificationManager.showUpdateCheckFailedNotification(errorResult.message)
            errorResult
        }
    }
    
    /**
     * Otomatik güncelleme kontrolü yapar (arka planda)
     */
    fun startPeriodicUpdateCheck() {
        if (!isAutoUpdateEnabled()) {
            Log.d(TAG, "Otomatik güncelleme devre dışı")
            SettingsActivity.writeDebugLog(context, "Otomatik güncelleme devre dışı")
            return
        }
        
        SettingsActivity.writeDebugLog(context, "Periyodik güncelleme kontrolü başlatıldı")
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    if (shouldCheckForUpdates()) {
                        Log.d(TAG, "Otomatik güncelleme kontrolü başlatıldı")
                        SettingsActivity.writeDebugLog(context, "Otomatik güncelleme kontrolü başlatıldı")
                        val result = updateChecker.checkForUpdates()
                        SettingsActivity.writeDebugLog(context, "Otomatik güncelleme sonucu: $result")
                        handleUpdateResult(result, isManual = false)
                        updateLastCheckTime()
                    }
                    
                    // Bir sonraki kontrole kadar bekle
                    delay(60 * 60 * 1000L) // 1 saat bekle
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Otomatik güncelleme kontrolü hatası", e)
                    SettingsActivity.writeDebugLog(context, "Otomatik güncelleme kontrolü hatası: ${e.message}")
                    delay(60 * 60 * 1000L) // Hata durumunda da 1 saat bekle
                }
            }
        }
    }
    
    /**
     * Güncelleme indirme ve kurulum işlemini başlatır
     */
    suspend fun downloadAndInstallUpdate(updateInfo: UpdateInfo): Boolean {
        Log.d(TAG, "downloadAndInstallUpdate() called for version: ${updateInfo.version}")
        Log.d(TAG, "Download URL: ${updateInfo.downloadUrl}")
        SettingsActivity.writeDebugLog(context, "Güncelleme indirme başlatıldı: ${updateInfo.version}")
        SettingsActivity.writeDebugLog(context, "İndirme URL'si: ${updateInfo.downloadUrl}")
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download in IO context...")
                Log.d(TAG, "Güncelleme indiriliyor: ${updateInfo.version}")
                SettingsActivity.writeDebugLog(context, "IO context'te indirme başlatıldı")
                
                // APK'yı indir
                val downloadResult = updateDownloader.downloadAndInstall(
                    updateInfo.downloadUrl,
                    updateInfo.version
                )
                
                when (downloadResult) {
                    is DownloadResult.Success -> {
                        Log.d(TAG, "APK başarıyla indirildi: ${downloadResult.apkFile.absolutePath}")
                        SettingsActivity.writeDebugLog(context, "APK başarıyla indirildi: ${downloadResult.apkFile.absolutePath}")
                        
                        // Güvenlik kontrolü yap
                        val securityResult = securityManager.verifyApkSecurity(downloadResult.apkFile)
                        when (securityResult) {
                            is SecurityCheckResult.Passed -> {
                                Log.d(TAG, "APK güvenlik kontrolü başarılı")
                                SettingsActivity.writeDebugLog(context, "APK güvenlik kontrolü başarılı")
                                
                                // Kurulumu başlat
                                updateDownloader.installApk(downloadResult.apkFile)
                                Log.d(TAG, "APK kurulum başlatıldı")
                                SettingsActivity.writeDebugLog(context, "APK kurulum başlatıldı")
                                notificationManager.showUpdateSuccessNotification(updateInfo.version)
                                return@withContext true
                            }
                            is SecurityCheckResult.Failed -> {
                                Log.e(TAG, "APK güvenlik kontrolü başarısız: ${securityResult.reason}")
                                SettingsActivity.writeDebugLog(context, "APK güvenlik kontrolü başarısız: ${securityResult.reason}")
                                notificationManager.showUpdateCheckFailedNotification(
                                    "Güvenlik kontrolü başarısız: ${securityResult.reason}"
                                )
                                // İndirilen dosyayı sil
                                downloadResult.apkFile.delete()
                                return@withContext false
                            }
                        }
                    }
                    is DownloadResult.Error -> {
                        Log.e(TAG, "APK indirme hatası: ${downloadResult.message}")
                        SettingsActivity.writeDebugLog(context, "APK indirme hatası: ${downloadResult.message}")
                        notificationManager.showUpdateCheckFailedNotification(
                            "İndirme başarısız: ${downloadResult.message}"
                        )
                        return@withContext false
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Güncelleme işlemi hatası", e)
                notificationManager.showUpdateCheckFailedNotification(
                    "Güncelleme başarısız: ${e.message}"
                )
                return@withContext false
            }
        }
    }
    
    /**
     * Güncelleme sonucunu işler
     */
    private fun handleUpdateResult(result: UpdateResult, isManual: Boolean) {
        when (result) {
            is UpdateResult.Available -> {
                Log.d(TAG, "Güncelleme mevcut: ${result.updateInfo.version}")
                notificationManager.showUpdateAvailableNotification(
                    result.updateInfo
                ) {
                    // Güncelleme butonuna tıklandığında
                    CoroutineScope(Dispatchers.IO).launch {
                        downloadAndInstallUpdate(result.updateInfo)
                    }
                }
            }
            is UpdateResult.NoUpdate -> {
                Log.d(TAG, "Uygulama güncel")
                if (isManual) {
                    // Manuel kontrolde kullanıcıya bilgi ver
                    notificationManager.showUpdateSuccessNotification("Uygulama zaten güncel")
                }
            }
            is UpdateResult.Error -> {
                Log.e(TAG, "Güncelleme kontrolü hatası: ${result.message}")
                if (isManual) {
                    notificationManager.showUpdateCheckFailedNotification(result.message)
                }
            }
        }
    }
    
    /**
     * Güncelleme kontrolü yapılması gerekip gerekmediğini kontrol eder
     */
    private fun shouldCheckForUpdates(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastCheck
        val intervalMs = CHECK_INTERVAL_HOURS * 60 * 60 * 1000L
        
        return timeDiff >= intervalMs
    }
    
    /**
     * Son kontrol zamanını günceller
     */
    private fun updateLastCheckTime() {
        prefs.edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Otomatik güncelleme ayarını kontrol eder
     */
    fun isAutoUpdateEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true) // Varsayılan olarak açık
    }
    
    /**
     * Otomatik güncelleme ayarını değiştirir
     */
    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled)
            .apply()
        
        if (enabled) {
            startPeriodicUpdateCheck()
        } else {
            stopPeriodicUpdateCheck()
        }
    }
    
    /**
     * Periyodik güncelleme kontrolünü durdurur
     */
    fun stopPeriodicUpdateCheck() {
        updateJob?.cancel()
        updateJob = null
        Log.d(TAG, "Otomatik güncelleme kontrolü durduruldu")
    }
    
    /**
     * UI için basit güncelleme kontrolü
     */
    fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            checkForUpdatesManually()
        }
    }
    
    /**
     * Tüm bildirimleri temizler
     */
    fun clearNotifications() {
        notificationManager.clearAllNotifications()
    }
    
    /**
     * Mevcut imza hash'ini loglar (geliştirme amaçlı)
     */
    fun logCurrentSignatureHash() {
        securityManager.logCurrentSignatureHash()
    }
}