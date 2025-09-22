package com.example.demantiaclockx.update

/**
 * Configuration class for update system
 */
object UpdateConfig {
    
    // GitHub repository configuration
    const val GITHUB_OWNER = "osmanksonmez"
    const val GITHUB_REPO = "DemantiaClockX"
    
    // API endpoints
    const val GITHUB_API_BASE = "https://api.github.com"
    const val RELEASES_ENDPOINT = "$GITHUB_API_BASE/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    
    // Network timeouts (in milliseconds)
    const val CONNECT_TIMEOUT = 15000
    const val READ_TIMEOUT = 15000
    
    // Update check settings
    const val AUTO_CHECK_ENABLED = true
    const val CHECK_INTERVAL_HOURS = 24
    
    // Security settings
    const val VERIFY_APK_SIGNATURE = true
    const val ALLOW_DOWNGRADE = false
    
    // User Agent for API requests
    const val USER_AGENT = "DemantiaClockX-UpdateChecker/1.0"
    
    /**
     * Get the full GitHub releases API URL
     */
    fun getReleasesApiUrl(): String = RELEASES_ENDPOINT
    
    /**
     * Check if automatic update checking is enabled
     */
    fun isAutoCheckEnabled(): Boolean = AUTO_CHECK_ENABLED
    
    /**
     * Get the update check interval in milliseconds
     */
    fun getCheckIntervalMs(): Long = CHECK_INTERVAL_HOURS * 60 * 60 * 1000L
}