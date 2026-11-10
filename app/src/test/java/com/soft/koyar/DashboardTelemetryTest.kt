package com.soft.koyar

import com.soft.koyar.data.model.BikeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTelemetryTest {

    @Test
    fun speedometer_speedUnitConversion() {
        val speedKmh = 42.5f
        val speedMph = speedKmh * 0.621371f
        assertEquals(26.408268f, speedMph, 0.001f)
    }

    @Test
    fun speedometer_maxSpeedClamping() {
        val maxSpeed = 60.0f
        val overSpeed = 75.0f
        val clampedSpeed = overSpeed.coerceIn(0f, maxSpeed)
        assertEquals(60.0f, clampedSpeed, 0.001f)

        val negativeSpeed = -10.0f
        val clampedMin = negativeSpeed.coerceIn(0f, maxSpeed)
        assertEquals(0.0f, clampedMin, 0.001f)
    }

    @Test
    fun bikeState_telemetryFlags() {
        var state = BikeState(speed = 42.5f, distance = 12.0f, isLocked = false, isLightsOn = true, isBrakeOn = true, isSirenOn = true, lastAlarm = "15min ago")
        assertEquals(42.5f, state.speed, 0.001f)
        assertEquals(12.0f, state.distance, 0.001f)
        assertFalse(state.isLocked)
        assertTrue(state.isLightsOn)
        assertTrue(state.isBrakeOn)
        assertTrue(state.isSirenOn)
        assertEquals("15min ago", state.lastAlarm)

        state = state.copy(isLightsOn = false, isBrakeOn = false, isSirenOn = false)
        assertFalse(state.isLightsOn)
        assertFalse(state.isBrakeOn)
        assertFalse(state.isSirenOn)
    }
}
