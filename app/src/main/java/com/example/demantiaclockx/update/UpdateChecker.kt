package com.example.demantiaclockx.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateChecker"
        // GitHub repository bilgilerinizi buraya ekleyin
        private const val GITHUB_OWNER = "your-username"  // GitHub kullanıcı adınız
        private const val GITHUB_REPO = "DemantiaClockX"   // Repository adı
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * GitHub'dan en son release bilgisini kontrol eder
     */
    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            Log.d(TAG, "Current version: $currentVersion")
            
            val latestRelease = getLatestRelease()
            if (latestRelease == null) {
                Log.w(TAG, "Could not fetch latest release")
                return@withContext UpdateResult.Error("Release bilgisi alınamadı")
            }
            
            val latestVersion = latestRelease.tagName.removePrefix("v")
            Log.d(TAG, "Latest version: $latestVersion")
            
            if (isNewerVersion(currentVersion, latestVersion)) {
                val apkAsset = latestRelease.assets.find { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                if (apkAsset != null) {
                    Log.d(TAG, "Update available: $latestVersion")
                    return@withContext UpdateResult.UpdateAvailable(
                        latestVersion = latestVersion,
                        downloadUrl = apkAsset.downloadUrl,
                        releaseNotes = latestRelease.body,
                        fileSize = apkAsset.size
                    )
                } else {
                    Log.w(TAG, "No APK found in latest release")
                    return@withContext UpdateResult.Error("APK dosyası bulunamadı")
                }
            } else {
                Log.d(TAG, "App is up to date")
                return@withContext UpdateResult.UpToDate
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return@withContext UpdateResult.Error("Güncelleme kontrolü başarısız: ${e.message}")
        }
    }
    
    private suspend fun getLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    return@withContext gson.fromJson(responseBody, GitHubRelease::class.java)
                }
            } else {
                Log.e(TAG, "GitHub API error: ${response.code} - ${response.message}")
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest release", e)
            return@withContext null
        }
    }
    
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not get current version", e)
            "1.0"
        }
    }
    
    /**
     * Versiyon karşılaştırması yapar
     * @param current Mevcut versiyon (örn: "1.0.0")
     * @param latest En son versiyon (örn: "1.1.0")
     * @return true eğer latest > current ise
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, latestParts.size)
            
            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrNull(i) ?: 0
                val latestPart = latestParts.getOrNull(i) ?: 0
                
                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            return false
        }
    }
}

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val fileSize: Long
    ) : UpdateResult()
    
    data class Error(val message: String) : UpdateResult()
}