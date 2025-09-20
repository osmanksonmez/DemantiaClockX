package com.example.demantiaclockx

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isNewer: Boolean
)

sealed class UpdateResult {
    data class Available(val updateInfo: UpdateInfo) : UpdateResult()
    object NoUpdate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

class UpdateChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateChecker"
        // GitHub repository bilgileri - gerçek repository bilgilerinizle değiştirin
        private const val GITHUB_OWNER = "your-username"  // GitHub kullanıcı adınız
        private const val GITHUB_REPO = "DemantiaClockX"  // Repository adı
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }
    
    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from GitHub Releases...")
            
            val currentVersion = getCurrentVersion()
            Log.d(TAG, "Current version: $currentVersion")
            
            val latestRelease = fetchLatestRelease()
            if (latestRelease == null) {
                Log.w(TAG, "Could not fetch latest release")
                return@withContext UpdateResult.Error("Güncelleme bilgisi alınamadı")
            }
            
            val latestVersion = latestRelease.getString("tag_name").removePrefix("v")
            val downloadUrl = getApkDownloadUrl(latestRelease)
            val releaseNotes = latestRelease.optString("body", "Güncelleme notları mevcut değil")
            
            Log.d(TAG, "Latest version: $latestVersion")
            Log.d(TAG, "Download URL: $downloadUrl")
            
            if (downloadUrl.isEmpty()) {
                Log.w(TAG, "No APK download URL found in release")
                return@withContext UpdateResult.Error("APK dosyası bulunamadı")
            }
            
            val isNewer = isVersionNewer(currentVersion, latestVersion)
            Log.d(TAG, "Is newer version available: $isNewer")
            
            if (isNewer) {
                val updateInfo = UpdateInfo(
                    version = latestVersion,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = true
                )
                UpdateResult.Available(updateInfo)
            } else {
                UpdateResult.NoUpdate
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateResult.Error("Güncelleme kontrolü başarısız: ${e.message}")
        }
    }
    
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not get current version", e)
            "1.0.0"
        }
    }
    
    private fun fetchLatestRelease(): JSONObject? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "GitHub API response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d(TAG, "GitHub API response received")
                JSONObject(response)
            } else {
                Log.w(TAG, "GitHub API request failed with code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest release", e)
            null
        }
    }
    
    private fun getApkDownloadUrl(release: JSONObject): String {
        return try {
            val assets = release.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return asset.getString("browser_download_url")
                }
            }
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing APK download URL", e)
            ""
        }
    }
    
    private fun isVersionNewer(currentVersion: String, latestVersion: String): Boolean {
        return try {
            val current = parseVersion(currentVersion)
            val latest = parseVersion(latestVersion)
            
            for (i in 0 until maxOf(current.size, latest.size)) {
                val currentPart = current.getOrNull(i) ?: 0
                val latestPart = latest.getOrNull(i) ?: 0
                
                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            false
        }
    }
    
    private fun parseVersion(version: String): List<Int> {
        return version.split(".")
            .map { it.replace(Regex("[^0-9]"), "") }
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }
    }
}