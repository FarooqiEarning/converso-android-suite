package com.converso.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import android.media.projection.MediaProjectionManager
import com.converso.stream.ConversoStreamer

class ForegroundService : Service() {
    private val CHANNEL_ID = "ConversoForeground"
    private var streamer: ConversoStreamer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converso Active")
            .setContentText("Remote management is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_STREAM") {
            val resultCode = intent.getIntExtra("RESULT_CODE", 0)
            val data = intent.getParcelableExtra<Intent>("DATA")
            if (data != null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, data)
                
                streamer = ConversoStreamer(this, projection) { frame ->
                    // Logic to send frame back to BackgroundService / EventBus
                    // For now, assume it communicates via a shared WebSocket in BackgroundService
                    // Or we could have a dedicated stream socket
                }
                streamer?.start()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        streamer?.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Converso Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
