package com.converso.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.converso.R
import com.converso.utils.PermissionUtils
import com.converso.utils.PowerUtils

class PermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        findViewById<Button>(R.id.btnPower).setOnClickListener {
            PowerUtils.requestIgnoreBatteryOptimizations(this)
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            if (isAllReady()) {
                startActivity(Intent(this, StatusActivity::class.java))
                finish()
            }
        }
    }

    private fun isAllReady(): Boolean {
        return PermissionUtils.isAccessibilityServiceEnabled(this, com.converso.service.ConversoAccessibilityService::class.java) &&
               PermissionUtils.isNotificationServiceEnabled(this)
    }
}
