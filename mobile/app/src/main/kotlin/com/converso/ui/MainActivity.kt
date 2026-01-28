package com.converso.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.converso.utils.Config
import android.text.TextUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val serverUrl = Config.getServerUrl(this)
        
        if (serverUrl == null) {
            startActivity(Intent(this, ScannerActivity::class.java))
            finish()
            return
        }

        // Check if essential enterprise permissions are granted
        if (!isAccessibilityServiceEnabled() || !isNotificationServiceEnabled()) {
            startActivity(Intent(this, PermissionsActivity::class.java))
        } else {
            startActivity(Intent(this, StatusActivity::class.java))
        }
        finish()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "${packageName}/com.converso.service.ConversoAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equalsIgnoreCase(expectedService)) return true
        }
        return false
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun String.equalsIgnoreCase(other: String) = this.equals(other, ignoreCase = true)
}
