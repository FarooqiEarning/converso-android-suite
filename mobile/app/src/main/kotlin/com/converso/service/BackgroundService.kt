package com.converso.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.converso.utils.Config
import okhttp3.*
import java.util.concurrent.TimeUnit

class BackgroundService : Service() {
    private val TAG = "ConversoBG"
    private lateinit var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private var isClosing = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service Created")
        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting WebSocket Link")
        connectWebSocket()
        return START_STICKY
    }

    private fun connectWebSocket() {
        if (isClosing) return
        
        val url = Config.getWebSocketUrl(this)
        Log.d(TAG, "Connecting to $url")
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Relay")
                // Register device with hardware ID
                val registration = """{"type":"DEVICE", "deviceId":"${android.os.Build.ID}"}"""
                webSocket.send(registration)
                startTelemetryLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Command Received: $text")
                val processor = com.converso.control.CommandProcessor(this@BackgroundService)
                processor.process(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}. Retrying in 5s...")
                if (!isClosing) {
                    // Primitive exponential backoff could be added here
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connectWebSocket()
                    }, 5000)
                }
            }
        })
    }

    private fun startTelemetryLoop() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val hardwareController = com.converso.control.HardwareController(this)
        
        handler.post(object : Runnable {
            override fun run() {
                if (webSocket != null) {
                    val stats = hardwareController.getSystemStats()
                    val payload = """
                        {
                            "type": "TELEMETRY",
                            "deviceId": "${android.os.Build.ID}",
                            "batteryLevel": ${stats["batteryLevel"]},
                            "cpuUsage": ${stats["cpuUsage"]},
                            "ramUsedMb": ${stats["ramUsedMb"]},
                            "ramTotalMb": ${stats["ramTotalMb"]}
                        }
                    """.trimIndent()
                    webSocket?.send(payload)
                }
                handler.postDelayed(this, 10000) // Every 10 seconds
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        isClosing = true
        webSocket?.close(1000, "Service destroyed")
        super.onDestroy()
    }
}
