package com.soft.koyar.data.model

/**
 * Represents the current Bluetooth connection status with the ESP32 controller.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String, val macAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
