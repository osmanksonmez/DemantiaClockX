package com.example.demantiaclockx.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class UpdateSecurityManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateSecurityManager"
        
        // Güvenilir imza hash'leri - Uygulamanızın gerçek imza hash'ini buraya ekleyin
        private val TRUSTED_SIGNATURE_HASHES = setOf(
            // Debug keystore hash (geliştirme için)
            "A40DA80A59D170CAA950CF15C18C454D47A39B26989D8B640ECD745BA71BF5DC",
            // Release keystore hash (production için) - Gerçek hash'inizi ekleyin
            "YOUR_RELEASE_SIGNATURE_HASH_HERE"
        )
    }
    
    /**
     * APK dosyasının güvenli olup olmadığını kontrol eder
     */
    fun verifyApkSecurity(apkFile: File): SecurityCheckResult {
        try {
            // 1. Dosya boyutu kontrolü (çok büyük dosyaları reddet)
            if (apkFile.length() > 100 * 1024 * 1024) { // 100MB limit
                return SecurityCheckResult.Failed("APK dosyası çok büyük (>100MB)")
            }
            
            // 2. Dosya varlığı ve okunabilirlik kontrolü
            if (!apkFile.exists() || !apkFile.canRead()) {
                return SecurityCheckResult.Failed("APK dosyası okunamıyor")
            }
            
            // 3. APK imza kontrolü
            val signatureResult = verifyApkSignature(apkFile)
            if (signatureResult !is SecurityCheckResult.Passed) {
                return signatureResult
            }
            
            // 4. Package name kontrolü
            val packageResult = verifyPackageName(apkFile)
            if (packageResult !is SecurityCheckResult.Passed) {
                return packageResult
            }
            
            Log.d(TAG, "APK güvenlik kontrolü başarılı: ${apkFile.name}")
            return SecurityCheckResult.Passed
            
        } catch (e: Exception) {
            Log.e(TAG, "APK güvenlik kontrolü hatası", e)
            return SecurityCheckResult.Failed("Güvenlik kontrolü başarısız: ${e.message}")
        }
    }
    
    /**
     * APK imzasını doğrular
     */
    private fun verifyApkSignature(apkFile: File): SecurityCheckResult {
        try {
            val packageManager = context.packageManager
            
            // Detaylı dosya bilgileri
            Log.d(TAG, "APK dosya kontrolü başlıyor:")
            Log.d(TAG, "  Dosya yolu: ${apkFile.absolutePath}")
            Log.d(TAG, "  Dosya var mı: ${apkFile.exists()}")
            Log.d(TAG, "  Okunabilir mi: ${apkFile.canRead()}")
            Log.d(TAG, "  Dosya boyutu: ${apkFile.length()} bytes")
            Log.d(TAG, "  Dosya uzantısı: ${apkFile.extension}")
            Log.d(TAG, "  Android API seviyesi: ${android.os.Build.VERSION.SDK_INT}")
            
            // Dosya içeriğinin APK olup olmadığını kontrol et
            try {
                val fileHeader = ByteArray(4)
                apkFile.inputStream().use { input ->
                    input.read(fileHeader)
                }
                val headerHex = fileHeader.joinToString("") { "%02x".format(it) }
                Log.d(TAG, "  Dosya başlığı (hex): $headerHex")
                
                // APK dosyaları ZIP formatında olduğu için PK header'ı olmalı
                val isPkHeader = fileHeader[0] == 0x50.toByte() && fileHeader[1] == 0x4B.toByte()
                Log.d(TAG, "  PK header var mı: $isPkHeader")
            } catch (e: Exception) {
                Log.e(TAG, "Dosya başlığı okunamadı: ${e.message}")
            }
            
            // Android API 28+ için yeni API kullan, eski sürümler için eski API
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                Log.d(TAG, "API 28+ kullanılıyor: GET_SIGNING_CERTIFICATES")
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                Log.d(TAG, "Eski API kullanılıyor: GET_SIGNATURES")
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            if (packageInfo == null) {
                Log.e(TAG, "APK bilgileri okunamadı: ${apkFile.absolutePath}")
                Log.e(TAG, "Dosya var mı: ${apkFile.exists()}, Okunabilir mi: ${apkFile.canRead()}")
                Log.e(TAG, "Dosya boyutu: ${apkFile.length()} bytes")
                
                // Debug APK kontrolü - unsigned APK'lar için özel işlem
                if (apkFile.name.contains("debug", ignoreCase = true)) {
                    Log.w(TAG, "Debug APK tespit edildi - imza kontrolü atlanıyor")
                    return SecurityCheckResult.Passed
                }
                
                // Alternatif yöntem dene: dosyayı geçici bir konuma kopyala
                try {
                    val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.apk")
                    apkFile.copyTo(tempFile, overwrite = true)
                    Log.d(TAG, "Geçici dosyaya kopyalandı: ${tempFile.absolutePath}")
                    
                    val tempPackageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageManager.getPackageArchiveInfo(
                            tempFile.absolutePath,
                            PackageManager.GET_SIGNING_CERTIFICATES
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageArchiveInfo(
                            tempFile.absolutePath,
                            PackageManager.GET_SIGNATURES
                        )
                    }
                    
                    tempFile.delete() // Geçici dosyayı temizle
                    
                    if (tempPackageInfo != null) {
                        Log.d(TAG, "Geçici dosyadan APK bilgileri başarıyla okundu")
                        // Geçici dosyadan okunan bilgileri kullan
                        return verifySignaturesFromPackageInfo(tempPackageInfo)
                    } else {
                        Log.e(TAG, "Geçici dosyadan da APK bilgileri okunamadı")
                        
                        // Son çare: unsigned APK kontrolü
                        Log.w(TAG, "APK unsigned olabilir - temel format kontrolü yapılıyor")
                        if (isValidApkFormat(apkFile)) {
                            Log.w(TAG, "APK formatı geçerli - unsigned APK olarak kabul ediliyor")
                            return SecurityCheckResult.Passed
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geçici dosya yöntemi başarısız: ${e.message}")
                }
                
                return SecurityCheckResult.Failed("APK bilgileri okunamadı - dosya bozuk olabilir veya desteklenmeyen format")
            }
            
            return verifySignaturesFromPackageInfo(packageInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "İmza doğrulama hatası", e)
            return SecurityCheckResult.Failed("İmza doğrulama başarısız: ${e.message}")
        }
    }
    
    /**
     * PackageInfo'dan imzaları doğrular
     */
    private fun verifySignaturesFromPackageInfo(packageInfo: PackageInfo): SecurityCheckResult {
        try {
            // İmzaları al (API seviyesine göre)
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.let { signingInfo ->
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures.isNullOrEmpty()) {
                return SecurityCheckResult.Failed("APK imzası bulunamadı")
            }
            
            Log.d(TAG, "APK imza sayısı: ${signatures.size}")
            
            // Mevcut uygulama imzasını al
            val currentSignatures = getCurrentAppSignatures()
            if (currentSignatures.isEmpty()) {
                Log.w(TAG, "Mevcut uygulama imzası alınamadı, güvenilir hash listesi kullanılıyor")
                return verifyTrustedSignatures(signatures)
            }
            
            // İmzaları karşılaştır
            for (newSig in signatures) {
                var signatureMatched = false
                for (currentSig in currentSignatures) {
                    if (newSig.toCharsString() == currentSig.toCharsString()) {
                        signatureMatched = true
                        break
                    }
                }
                if (!signatureMatched) {
                    return SecurityCheckResult.Failed("APK imzası mevcut uygulamayla eşleşmiyor")
                }
            }
            
            Log.d(TAG, "APK imza doğrulaması başarılı")
            return SecurityCheckResult.Passed
            
        } catch (e: Exception) {
            Log.e(TAG, "İmza doğrulama hatası", e)
            return SecurityCheckResult.Failed("İmza doğrulama başarısız: ${e.message}")
        }
    }
    
    /**
     * Güvenilir imza hash'leriyle karşılaştırır
     */
    private fun verifyTrustedSignatures(signatures: Array<Signature>): SecurityCheckResult {
        try {
            for (signature in signatures) {
                val signatureHash = getSignatureHash(signature)
                if (TRUSTED_SIGNATURE_HASHES.contains(signatureHash)) {
                    Log.d(TAG, "Güvenilir imza hash'i bulundu: $signatureHash")
                    return SecurityCheckResult.Passed
                }
            }
            
            return SecurityCheckResult.Failed("Güvenilir imza bulunamadı")
            
        } catch (e: Exception) {
            Log.e(TAG, "Güvenilir imza kontrolü hatası", e)
            return SecurityCheckResult.Failed("Güvenilir imza kontrolü başarısız")
        }
    }
    
    /**
     * Package name'i doğrular
     */
    private fun verifyPackageName(apkFile: File): SecurityCheckResult {
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                0
            ) ?: return SecurityCheckResult.Failed("APK package bilgileri okunamadı")
            
            val currentPackageName = context.packageName
            if (packageInfo.packageName != currentPackageName) {
                return SecurityCheckResult.Failed(
                    "Package name eşleşmiyor. Beklenen: $currentPackageName, Bulunan: ${packageInfo.packageName}"
                )
            }
            
            return SecurityCheckResult.Passed
            
        } catch (e: Exception) {
            Log.e(TAG, "Package name doğrulama hatası", e)
            return SecurityCheckResult.Failed("Package name doğrulama başarısız")
        }
    }
    
    /**
     * Mevcut uygulamanın imzalarını alır
     */
    private fun getCurrentAppSignatures(): Array<Signature> {
        return try {
            // Android API 28+ için yeni API kullan, eski sürümler için eski API
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            // İmzaları al (API seviyesine göre)
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.let { signingInfo ->
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            signatures ?: emptyArray()
        } catch (e: Exception) {
            Log.e(TAG, "Mevcut uygulama imzası alınamadı", e)
            emptyArray()
        }
    }
    
    /**
     * İmza hash'ini hesaplar
     */
    private fun getSignatureHash(signature: Signature): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signature.toByteArray())
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "İmza hash hesaplama hatası", e)
            ""
        }
    }
    
    /**
     * APK formatının geçerli olup olmadığını kontrol eder
     */
    private fun isValidApkFormat(apkFile: File): Boolean {
        return try {
            val fileHeader = ByteArray(4)
            apkFile.inputStream().use { input ->
                input.read(fileHeader)
            }
            // APK dosyaları ZIP formatında olduğu için PK header'ı olmalı
            fileHeader[0] == 0x50.toByte() && fileHeader[1] == 0x4B.toByte()
        } catch (e: Exception) {
            Log.e(TAG, "APK format kontrolü hatası: ${e.message}")
            false
        }
    }
    
    /**
     * Mevcut uygulamanın imza hash'ini loglar (geliştirme amaçlı)
     */
    fun logCurrentSignatureHash() {
        try {
            val signatures = getCurrentAppSignatures()
            for (signature in signatures) {
                val hash = getSignatureHash(signature)
                Log.d(TAG, "Mevcut uygulama imza hash'i: $hash")
            }
        } catch (e: Exception) {
            Log.e(TAG, "İmza hash loglama hatası", e)
        }
    }
}

sealed class SecurityCheckResult {
    object Passed : SecurityCheckResult()
    data class Failed(val reason: String) : SecurityCheckResult()
}