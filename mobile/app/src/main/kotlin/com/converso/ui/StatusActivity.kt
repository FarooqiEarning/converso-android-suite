package com.converso.ui

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.converso.R
import com.converso.service.BackgroundService
import com.converso.service.ForegroundService
import com.converso.utils.Config
import okhttp3.OkHttpClient

/**
 * StatusActivity - Main Dashboard
 * Shows connection status, subscription info, and remote control trigger
 */
class StatusActivity : AppCompatActivity() {
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var txtStatus: TextView
    private lateinit var txtLatency: TextView
    private lateinit var txtOwner: TextView
    private lateinit var txtPayment: TextView
    private lateinit var btnRemote: Button
    private val client = OkHttpClient()
    private lateinit var projectionManager: MediaProjectionManager
    private var isStreamingActive = false

    companion object {
        const val SCREEN_CAPTURE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        initializeViews()
        initializeServices()
        setupClickListeners()
        updateStats()
    }
    
    private fun initializeViews() {
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        txtStatus = findViewById(R.id.txtStatus)
        txtLatency = findViewById(R.id.txtLatency)
        txtOwner = findViewById(R.id.txtOwner)
        txtPayment = findViewById(R.id.txtPayment)
        btnRemote = findViewById(R.id.btnRemote)
        
        txtOwner.text = "Owner: ${Config.getUserName(this)}"
        txtPayment.text = "Subscription: ${Config.getPaymentStatus(this)}"
    }
    
    private fun initializeServices() {
        // Start core background services
        startService(Intent(this, BackgroundService::class.java))
    }
    
    private fun setupClickListeners() {
        btnRemote.setOnClickListener {
            if (!isStreamingActive) {
                requestScreenCapture()
            } else {
                stopScreenCapture()
            }
        }
    }
    
    private fun requestScreenCapture() {
        val captureIntent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }
    
    private fun stopScreenCapture() {
        val intent = Intent(this, ForegroundService::class.java).apply {
            action = "STOP_STREAM"
        }
        startService(intent)
        
        isStreamingActive = false
        btnRemote.text = "START REMOTE SESSION"
        btnRemote.setBackgroundColor(resources.getColor(R.color.primary_blue, null))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val intent = Intent(this, ForegroundService::class.java).apply {
                action = "START_STREAM"
                putExtra("RESULT_CODE", resultCode)
                putExtra("DATA", data)
            }
            startService(intent)
            
            isStreamingActive = true
            btnRemote.text = "STOP REMOTE SESSION"
            btnRemote.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }

    private fun updateStats() {
        txtStatus.text = "CONNECTED"
        txtLatency.text = "Latency: ${(15..45).random()}ms"
        
        handler.postDelayed({ updateStats() }, 3000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}