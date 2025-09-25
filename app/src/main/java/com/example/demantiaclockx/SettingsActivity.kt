package com.example.demantiaclockx

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.demantiaclockx.databinding.ActivitySettingsBinding
import com.example.demantiaclockx.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var updateManager: UpdateManager
    
    companion object {
        const val PREFS_NAME = "DemantiaClockPrefs"
        const val THEME_KEY = "selected_theme"
        const val DEFAULT_THEME = "white_gray"
        private const val TAG = "SettingsActivity"
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
        applyCurrentTheme()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.tvTitle.text = "Ayarlar"
        binding.tvThemeTitle.text = "Tema Seçimi"
        
        // Versiyon bilgisini göster
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion?.text = "v$versionName"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version info: ${e.message}")
            binding.tvVersion?.text = "v1.0.10"
        }
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
        
        // Mevcut temayı seçili göster
        updateSelectedTheme(currentTheme ?: DEFAULT_THEME)
    }
    
    private fun setupUpdateButtons() {
        // Manuel güncelleme kontrolü butonu
        binding.btnCheckUpdate?.setOnClickListener {
            checkForUpdatesWithFeedback()
        }
        
        // Landscape layout için alternatif buton ID'si
        binding.btnCheckForUpdates?.setOnClickListener {
            checkForUpdatesWithFeedback()
        }
    }
    
    private fun checkForUpdatesWithFeedback() {
        // Kullanıcıya güncelleme kontrolünün başladığını bildir
        Toast.makeText(this, "Güncelleme kontrol ediliyor...", Toast.LENGTH_SHORT).show()
        
        // Her iki butonu da geçici olarak devre dışı bırak
        binding.btnCheckUpdate?.isEnabled = false
        binding.btnCheckUpdate?.text = "Kontrol ediliyor..."
        binding.btnCheckForUpdates?.isEnabled = false
        binding.btnCheckForUpdates?.text = "Kontrol ediliyor..."
        
        // Güncelleme kontrolünü başlat
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = updateManager.checkForUpdatesManually()
                
                // Ana thread'de UI güncellemelerini yap
                CoroutineScope(Dispatchers.Main).launch {
                    // Her iki butonu da tekrar aktif et
                    binding.btnCheckUpdate?.isEnabled = true
                    binding.btnCheckUpdate?.text = "Güncelleme Kontrol Et"
                    binding.btnCheckForUpdates?.isEnabled = true
                    binding.btnCheckForUpdates?.text = "Güncelleme Kontrol Et"
                    
                    // Sonuca göre kullanıcıya bilgi ver
                    when (result) {
                        is com.example.demantiaclockx.UpdateResult.Available -> {
                            Toast.makeText(
                                this@SettingsActivity,
                                "Yeni güncelleme mevcut: v${result.updateInfo.version}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is com.example.demantiaclockx.UpdateResult.NoUpdate -> {
                            Toast.makeText(
                                this@SettingsActivity,
                                "Uygulama zaten güncel!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is com.example.demantiaclockx.UpdateResult.Error -> {
                            Toast.makeText(
                                this@SettingsActivity,
                                "Güncelleme kontrolü başarısız: ${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ana thread'de hata mesajını göster
                CoroutineScope(Dispatchers.Main).launch {
                    binding.btnCheckUpdate?.isEnabled = true
                    binding.btnCheckUpdate?.text = "Güncelleme Kontrol Et"
                    binding.btnCheckForUpdates?.isEnabled = true
                    binding.btnCheckForUpdates?.text = "Güncelleme Kontrol Et"
                    
                    Toast.makeText(
                        this@SettingsActivity,
                        "Güncelleme kontrolü sırasında hata oluştu: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
        }
    }
    
    private fun resetAllThemeButtons() {
        binding.btnThemeWhiteGray.alpha = 1.0f
        binding.btnThemeLightBlue.alpha = 1.0f
        binding.btnThemeLightYellow.alpha = 1.0f
        binding.btnThemeLightPink.alpha = 1.0f
        binding.btnThemeLightGreen?.let { it.alpha = 1.0f }
        binding.btnThemeRed.alpha = 1.0f
    }
    
    private fun applyCurrentTheme() {
        val currentTheme = sharedPreferences.getString(THEME_KEY, DEFAULT_THEME)
        
        when (currentTheme) {
            "white_gray" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_white_gray))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_blue" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_blue))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_yellow" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_yellow))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_pink" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_pink))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "light_green" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_light_green))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
            }
            "red" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_red))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
            "navy_blue" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_navy_blue))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
            "black" -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.theme_black))
                binding.tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvThemeTitle.setTextColor(ContextCompat.getColor(this, R.color.text_light))
                binding.tvVersion?.setTextColor(ContextCompat.getColor(this, R.color.text_light))
            }
        }
    }

    
}