package com.soft.koyar

import com.soft.koyar.data.bluetooth.BluetoothManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidationTest {

    private val macRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")

    @Test
    fun validMacAddress_passesRegex() {
        assertTrue(macRegex.matches("00:11:22:33:44:55"))
        assertTrue(macRegex.matches("AA:BB:CC:DD:EE:FF"))
        assertTrue(macRegex.matches("24:0A:C4:00:01:02"))
    }

    @Test
    fun invalidMacAddress_failsRegex() {
        assertFalse(macRegex.matches("001122334455"))
        assertFalse(macRegex.matches("00:11:22:33:44"))
        assertFalse(macRegex.matches("00:11:22:33:44:55:66"))
        assertFalse(macRegex.matches("ZZ:11:22:33:44:55"))
    }

    @Test
    fun rawHexFormatting_addsColons() {
        val rawHex = "001122334455"
        val formatted = rawHex.chunked(2).joinToString(":")
        assertTrue(macRegex.matches(formatted))
    }

    @Test
    fun normalizeMacAddress_handlesDashesLowercaseAndSpaces() {
        val input1 = "  aa-bb-cc-dd-ee-ff  "
        val normalized1 = BluetoothManager.normalizeMacAddress(input1)
        assertEquals("AA:BB:CC:DD:EE:FF", normalized1)

        val input2 = "001122334455"
        val normalized2 = BluetoothManager.normalizeMacAddress(input2)
        assertEquals("00:11:22:33:44:55", normalized2)
    }
}

