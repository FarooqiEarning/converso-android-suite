package com.converso.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.converso.R
import com.converso.utils.Config
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        viewFinder = findViewById(R.id.viewFinder)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRAnalyzer { qrCode ->
                        handleScannedQR(qrCode)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleScannedQR(qrCode: String) {
        if (Config.saveFromQR(this, qrCode)) {
            val token = Config.getRegistrationToken(this) ?: return
            val serverUrl = Config.getServerUrl(this) ?: return
            
            val deviceData = JSONObject().apply {
                put("deviceId", Build.ID)
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("androidVersion", Build.VERSION.RELEASE)
                put("registrationToken", token)
            }

            val request = Request.Builder()
                .url("${serverUrl}/api/devices/register")
                .post(deviceData.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { Toast.makeText(this@ScannerActivity, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show() }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        try {
                            val data = JSONObject(body).getJSONObject("data")
                            val subs = data.optJSONArray("subscriptions")
                            val status = if (subs != null && subs.length() > 0) "ACTIVE" else "PENDING"
                            Config.saveSubscriptionInfo(this@ScannerActivity, status)
                        } catch (e: Exception) {
                            Log.e("Scanner", "Failed to parse registration response", e)
                        }
                        
                        runOnUiThread {
                            Toast.makeText(this@ScannerActivity, "Device Registered Successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ScannerActivity, PermissionsActivity::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread { Toast.makeText(this@ScannerActivity, "Server error: ${response.code}", Toast.LENGTH_SHORT).show() }
                    }
                }
            })
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

class QRAnalyzer(private val onQrDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onQrDetected(it) }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}
