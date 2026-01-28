package com.converso.stream

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface

class ScreenCapture(private val mediaProjection: MediaProjection) {
    private val TAG = "ConversoStream"
    private var virtualDisplay: VirtualDisplay? = null

    fun startCapture(surface: Surface, width: Int, height: Int, dpi: Int) {
        Log.i(TAG, "Starting Screen Capture at ${width}x${height}")
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ConversoCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null
        )
    }

    fun stopCapture() {
        Log.i(TAG, "Stopping Screen Capture")
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection.stop()
    }
}
