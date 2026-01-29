package com.converso.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.converso.R
import com.converso.service.ConversoAccessibilityService
import com.converso.utils.PowerUtils

/**
 * PermissionsActivity - Permission Setup Screen
 * Guides user through granting required system permissions
 */
class PermissionsActivity : AppCompatActivity() {

    private lateinit var btnAccessibility: Button
    private lateinit var btnNotifications: Button
    private lateinit var btnPower: Button
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        initializeViews()
        setupClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }
    
    private fun initializeViews() {
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnPower = findViewById(R.id.btnPower)
        btnContinue = findViewById(R.id.btnContinue)
    }
    
    private fun setupClickListeners() {
        btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        btnNotifications.setOnClickListener {
            openNotificationSettings()
        }

        btnPower.setOnClickListener {
            PowerUtils.requestIgnoreBatteryOptimizations(this)
        }

        btnContinue.setOnClickListener {
            if (isAllReady()) {
                navigateToStatus()
            }
        }
    }
    
    private fun updateButtonStates() {
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasNotifications = isNotificationServiceEnabled()
        val hasPower = PowerUtils.isIgnoringBatteryOptimizations(this)
        
        btnAccessibility.text = if (hasAccessibility) "✓ Accessibility Enabled" else "Enable Accessibility"
        btnNotifications.text = if (hasNotifications) "✓ Notifications Enabled" else "Enable Notifications"
        btnPower.text = if (hasPower) "✓ Battery Optimized" else "Disable Power Savings"
        
        btnContinue.isEnabled = isAllReady()
    }
    
    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun openNotificationSettings() {
        try {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAllReady(): Boolean {
        return isAccessibilityServiceEnabled() && 
               isNotificationServiceEnabled() &&
               PowerUtils.isIgnoringBatteryOptimizations(this)
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${ConversoAccessibilityService::class.java.name}"
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
    
    private fun navigateToStatus() {
        startActivity(Intent(this, StatusActivity::class.java))
        finish()
    }
}