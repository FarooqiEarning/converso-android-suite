package com.converso.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.converso.control.CommandProcessor
import com.converso.control.HardwareController
import com.converso.utils.Config
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * BackgroundService - WebSocket Communication Service
 * Maintains persistent connection to backend and handles commands
 */
class BackgroundService : Service() {
    
    private val TAG = "ConversoBG"
    private lateinit var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private var isClosing = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var hardwareController: HardwareController
    
    private val frameReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val frame = intent?.getStringExtra("frame")
            if (frame != null && webSocket != null) {
                sendScreenFrame(frame)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service Created")
        
        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
            
        commandProcessor = CommandProcessor(this)
        hardwareController = HardwareController(this)
        
        // Register frame receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(frameReceiver, IntentFilter("com.converso.SCREEN_FRAME"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(frameReceiver, IntentFilter("com.converso.SCREEN_FRAME"))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting WebSocket Link")
        connectWebSocket()
        return START_STICKY
    }

    private fun connectWebSocket() {
        if (isClosing || webSocket != null) return
        
        val url = Config.getWebSocketUrl(this)
        if (url.isNullOrEmpty()) {
            Log.e(TAG, "WebSocket URL not configured")
            return
        }
        
        Log.d(TAG, "Connecting to $url")
        
        val deviceId = Build.ID
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Relay")
                reconnectAttempts = 0
                
                // Register device
                val registration = JSONObject().apply {
                    put("type", "DEVICE")
                    put("deviceId", deviceId)
                    put("manufacturer", Build.MANUFACTURER)
                    put("model", Build.MODEL)
                    put("androidVersion", Build.VERSION.RELEASE)
                }
                webSocket.send(registration.toString())
                
                // Start telemetry
                startTelemetryLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Command Received: $text")
                handleCommand(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Connection closed: $reason")
                this@BackgroundService.webSocket = null
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}")
                this@BackgroundService.webSocket = null
                scheduleReconnect()
            }
        })
    }
    
    private fun handleCommand(rawCommand: String) {
        try {
            commandProcessor.process(rawCommand)
            
            // Send acknowledgment
            val result = JSONObject().apply {
                put("type", "COMMAND_RESULT")
                put("deviceId", Build.ID)
                put("status", "SUCCESS")
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(result.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Command processing error", e)
            
            val error = JSONObject().apply {
                put("type", "COMMAND_RESULT")
                put("deviceId", Build.ID)
                put("status", "ERROR")
                put("error", e.message)
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(error.toString())
        }
    }
    
    private fun sendScreenFrame(frame: String) {
        val payload = JSONObject().apply {
            put("type", "SCREEN_FRAME")
            put("deviceId", Build.ID)
            put("frame", frame)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(payload.toString())
    }

    private fun startTelemetryLoop() {
        android.os.Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (webSocket != null && !isClosing) {
                    sendTelemetry()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 10000)
                }
            }
        })
    }
    
    private fun sendTelemetry() {
        try {
            val stats = hardwareController.getSystemStats()
            val payload = JSONObject().apply {
                put("type", "TELEMETRY")
                put("deviceId", Build.ID)
                put("batteryLevel", stats["batteryLevel"])
                put("cpuUsage", stats["cpuUsage"])
                put("ramUsedMb", stats["ramUsedMb"])
                put("ramTotalMb", stats["ramTotalMb"])
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send(payload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Telemetry send error", e)
        }
    }
    
    private fun scheduleReconnect() {
        if (isClosing || reconnectAttempts >= maxReconnectAttempts) {
            Log.e(TAG, "Max reconnect attempts reached or service closing")
            return
        }
        
        reconnectAttempts++
        val delay = minOf(5000L * reconnectAttempts, 30000L)
        
        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delay}ms")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            connectWebSocket()
        }, delay)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        isClosing = true
        
        try {
            unregisterReceiver(frameReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        webSocket?.close(1000, "Service destroyed")
        webSocket = null
        
        super.onDestroy()
    }
}