package com.converso.control

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class ControlService : AccessibilityService() {
    private val TAG = "ConversoControl"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun tap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
