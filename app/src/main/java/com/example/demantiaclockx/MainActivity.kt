package com.example.demantiaclockx

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.demantiaclockx.databinding.ActivityMainBinding
import com.example.demantiaclockx.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var updateManager: UpdateManager
    private val handler = Handler(Looper.getMainLooper())
    private var showAnalog = false
    
    companion object {
        const val PREFS_NAME = "DemantiaClockPrefs"
        const val THEME_KEY = "selected_theme"
        const val DEFAULT_THEME = "white_gray"
    }

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("DemantiaClockX", "*** onCreate STARTED ***")
        System.out.println("*** SYSTEM OUT: onCreate STARTED ***")
        super.onCreate(savedInstanceState)
        
        // Tam ekran modunu etkinleştir
        enableFullscreen()
        
        // Otomatik başlatma kontrolü
        enableAutoStart()
        
        // SharedPreferences'ı başlat
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // UpdateManager'ı başlat
        updateManager = UpdateManager.getInstance(this)
        

        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fullscreen-ish (hide action bar if any)
        supportActionBar?.hide()
        
        // Temayı uygula
        applyTheme()

        // Başlangıç durumunu ayarla (dijital mod)
        android.util.Log.d("DemantiaClockX", "onCreate: calling applyToggleState")
        applyToggleState()
        
        // İlk açılışta saat bilgilerini göster
        android.util.Log.d("DemantiaClockX", "onCreate: calling updateClock")
        updateClock()

        android.util.Log.d("DemantiaClockX", "Setting up toggle button listener")
        android.util.Log.d("DemantiaClockX", "Toggle button is null: ${binding.btnToggle == null}")
        android.util.Log.d("DemantiaClockX", "Toggle button visibility: ${binding.btnToggle.visibility}")
        android.util.Log.d("DemantiaClockX", "Toggle button isClickable: ${binding.btnToggle.isClickable}")
        android.util.Log.d("DemantiaClockX", "Toggle button isEnabled: ${binding.btnToggle.isEnabled}")
        binding.btnToggle.setOnClickListener {
            android.util.Log.d("DemantiaClockX", "*** TOGGLE BUTTON CLICKED! ***")
            System.out.println("*** SYSTEM OUT: TOGGLE BUTTON CLICKED! ***")
            android.util.Log.d("DemantiaClockX", "Toggle button clicked! Current showAnalog: $showAnalog")
            showAnalog = !showAnalog
            android.util.Log.d("DemantiaClockX", "Toggle button clicked! New showAnalog: $showAnalog")
            applyToggleState()
            android.util.Log.d("DemantiaClockX", "*** TOGGLE BUTTON CLICK COMPLETE! ***")
        }
        android.util.Log.d("DemantiaClockX", "Toggle button listener set up complete")
        
        // Settings butonu listener'ı
        binding.btnSettings?.setOnClickListener {
            android.util.Log.d("DemantiaClockX", "Settings button clicked")
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Test butonu listener'ı - BootReceiver'ı test et
        binding.btnTest?.setOnClickListener {
            android.util.Log.d("DemantiaClockX", "Test button clicked - Testing BootReceiver")
            testBootReceiver()
        }
        
        // Update Test butonu listener'ı - Update kontrolünü test et
        binding.btnUpdateTest?.setOnClickListener {
            android.util.Log.d("DemantiaClockX", "Update Test button clicked - Testing update check and broadcast receiver")
            testUpdateCheck()
            // Also test the broadcast receiver
            Handler(Looper.getMainLooper()).postDelayed({
                testBroadcastReceiver()
            }, 2000) // Wait 2 seconds before testing broadcast receiver
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("DemantiaClockX", "onResume: starting clock updates")
        
        // Tam ekran modunu yeniden etkinleştir
        enableFullscreen()
        
        // Tema değişikliği olabilir, yeniden uygula
        applyTheme()
        handler.post(tick)
        
        // Otomatik güncelleme kontrolünü başlat
        updateManager.startPeriodicUpdateCheck()
        
        // İmza hash'ini logla (geliştirme amaçlı)
        updateManager.logCurrentSignatureHash()
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("DemantiaClockX", "onPause: stopping clock updates")
        handler.removeCallbacks(tick)
    }
    
    override fun onStop() {
        super.onStop()
        android.util.Log.d("DemantiaClockX", "onStop: cleaning up resources")
        handler.removeCallbacks(tick)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("DemantiaClockX", "onDestroy: final cleanup")
        
        try {
            // Handler callback'lerini temizle
            handler.removeCallbacks(tick)
            
            // Window flags'leri temizle
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Güncelleme kontrolünü durdur
            updateManager.stopPeriodicUpdateCheck()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during cleanup", e)
        }
    }

    private fun applyToggleState() {
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        android.util.Log.d("DemantiaClockX", "applyToggleState: showAnalog=$showAnalog, isLandscape=$isLandscape")
        
        // SW600dp layout için analogLayout container'ını kontrol et
        val analogLayout = findViewById<android.view.ViewGroup>(R.id.analogLayout)
        val analogElements = findViewById<android.view.ViewGroup>(R.id.analogElements)
        
        if (showAnalog) {
            // Analog modda: analog layout/saat ve analog elementler görünür, dijital elementler gizlenir
            if (analogLayout != null) {
                // SW600dp layout - analogLayout container'ını kullan
                analogLayout.visibility = android.view.View.VISIBLE
                binding.digitalLayout?.visibility = android.view.View.GONE
                android.util.Log.d("DemantiaClockX", "Analog mode (SW600dp): analogLayout visibility = ${analogLayout.visibility}, digitalLayout visibility = ${binding.digitalLayout?.visibility}")
            } else {
                // Diğer layout'lar - eski sistem
                binding.analogClock.visibility = android.view.View.VISIBLE
                binding.digitalLayout?.visibility = android.view.View.GONE
                analogElements?.visibility = android.view.View.VISIBLE
                android.util.Log.d("DemantiaClockX", "Analog mode (other): analogClock visibility = ${binding.analogClock.visibility}, digitalLayout visibility = ${binding.digitalLayout?.visibility}, analogElements visibility = ${analogElements?.visibility}")
            }
        } else {
            // Dijital modda: sadece dijital elementler görünür, analog layout/saat ve analog elementler gizlenir
            if (analogLayout != null) {
                // SW600dp layout - analogLayout container'ını gizle
                analogLayout.visibility = android.view.View.GONE
                binding.digitalLayout?.visibility = android.view.View.VISIBLE
                android.util.Log.d("DemantiaClockX", "Digital mode (SW600dp): analogLayout visibility = ${analogLayout.visibility}, digitalLayout visibility = ${binding.digitalLayout?.visibility}")
            } else {
                // Diğer layout'lar - eski sistem
                binding.analogClock.visibility = android.view.View.GONE
                binding.digitalLayout?.visibility = android.view.View.VISIBLE
                analogElements?.visibility = android.view.View.GONE
                android.util.Log.d("DemantiaClockX", "Digital mode (other): analogClock visibility = ${binding.analogClock.visibility}, digitalLayout visibility = ${binding.digitalLayout?.visibility}, analogElements visibility = ${analogElements?.visibility}")
            }
        }
    }


 
    private fun updateClock() {
        val now = Calendar.getInstance(Locale("tr", "TR")).time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("tr", "TR"))
        val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))

        val timeText = timeFormat.format(now)
        val dayText = dayFormat.format(now).replaceFirstChar { it.titlecase(Locale("tr", "TR")) }
        val dateText = dateFormat.format(now)
        val timeOfDayText = timeOfDayLabel()

        // Dijital elementleri güncelle
        binding.tvTime.text = timeText
        binding.tvDate.text = dayText
        binding.tvDate2.text = dateText
        binding.tvTimeOfDay.text = timeOfDayText
        
        // Analog elementleri findViewById ile güncelle
        findViewById<android.widget.TextView>(R.id.tvTimeAnalog)?.text = timeText
        findViewById<android.widget.TextView>(R.id.tvDateAnalog)?.text = dayText
        findViewById<android.widget.TextView>(R.id.tvDate2Analog)?.text = dateText
        findViewById<android.widget.TextView>(R.id.tvTimeOfDayAnalog)?.text = timeOfDayText
        
        // Debug log - commented out to reduce log noise
        // android.util.Log.d("DemantiaClockX", "Clock updated: $timeText, $dateText, $timeOfDayText")
        // android.util.Log.d("DemantiaClockX", "Digital TextView visibilities - tvDate: ${binding.tvDate.visibility}, tvTime: ${binding.tvTime.visibility}, tvTimeOfDay: ${binding.tvTimeOfDay.visibility}, tvDate2: ${binding.tvDate2.visibility}")
        
        // Analog elementleri findViewById ile log - commented out to reduce log noise
        // val tvDateAnalog = findViewById<android.widget.TextView>(R.id.tvDateAnalog)
        // val tvTimeAnalog = findViewById<android.widget.TextView>(R.id.tvTimeAnalog)
        // val tvTimeOfDayAnalog = findViewById<android.widget.TextView>(R.id.tvTimeOfDayAnalog)
        // val tvDate2Analog = findViewById<android.widget.TextView>(R.id.tvDate2Analog)
        // android.util.Log.d("DemantiaClockX", "Analog TextView visibilities - tvDateAnalog: ${tvDateAnalog?.visibility}, tvTimeAnalog: ${tvTimeAnalog?.visibility}, tvTimeOfDayAnalog: ${tvTimeOfDayAnalog?.visibility}, tvDate2Analog: ${tvDate2Analog?.visibility}")
    }

    private fun timeOfDayLabel(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> getString(R.string.morning)     // Sabah
            in 11..13 -> getString(R.string.noon)       // Öğle
            in 14..17 -> getString(R.string.afternoon)  // İkindi
            in 18..21 -> getString(R.string.evening)    // Akşam
            else -> getString(R.string.night)           // Gece
        }
    }
    
    private fun applyTheme() {
        val selectedTheme = sharedPreferences.getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
        android.util.Log.d("DemantiaClockX", "Applying theme: $selectedTheme")
        
        val (backgroundColor, textColor) = when (selectedTheme) {
            "white_gray" -> Pair(R.color.theme_white_gray, R.color.text_dark)
            "light_blue" -> Pair(R.color.theme_light_blue, R.color.text_dark)
            "light_yellow" -> Pair(R.color.theme_light_yellow, R.color.text_dark)
            "light_pink" -> Pair(R.color.theme_light_pink, R.color.text_dark)
            "red" -> Pair(R.color.theme_red, R.color.text_dark)
            "navy_blue" -> Pair(R.color.theme_navy_blue, R.color.text_light)
            "black" -> Pair(R.color.theme_black, R.color.text_light)
            else -> Pair(R.color.theme_white_gray, R.color.text_dark)
        }
        
        // Ana layout'un arka plan rengini değiştir
        binding.root.setBackgroundColor(ContextCompat.getColor(this, backgroundColor))
        
        // Tüm text view'ların rengini değiştir
        val textColorValue = ContextCompat.getColor(this, textColor)
        
        // Dijital elementler
        binding.tvTime.setTextColor(textColorValue)
        binding.tvDate.setTextColor(textColorValue)
        binding.tvDate2.setTextColor(textColorValue)
        binding.tvTimeOfDay.setTextColor(textColorValue)
        
        // Analog elementler
        findViewById<android.widget.TextView>(R.id.tvTimeAnalog)?.setTextColor(textColorValue)
        findViewById<android.widget.TextView>(R.id.tvDateAnalog)?.setTextColor(textColorValue)
        findViewById<android.widget.TextView>(R.id.tvDate2Analog)?.setTextColor(textColorValue)
        findViewById<android.widget.TextView>(R.id.tvTimeOfDayAnalog)?.setTextColor(textColorValue)
        
        android.util.Log.d("DemantiaClockX", "Theme applied: background=$backgroundColor, text=$textColor")
    }
    
    private fun enableFullscreen() {
        try {
            // Ekranı sürekli açık tut
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Tam ekran için sistem UI'sini gizle
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            
            Log.d("MainActivity", "Fullscreen mode enabled successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error enabling fullscreen mode", e)
            // Hata durumunda basit tam ekran moduna geç
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } catch (fallbackError: Exception) {
                Log.e("MainActivity", "Fallback fullscreen also failed", fallbackError)
            }
        }
    }
    
    private fun enableAutoStart() {
        try {
            // BootReceiver'ın etkin olduğundan emin ol
            val componentName = ComponentName(this, BootReceiver::class.java)
            val packageManager = packageManager
            
            val currentState = packageManager.getComponentEnabledSetting(componentName)
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.d("MainActivity", "BootReceiver enabled for auto-start")
            } else {
                Log.d("MainActivity", "BootReceiver already enabled")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to enable auto-start: ${e.message}", e)
        }
    }
    
    private fun testBootReceiver() {
        try {
            Log.d("MainActivity", "Testing BootReceiver and AutoStartService...")
            
            // Önce battery optimization kontrolü yap
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Log.w("MainActivity", "App is not whitelisted from battery optimization")
                    Toast.makeText(this, "Uyarı: Pil optimizasyonu aktif", Toast.LENGTH_LONG).show()
                }
            }
            
            // AutoStartService'i doğrudan test et
            val serviceIntent = Intent(this, AutoStartService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d("MainActivity", "AutoStartService started as foreground service for testing")
            } else {
                startService(serviceIntent)
                Log.d("MainActivity", "AutoStartService started for testing")
            }
            
            // BootReceiver'ı manuel olarak tetikle
            val testIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
            val bootReceiver = BootReceiver()
            bootReceiver.onReceive(this, testIntent)
            
            Toast.makeText(this, "BootReceiver ve AutoStartService test edildi", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "BootReceiver and AutoStartService test completed")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "BootReceiver test failed: ${e.message}", e)
            Toast.makeText(this, "Test başarısız: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkBatteryOptimization() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = packageName
                
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Log.w("MainActivity", "App is NOT whitelisted from battery optimization")
                    Toast.makeText(this, "Pil optimizasyonu aktif - Ayarlara yönlendiriliyor", Toast.LENGTH_LONG).show()
                    
                    // Battery optimization ayarlarını aç
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } else {
                    Log.i("MainActivity", "App is whitelisted from battery optimization")
                    Toast.makeText(this, "Pil optimizasyonu devre dışı ✓", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Battery optimization check failed: ${e.message}", e)
        }
    }
    
    /**
     * Test update check manually
     */
    private fun testUpdateCheck() {
        Log.d("MainActivity", "Manual update check triggered")
        Toast.makeText(this, "Güncelleme kontrolü başlatılıyor...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "Calling updateManager.checkForUpdatesManually()")
                updateManager.checkForUpdatesManually()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during manual update check", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Güncelleme kontrolü hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * Test broadcast receiver internally
     */
    private fun testBroadcastReceiver() {
        Log.d("MainActivity", "Testing UpdateBroadcastReceiver internally")
        
        val intent = Intent("com.example.demantiaclockx.UPDATE_ACTION").apply {
            putExtra("version", "2.1.0")
            putExtra("downloadUrl", "https://example.com/internal-test.apk")
            putExtra("releaseNotes", "Internal broadcast test")
            setPackage(packageName) // Ensure it stays within our app
        }
        
        Log.d("MainActivity", "Sending internal broadcast...")
        sendBroadcast(intent)
        Log.d("MainActivity", "Internal broadcast sent")
    }

    /**
     * Request notification permission for Android 13+
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "Requesting notification permission...")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Log.d("MainActivity", "Notification permission already granted")
            }
        } else {
            Log.d("MainActivity", "Android version < 13, notification permission not required")
        }
    }
}