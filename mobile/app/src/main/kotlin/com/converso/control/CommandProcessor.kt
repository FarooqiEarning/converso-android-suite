package com.converso.control

import android.content.Context
import android.util.Log
import com.converso.service.ConversoAccessibilityService
import org.json.JSONObject

/**
 * CommandProcessor
 * Dispatches commands from the backend to local hardware/accessibility services.
 */
class CommandProcessor(private val context: Context) {

    private val hardwareController = HardwareController(context)

    fun process(rawCommand: String) {
        try {
            val json = JSONObject(rawCommand)
            val type = json.optString("type")
            val params = json.optJSONObject("params") ?: JSONObject()

            Log.i("CommandProc", "Processing: $type")

            when (type) {
                "CLICK" -> {
                    val x = params.optDouble("x", 0.0).toFloat()
                    val y = params.optDouble("y", 0.0).toFloat()
                    ConversoAccessibilityService.instance?.performClick(x, y)
                }
                "SWIPE" -> {
                    val x1 = params.optDouble("x1", 0.0).toFloat()
                    val y1 = params.optDouble("y1", 0.0).toFloat()
                    val x2 = params.optDouble("x2", 0.0).toFloat()
                    val y2 = params.optDouble("y2", 0.0).toFloat()
                    ConversoAccessibilityService.instance?.performSwipe(x1, y1, x2, y2)
                }
                "WIFI" -> {
                    val enabled = params.optBoolean("enabled", false)
                    hardwareController.setWifiEnabled(enabled)
                }
                "BLUETOOTH" -> {
                    val enabled = params.optBoolean("enabled", false)
                    hardwareController.setBluetoothEnabled(enabled)
                }
                // More enterprise commands (REBOOT, INSTALL_APP, etc.)
            }
        } catch (e: Exception) {
            Log.e("CommandProc", "Command parse error", e)
        }
    }
}
