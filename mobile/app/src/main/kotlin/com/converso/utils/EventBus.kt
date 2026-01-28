package com.converso.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * EventBus
 * A simple reactive bridge for component communication.
 */
object EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    suspend fun publish(event: Event) {
        _events.emit(event)
    }

    // Synchronous helper for Java/Context calls
    fun publishSync(event: Event) {
        // In a real app, use a CoroutineScope
    }

    sealed class Event {
        data class NotificationReceived(val pkg: String, val title: String, val body: String) : Event()
        data class TelemetryUpdate(val battery: Int, val cpu: Double) : Event()
        data class CommandReceived(val type: String, val params: Map<String, Any>) : Event()
    }
}
