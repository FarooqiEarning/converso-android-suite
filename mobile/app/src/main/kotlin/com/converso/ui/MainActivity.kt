package com.converso.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.converso.R
import com.converso.utils.Config

/**
 * MainActivity - Application Entry Point
 * Handles initial routing based on registration and permission status
 */
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        routeUser()
    }
    
    private fun routeUser() {
        val serverUrl = Config.getServerUrl(this)
        
        // Not registered - show QR scanner
        if (serverUrl.isNullOrEmpty()) {
            navigateToScanner()
            return
        }
        
        // Registered but missing permissions
        if (!hasRequiredPermissions()) {
            navigateToPermissions()
            return
        }
        
        // All set - go to status screen
        navigateToStatus()
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() && isNotificationServiceEnabled()
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/com.converso.service.ConversoAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver, 
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, 
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }
    
    private fun navigateToScanner() {
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }
    
    private fun navigateToPermissions() {
        startActivity(Intent(this, PermissionsActivity::class.java))
        finish()
    }
    
    private fun navigateToStatus() {
        startActivity(Intent(this, StatusActivity::class.java))
        finish()
    }
}