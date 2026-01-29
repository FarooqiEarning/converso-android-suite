package com.converso.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.converso.R
import com.converso.stream.ConversoStreamer

/**
 * ForegroundService - Screen Capture Service
 * Manages MediaProjection and screen streaming
 */
class ForegroundService : Service() {
    
    private val TAG = "ConversoForeground"
    private val CHANNEL_ID = "ConversoForeground"
    private val NOTIFICATION_ID = 1
    
    private var streamer: ConversoStreamer? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand with action: ${intent?.action}")
        
        when (intent?.action) {
            "START_STREAM" -> handleStartStream(intent)
            "STOP_STREAM" -> handleStopStream()
            else -> startForegroundWithNotification()
        }
        
        return START_STICKY
    }
    
    private fun handleStartStream(intent: Intent) {
        val resultCode = intent.getIntExtra("RESULT_CODE", 0)
        val data = intent.getParcelableExtra<Intent>("DATA")
        
        if (data != null && resultCode != 0) {
            try {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, data)
                
                if (projection != null) {
                    streamer = ConversoStreamer(this, projection) { frame ->
                        // Send frame via WebSocket through BackgroundService
                        sendFrameToBackgroundService(frame)
                    }
                    streamer?.start()
                    
                    startForegroundWithNotification("Streaming Active")
                    Log.i(TAG, "Screen streaming started")
                } else {
                    Log.e(TAG, "Failed to get MediaProjection")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting stream", e)
            }
        } else {
            Log.e(TAG, "Invalid stream start parameters")
        }
    }
    
    private fun handleStopStream() {
        streamer?.stop()
        streamer = null
        stopForeground(true)
        stopSelf()
        Log.i(TAG, "Screen streaming stopped")
    }
    
    private fun sendFrameToBackgroundService(frame: String) {
        val intent = Intent("com.converso.SCREEN_FRAME").apply {
            putExtra("frame", frame)
        }
        sendBroadcast(intent)
    }

    private fun startForegroundWithNotification(contentText: String = "Remote management is running") {
        val notification = createNotification(contentText)
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converso Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Converso Service"
            val descriptionText = "Manages remote device control"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        streamer?.stop()
        streamer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}