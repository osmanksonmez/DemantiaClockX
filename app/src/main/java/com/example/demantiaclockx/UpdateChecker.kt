package com.example.demantiaclockx

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.demantiaclockx.update.UpdateConfig
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
    }
    
    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from GitHub Releases...")
            Log.d(TAG, "Repository: ${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}")
            
            val currentVersion = getCurrentVersion()
            Log.d(TAG, "Current version: $currentVersion")
            
            val latestRelease = fetchLatestRelease()
            if (latestRelease == null) {
                Log.w(TAG, "Could not fetch latest release - API call failed")
                return@withContext UpdateResult.Error("GitHub'dan güncelleme bilgisi alınamadı. İnternet bağlantınızı kontrol edin.")
            }
            
            val latestVersion = latestRelease.getString("tag_name").removePrefix("v")
            val downloadUrl = getApkDownloadUrl(latestRelease)
            val releaseNotes = latestRelease.optString("body", "Güncelleme notları mevcut değil")
            
            Log.d(TAG, "Latest version: $latestVersion")
            Log.d(TAG, "Download URL: $downloadUrl")
            
            if (downloadUrl.isEmpty()) {
                Log.w(TAG, "No APK download URL found in release")
                return@withContext UpdateResult.Error("Güncelleme dosyası bulunamadı")
            }
            
            val isNewer = isVersionNewer(currentVersion, latestVersion)
            Log.d(TAG, "Version comparison - Current: $currentVersion, Latest: $latestVersion, Is newer: $isNewer")
            
            if (isNewer) {
                Log.i(TAG, "New version available: $latestVersion")
                val updateInfo = UpdateInfo(
                    version = latestVersion,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = true
                )
                UpdateResult.Available(updateInfo)
            } else {
                Log.i(TAG, "No update needed - current version is up to date")
                UpdateResult.NoUpdate
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}", e)
            UpdateResult.Error("Güncelleme kontrolü başarısız: ${e.localizedMessage ?: e.message}")
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
            val apiUrl = UpdateConfig.getReleasesApiUrl()
            Log.d(TAG, "=== GitHub API Request Debug ===")
            Log.d(TAG, "API URL: $apiUrl")
            Log.d(TAG, "User-Agent: ${UpdateConfig.USER_AGENT}")
            Log.d(TAG, "Connect Timeout: ${UpdateConfig.CONNECT_TIMEOUT}ms")
            Log.d(TAG, "Read Timeout: ${UpdateConfig.READ_TIMEOUT}ms")
            
            val url = URL(apiUrl)
            Log.d(TAG, "URL object created successfully")
            
            val connection = url.openConnection() as HttpURLConnection
            Log.d(TAG, "HTTP connection opened")
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
            connection.connectTimeout = UpdateConfig.CONNECT_TIMEOUT
            connection.readTimeout = UpdateConfig.READ_TIMEOUT
            
            Log.d(TAG, "Request properties set, attempting connection...")
            val responseCode = connection.responseCode
            Log.d(TAG, "GitHub API response code: $responseCode")
            Log.d(TAG, "Response message: ${connection.responseMessage}")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d(TAG, "GitHub API response received successfully")
                Log.d(TAG, "Response length: ${response.length} characters")
                JSONObject(response)
            } else {
                // Read error response for better debugging
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).readText()
                } else {
                    "No error details available"
                }
                
                Log.e(TAG, "GitHub API request failed with code: $responseCode")
                Log.e(TAG, "Error response: $errorMessage")
                
                when (responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e(TAG, "Repository or releases not found (404). Check if repository exists and has releases.")
                    }
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        Log.e(TAG, "Access forbidden (403). Repository might be private or rate limit exceeded.")
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        Log.e(TAG, "Unauthorized access (401). Authentication might be required.")
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== GitHub API Error Debug ===")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Full exception:", e)
            
            // Check if it's a network-related exception
            when (e) {
                is java.net.UnknownHostException -> {
                    Log.e(TAG, "Network error: Unable to resolve hostname")
                }
                is java.net.ConnectException -> {
                    Log.e(TAG, "Network error: Connection failed")
                }
                is java.net.SocketTimeoutException -> {
                    Log.e(TAG, "Network error: Connection timeout")
                }
                is java.io.IOException -> {
                    Log.e(TAG, "IO error: ${e.message}")
                }
            }
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