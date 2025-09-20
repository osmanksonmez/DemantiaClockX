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
            val packageInfo = packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNATURES
            ) ?: return SecurityCheckResult.Failed("APK bilgileri okunamadı")
            
            val signatures = packageInfo.signatures
            if (signatures.isNullOrEmpty()) {
                return SecurityCheckResult.Failed("APK imzası bulunamadı")
            }
            
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
            
            return SecurityCheckResult.Passed
            
        } catch (e: Exception) {
            Log.e(TAG, "İmza doğrulama hatası", e)
            return SecurityCheckResult.Failed("İmza doğrulama başarısız")
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
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            packageInfo.signatures ?: emptyArray()
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