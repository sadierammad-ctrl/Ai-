package com.example.androidcontrol

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmergencyStopManager {
    private val _lastStopTimestamp = MutableStateFlow(0L)
    val lastStopTimestamp: StateFlow<Long> = _lastStopTimestamp.asStateFlow()

    private val stopListeners = mutableListOf<() -> Unit>()

    fun registerStopListener(listener: () -> Unit) {
        if (!stopListeners.contains(listener)) {
            stopListeners.add(listener)
        }
    }

    fun unregisterStopListener(listener: () -> Unit) {
        stopListeners.remove(listener)
    }

    fun triggerEmergencyStop() {
        _lastStopTimestamp.value = System.currentTimeMillis()
        stopListeners.toList().forEach { runCatching { it.invoke() } }
    }
}
