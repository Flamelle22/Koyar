package com.soft.koyar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soft.koyar.data.local.SettingsRepository
import com.soft.koyar.data.model.BikeState
import com.soft.koyar.data.model.ConnectionState
import com.soft.koyar.data.model.LogMessage
import com.soft.koyar.data.model.Preset
import com.soft.koyar.data.repository.BikeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel exposing bike telemetry, connection status, log terminal entries,
 * preset configurations, and control actions.
 */
class MainViewModel(
    application: Application,
    private val bikeRepository: BikeRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        bikeRepository = BikeRepository(application)
    )

    val connectionState: StateFlow<ConnectionState> = bikeRepository.connectionState
    val bikeState: StateFlow<BikeState> = bikeRepository.bikeState
    val logs: StateFlow<List<LogMessage>> = bikeRepository.logs

    val macAddress: StateFlow<String> = bikeRepository.macAddressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val autoConnect: StateFlow<Boolean> = bikeRepository.autoConnectFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val presets: StateFlow<List<Preset>> = bikeRepository.presetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.DEFAULT_PRESETS
    )

    init {
        // Trigger auto-connect on launch if enabled and MAC address exists
        viewModelScope.launch {
            val isAuto = bikeRepository.autoConnectFlow.first()
            val mac = bikeRepository.macAddressFlow.first()
            if (isAuto && mac.isNotBlank()) {
                bikeRepository.connect(mac)
            }
        }
    }

    fun connect(mac: String? = null) {
        viewModelScope.launch {
            val targetMac = mac?.trim()?.uppercase() ?: macAddress.value.ifBlank { bikeRepository.macAddressFlow.first() }
            if (targetMac.isNotBlank()) {
                bikeRepository.connect(targetMac)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bikeRepository.disconnect()
        }
    }

    fun saveSettings(newMac: String, autoConnectEnabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val cleanedMac = newMac.trim().uppercase()
            bikeRepository.saveMacAddress(cleanedMac)
            bikeRepository.saveAutoConnect(autoConnectEnabled)
            onComplete?.invoke(true)
        }
    }

    fun savePresets(newPresets: List<Preset>, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            bikeRepository.savePresets(newPresets)
            onComplete?.invoke(true)
        }
    }

    fun sendLock() {
        viewModelScope.launch {
            bikeRepository.sendLock()
        }
    }

    fun sendUnlock() {
        viewModelScope.launch {
            bikeRepository.sendUnlock()
        }
    }

    fun sendLightOn() {
        viewModelScope.launch {
            bikeRepository.sendLightOn()
        }
    }

    fun sendLightOff() {
        viewModelScope.launch {
            bikeRepository.sendLightOff()
        }
    }

    fun sendSirenOn() {
        viewModelScope.launch {
            bikeRepository.sendSirenOn()
        }
    }

    fun sendSirenOff() {
        viewModelScope.launch {
            bikeRepository.sendSirenOff()
        }
    }

    fun sendColor(hex: String) {
        viewModelScope.launch {
            bikeRepository.sendColor(hex)
        }
    }

    fun sendBrightness(value: Int) {
        viewModelScope.launch {
            bikeRepository.sendBrightness(value)
        }
    }

    fun sendLightLow() {
        viewModelScope.launch {
            bikeRepository.sendLightLow()
        }
    }

    fun sendLightHigh() {
        viewModelScope.launch {
            bikeRepository.sendLightHigh()
        }
    }

    fun sendLightInhibit() {
        viewModelScope.launch {
            bikeRepository.sendLightInhibit()
        }
    }

    fun sendSignalLeft() {
        viewModelScope.launch {
            bikeRepository.sendSignalLeft()
        }
    }

    fun sendSignalStop() {
        viewModelScope.launch {
            bikeRepository.sendSignalStop()
        }
    }

    fun sendSignalRight() {
        viewModelScope.launch {
            bikeRepository.sendSignalRight()
        }
    }

    fun sendPresetCommand(command: String) {
        if (command.isNotBlank()) {
            viewModelScope.launch {
                bikeRepository.sendCustomCommand(command)
            }
        }
    }

    fun sendCustomCommand(command: String) {
        if (command.isNotBlank()) {
            viewModelScope.launch {
                bikeRepository.sendCustomCommand(command)
            }
        }
    }

    fun clearLogs() {
        bikeRepository.clearLogs()
    }
}
