package com.soft.koyar

import com.soft.koyar.data.model.BikeState
import com.soft.koyar.data.model.ConnectionState
import com.soft.koyar.data.model.LogMessage
import com.soft.koyar.data.model.Preset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeDataTest {

    @Test
    fun defaultBikeState_isLocked() {
        val state = BikeState()
        assertTrue(state.isLocked)
        assertEquals(0.0f, state.speed, 0.001f)
        assertEquals(0.0f, state.distance, 0.001f)
        assertFalse(state.isLightsOn)
        assertFalse(state.isBrakeOn)
        assertFalse(state.isSirenOn)
        assertEquals("None", state.lastAlarm)
        assertEquals("locked", state.rawState)
    }

    @Test
    fun logMessage_formatTime() {
        val log = LogMessage(timestamp = 1600000000000L, text = "state:locked", isIncoming = true)
        assertTrue(log.isIncoming)
        assertEquals("state:locked", log.text)
        assertTrue(log.formattedTime.isNotEmpty())
    }

    @Test
    fun connectionState_types() {
        val disconnected: ConnectionState = ConnectionState.Disconnected
        val connecting: ConnectionState = ConnectionState.Connecting
        val connected: ConnectionState = ConnectionState.Connected("ESP32", "00:11:22:33:44:55")
        val error: ConnectionState = ConnectionState.Error("Failed")

        assertTrue(disconnected is ConnectionState.Disconnected)
        assertTrue(connecting is ConnectionState.Connecting)
        assertTrue(connected is ConnectionState.Connected)
        assertTrue(error is ConnectionState.Error)
    }

    @Test
    fun preset_dataClass() {
        val preset = Preset(1, "Eco Light", "CMD:PRESET1\n")
        assertEquals(1, preset.id)
        assertEquals("Eco Light", preset.name)
        assertEquals("CMD:PRESET1\n", preset.command)
    }
}
