package com.converso.control

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import java.io.RandomAccessFile

/**
 * HardwareController
 * Provides enterprise-grade control over device hardware with real system metrics
 */
class HardwareController(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    } else {
        @Suppress("DEPRECATION")
        BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * Toggles Wi-Fi state.
     * Note: On Android 10+, this requires special permissions or user interaction
     */
    fun setWifiEnabled(enabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - open settings for user
                val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d("HardwareCtrl", "Opening WiFi settings (Android 10+)")
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = enabled
                Log.d("HardwareCtrl", "Wi-Fi toggle requested: $enabled")
            }
        } catch (e: Exception) {
            Log.e("HardwareCtrl", "Failed to toggle Wi-Fi", e)
        }
    }

    /**
     * Toggles Bluetooth state.
     */
    fun setBluetoothEnabled(enabled: Boolean) {
        bluetoothAdapter?.let {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ requires BLUETOOTH_CONNECT permission
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d("HardwareCtrl", "Opening Bluetooth settings (Android 12+)")
                } else {
                    @Suppress("DEPRECATION")
                    if (enabled) it.enable() else it.disable()
                    Log.d("HardwareCtrl", "Bluetooth toggle requested: $enabled")
                }
            } catch (e: Exception) {
                Log.e("HardwareCtrl", "Failed to toggle Bluetooth", e)
            }
        }
    }

    /**
     * Retrieves current battery and system levels for telemetry
     */
    fun getSystemStats(): Map<String, Any> {
        return mapOf(
            "batteryLevel" to getBatteryLevel(),
            "cpuUsage" to getCpuUsage(),
            "ramUsedMb" to getUsedRamMb(),
            "ramTotalMb" to getTotalRamMb(),
            "isCharging" to isCharging(),
            "batteryTemperature" to getBatteryTemperature()
        )
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            if (level == -1 || scale == -1) {
                0
            } else {
                (level * 100 / scale.toFloat()).toInt()
            }
        } catch (e: Exception) {
            Log.e("HardwareCtrl", "Error getting battery level", e)
            0
        }
    }
    
    private fun isCharging(): Boolean {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getBatteryTemperature(): Float {
        return try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            temp / 10.0f // Temperature is in tenths of a degree
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getCpuUsage(): Double {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            
            val toks = load.split(" +".toRegex())
            val idle = toks[4].toLong()
            val total = toks.slice(1..7).sumOf { it.toLong() }
            
            val usage = ((total - idle) * 100.0 / total)
            "%.2f".format(usage).toDouble()
        } catch (e: Exception) {
            Log.e("HardwareCtrl", "Error calculating CPU usage", e)
            0.0
        }
    }
    
    private fun getUsedRamMb(): Long {
        return try {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memInfo)
            
            val usedMemory = memInfo.totalMem - memInfo.availMem
            usedMemory / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.e("HardwareCtrl", "Error getting used RAM", e)
            0L
        }
    }
    
    private fun getTotalRamMb(): Long {
        return try {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memInfo)
            
            memInfo.totalMem / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.e("HardwareCtrl", "Error getting total RAM", e)
            0L
        }
    }
}