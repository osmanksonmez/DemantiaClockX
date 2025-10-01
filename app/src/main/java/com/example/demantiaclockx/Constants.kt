package com.example.demantiaclockx

/**
 * Uygulama genelinde kullanılan sabit değerler
 */
object Constants {
    
    /**
     * Uygulama versiyon bilgisi
     * Her versiyon güncellemesinde sadece bu değeri değiştirin
     * Not: build.gradle.kts dosyasındaki versionName ile senkronize tutun
     */
    const val APP_VERSION = "1.5"
    
    /**
     * Versiyon bilgisini "v" prefix'i ile birlikte döndürür
     */
    fun getVersionWithPrefix(): String = "v$APP_VERSION"
}