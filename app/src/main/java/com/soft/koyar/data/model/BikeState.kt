package com.soft.koyar.data.model

/**
 * Represents the current telemetry and operational state of the Smart Bike.
 */
data class BikeState(
    val isLocked: Boolean = true,
    val speed: Float = 0.0f,
    val distance: Float = 0.0f,
    val isLightsOn: Boolean = false,
    val isBrakeOn: Boolean = false,
    val isSirenOn: Boolean = false,
    val lastAlarm: String = "None",
    val colorHex: String = "FFFFFF",
    val brightness: Int = 255,
    val rawState: String = "locked",
    val lastUpdated: Long = System.currentTimeMillis()
)
