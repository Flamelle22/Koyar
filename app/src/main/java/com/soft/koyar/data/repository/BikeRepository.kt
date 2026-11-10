package com.soft.koyar.data.repository

import android.content.Context
import com.soft.koyar.data.bluetooth.BluetoothManager
import com.soft.koyar.data.local.SettingsRepository
import com.soft.koyar.data.model.BikeState
import com.soft.koyar.data.model.ConnectionState
import com.soft.koyar.data.model.LogMessage
import com.soft.koyar.data.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository orchestrating Bluetooth device operations and persistent user settings.
 */
class BikeRepository(
    private val bluetoothManager: BluetoothManager,
    private val settingsRepository: SettingsRepository
) {
    constructor(context: Context) : this(
        bluetoothManager = BluetoothManager(context.applicationContext),
        settingsRepository = SettingsRepository(context.applicationContext)
    )

    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val bikeState: StateFlow<BikeState> = bluetoothManager.bikeState
    val logs: StateFlow<List<LogMessage>> = bluetoothManager.logs

    val macAddressFlow: Flow<String> = settingsRepository.macAddressFlow
    val autoConnectFlow: Flow<Boolean> = settingsRepository.autoConnectFlow
    val presetsFlow: Flow<List<Preset>> = settingsRepository.presetsFlow

    suspend fun connect(macAddress: String) {
        bluetoothManager.connect(macAddress)
    }

    suspend fun connectSavedMac() {
        val mac = settingsRepository.getMacAddress()
        if (mac.isNotBlank()) {
            bluetoothManager.connect(mac)
        }
    }

    suspend fun disconnect() {
        bluetoothManager.disconnect()
    }

    suspend fun sendLock(): Boolean = bluetoothManager.sendLock()
    suspend fun sendUnlock(): Boolean = bluetoothManager.sendUnlock()
    suspend fun sendLightOn(): Boolean = bluetoothManager.sendLightOn()
    suspend fun sendLightOff(): Boolean = bluetoothManager.sendLightOff()
    suspend fun sendSirenOn(): Boolean = bluetoothManager.sendSirenOn()
    suspend fun sendSirenOff(): Boolean = bluetoothManager.sendSirenOff()
    suspend fun sendColor(hex: String): Boolean = bluetoothManager.sendColor(hex)
    suspend fun sendBrightness(value: Int): Boolean = bluetoothManager.sendBrightness(value)

    suspend fun sendLightLow(): Boolean = bluetoothManager.sendLightLow()
    suspend fun sendLightHigh(): Boolean = bluetoothManager.sendLightHigh()
    suspend fun sendLightInhibit(): Boolean = bluetoothManager.sendLightInhibit()

    suspend fun sendSignalLeft(): Boolean = bluetoothManager.sendSignalLeft()
    suspend fun sendSignalStop(): Boolean = bluetoothManager.sendSignalStop()
    suspend fun sendSignalRight(): Boolean = bluetoothManager.sendSignalRight()

    suspend fun sendCustomCommand(command: String): Boolean = bluetoothManager.sendCommand(command)

    suspend fun saveMacAddress(macAddress: String) {
        settingsRepository.saveMacAddress(macAddress)
    }

    suspend fun saveAutoConnect(autoConnect: Boolean) {
        settingsRepository.saveAutoConnect(autoConnect)
    }

    suspend fun savePresets(presets: List<Preset>) {
        settingsRepository.saveAllPresets(presets)
    }

    fun clearLogs() {
        bluetoothManager.clearLogs()
    }
}
