package com.converso.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.converso.utils.EventBus

/**
 * ConversoNotificationListener
 * Intercepts incoming notifications and relays them to the backend dashboard.
 */
class ConversoNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val title = sbn.notification.extras.getString("android.title")
        val text = sbn.notification.extras.getString("android.text")
        
        Log.d("ConversoNotif", "Post from $packageName: $title - $text")
        
        // Broadcast to the background service to send via WebSocket
        EventBus.publish(EventBus.Event.NotificationReceived(packageName, title ?: "", text ?: ""))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle notification removal if needed for syncing dashboard state
    }
}
