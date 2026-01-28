package com.converso.stream

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ConversoStreamer
 * Manages the screen capture pipeline using ImageReader.
 * Converts frames to JPEG and sends via callback.
 */
class ConversoStreamer(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onFrame: (String) -> Unit
) {
    private val isStreaming = AtomicBoolean(false)
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        if (isStreaming.getAndSet(true)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        
        // Scale down for performance
        val width = metrics.widthPixels / 2
        val height = metrics.heightPixels / 2
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ConversoStream",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                // Compress to JPEG
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                val base64Frame = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                
                onFrame(base64Frame)
                
                bitmap.recycle()
            } catch (e: Exception) {
                Log.e("ConversoStream", "Frame processing failed", e)
            } finally {
                image.close()
            }
        }, handler)
        
        Log.i("ConversoStream", "Streaming Pipeline Active")
    }

    fun stop() {
        isStreaming.set(false)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection.stop()
        Log.i("ConversoStream", "Streaming Pipeline Stopped")
    }
}
