package com.converso.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/**
 * ConversoAccessibilityService
 * The "Hammer" of the Remote Agent. 
 * Responsible for:
 * 1. UI Automation (Scraping screen content)
 * 2. Touch Injection (Clicks, Swipes)
 * 3. System Actions (Home, Back, Recents)
 */
class ConversoAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ConversoAccess", "Accessibility Service Linked & Powered")
        // Initialize service configuration if needed
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional: Listen for window changes or UI updates to sync dashboard
    }

    override fun onInterrupt() {
        Log.w("ConversoAccess", "Service Interrupted")
    }

    /**
     * Injects a click at (x, y) coordinates.
     */
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("ConversoAccess", "Click successfully injected at $x, $y")
            }
        }, null)
    }

    /**
     * Injects a swipe from (x1, y1) to (x2, y2).
     */
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300) {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Performs global system actions (Home, Back, notifications).
     */
    fun performGlobalAction(action: Int) {
        performGlobalAction(action)
    }

    companion object {
        var instance: ConversoAccessibilityService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
