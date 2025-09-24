package com.example.demantiaclockx

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.demantiaclockx.databinding.ActivitySettingsBinding
import com.example.demantiaclockx.update.UpdateManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var updateManager: UpdateManager
    
    // Debug log components
    private lateinit var btnShowLogs: MaterialButton
    private lateinit var btnClearLogs: MaterialButton
    private lateinit var scrollViewLogs: ScrollView
    private lateinit var tvLogs: TextView
    private var isLogsVisible = false
    
    companion object {
        const val PREFS_NAME = "DemantiaClockPrefs"
        const val THEME_KEY = "selected_theme"
        const val DEFAULT_THEME = "white_gray"
        private const val TAG = "SettingsActivity"
        
        // Debug log yazma fonksiyonu (static)
        fun writeDebugLog(context: Context, message: String) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val logMessage = "[$timestamp] $message\n"
                
                val logFile = File(context.filesDir, "update_logs.txt")
                logFile.appendText(logMessage)
                
                // Log dosyası çok büyürse eski logları temizle (5000 satırdan fazla)
                val lines = logFile.readLines()
                if (lines.size > 5000) {
                    val recentLines = lines.takeLast(3000)
                    logFile.writeText(recentLines.joinToString("\n") + "\n")
                }
                
                Log.d(TAG, "Debug log written: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing debug log", e)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        updateManager = UpdateManager(this)
        
        setupUI()
        setupThemeSelection()
        setupUpdateButtons()
        setupDebugLogs()
        applyCurrentTheme()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.tvTitle.text = "Ayarlar"
        binding.tvThemeTitle.text = "Tema Seçimi"
    }
    
    private fun setupThemeSelection() {
        val currentTheme = sharedPreferences.getString(THEME_KEY, DEFAULT_THEME)
        
        // Tema butonları için click listener'lar
        binding.btnThemeWhiteGray.setOnClickListener { selectTheme("white_gray") }
        binding.btnThemeLightBlue.setOnClickListener { selectTheme("light_blue") }
        binding.btnThemeLightYellow.setOnClickListener { selectTheme("light_yellow") }
        binding.btnThemeLightPink.setOnClickListener { selectTheme("light_pink") }
        binding.btnThemeLightGreen?.setOnClickListener { selectTheme("light_green") }
        binding.btnThemeRed.setOnClickListener { selectTheme("red") }
        binding.btnThemeNavyBlue.setOnClickListener { selectTheme("navy_blue") }
        binding.btnThemeBlack.setOnClickListener { selectTheme("black") }
        
        // Mevcut temayı seçili göster
        updateSelectedTheme(currentTheme ?: DEFAULT_THEME)
    }
    
    private fun setupUpdateButtons() {
        // Manuel güncelleme kontrolü butonu
        binding.btnCheckUpdate?.setOnClickListener {
            updateManager.checkForUpdates()
        }
        
        // Otomatik güncelleme switch'i
        binding.switchAutoUpdate?.let { switch ->
            switch.isChecked = updateManager.isAutoUpdateEnabled()
            switch.setOnCheckedChangeListener { _, isChecked ->
                updateManager.setAutoUpdateEnabled(isChecked)
            }
        }
    }
    
    private fun selectTheme(theme: String) {
        // SharedPreferences'a kaydet
        sharedPreferences.edit().putString(THEME_KEY, theme).apply()
        
        // UI'ı güncelle
        updateSelectedTheme(theme)
        applyCurrentTheme()
        
        // Kullanıcıya bilgi ver
        Toast.makeText(this, "Tema değiştirildi", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateSelectedTheme(selectedTheme: String) {
        // Tüm butonları normal duruma getir
        resetAllThemeButtons()
        
        // Seçili temayı vurgula
        when (selectedTheme) {
            "white_gray" -> binding.btnThemeWhiteGray.alpha = 0.7f
            "light_blue" -> binding.btnThemeLightBlue.alpha = 0.7f
            "light_yellow" -> binding.btnThemeLightYellow.alpha = 0.7f
            "light_pink" -> binding.btnThemeLightPink.alpha = 0.7f
            "light_green" -> binding.btnThemeLightGreen?.let { it.alpha = 0.7f }
            "red" -> binding.btnThemeRed.alpha = 0.7f
            "navy_blue" -> binding.btnThemeNavyBlue.alpha = 0.7f
            "black" -> binding.btnThemeBlack.alpha = 0.7f
        }
    }
    
    private fun resetAllThemeButtons() {
        binding.btnThemeWhiteGray.alpha = 1.0f
        binding.btnThemeLightBlue.alpha = 1.0f
        binding.btnThemeLightYellow.alpha = 1.0f
        binding.btnThemeLightPink.alpha = 1.0f
        binding.btnThemeLightGreen?.let { it.alpha = 1.0f }
        binding.btnThemeRed.alpha = 1.0f
        binding.btnThemeNavyBlue.alpha = 1.0f
        binding.btnThemeBlack.alpha = 1.0f
    }
    
    private fun applyCurrentTheme() {
        val currentTheme = sharedPreferences.getString(THEME_KEY, DEFAULT_THEME)
        
        when (currentTheme) {
            "white_gray" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_white_gray))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_blue" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_blue))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_yellow" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_yellow))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_pink" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_pink))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_green" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_green))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "red" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_red))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
            "navy_blue" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_navy_blue))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
            "black" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_black))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
        }
    }
    
    private fun setupDebugLogs() {
        // Debug log bileşenlerini bul
        btnShowLogs = binding.btnShowLogs ?: return
        btnClearLogs = binding.btnClearLogs ?: return
        scrollViewLogs = binding.scrollViewLogs ?: return
        tvLogs = binding.tvLogs ?: return
        
        // Logları göster/gizle butonu
        btnShowLogs.setOnClickListener {
            isLogsVisible = !isLogsVisible
            if (isLogsVisible) {
                scrollViewLogs.visibility = View.VISIBLE
                btnShowLogs.text = "Logları Gizle"
                loadDebugLogs()
            } else {
                scrollViewLogs.visibility = View.GONE
                btnShowLogs.text = "Logları Göster"
            }
        }
        
        // Logları temizle butonu
        btnClearLogs.setOnClickListener {
            clearDebugLogs()
            if (isLogsVisible) {
                loadDebugLogs()
            }
            Toast.makeText(this, "Debug logları temizlendi", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadDebugLogs() {
        try {
            val logText = StringBuilder()
            
            // UpdateManager loglarını al
            val updateLogs = getUpdateLogs()
            if (updateLogs.isNotEmpty()) {
                logText.append("=== UPDATE MANAGER LOGS ===\n")
                logText.append(updateLogs)
                logText.append("\n\n")
            }
            
            // System loglarını al (son 50 satır)
            val systemLogs = getSystemLogs()
            if (systemLogs.isNotEmpty()) {
                logText.append("=== SYSTEM LOGS ===\n")
                logText.append(systemLogs)
                logText.append("\n\n")
            }
            
            // Eğer hiç log yoksa
            if (logText.isEmpty()) {
                logText.append("Henüz debug logu bulunmuyor.\n")
                logText.append("Otomatik güncelleme işlemini başlatmayı deneyin.\n")
            }
            
            tvLogs.text = logText.toString()
            
            // ScrollView'ı en alta kaydır
            scrollViewLogs.post {
                scrollViewLogs.fullScroll(View.FOCUS_DOWN)
            }
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error loading debug logs", e)
            tvLogs.text = "Log yükleme hatası: ${e.message}"
        }
    }
    
    private fun getUpdateLogs(): String {
        return try {
            val logFile = File(filesDir, "update_logs.txt")
            if (logFile.exists()) {
                logFile.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error reading update logs", e)
            "Update log okuma hatası: ${e.message}\n"
        }
    }
    
    private fun getSystemLogs(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -t 50 -s DemantiaClockX:* UpdateManager:* UpdateDownloader:* UpdateChecker:*")
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error reading system logs", e)
            "System log okuma hatası: ${e.message}\n"
        }
    }
    
    private fun clearDebugLogs() {
        try {
            // Update log dosyasını temizle
            val logFile = File(filesDir, "update_logs.txt")
            if (logFile.exists()) {
                logFile.delete()
            }
            
            // Yeni boş log dosyası oluştur
            logFile.createNewFile()
            logFile.writeText("Debug logları temizlendi - ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error clearing debug logs", e)
        }
    }
    
}