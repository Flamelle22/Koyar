package com.soft.koyar.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager as SystemBluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.soft.koyar.data.model.BikeState
import com.soft.koyar.data.model.BluetoothDeviceInfo
import com.soft.koyar.data.model.ConnectionState
import com.soft.koyar.data.model.LogMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

/**
 * Manages Bluetooth Low Energy (BLE GATT) connection to the ESP32 Bike Controller.
 * Handles continuous line-delimited notifications (RX) and sending commands with newline delimiter (TX).
 */
class BluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"

        // BLE Custom Service and Characteristic UUIDs defined on the ESP32
        val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ae")

        // Standard Client Characteristic Configuration Descriptor (CCCD) for enabling NOTIFY / INDICATE
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val MAX_LOG_ENTRIES = 500

        /**
         * Normalizes MAC address by trimming whitespace, converting to uppercase (Locale.ROOT),
         * removing non-hex characters and auto-formatting into XX:XX:XX:XX:XX:XX before usage.
         */
        fun normalizeMacAddress(rawMac: String): String {
            val trimmed = rawMac.trim().uppercase(Locale.ROOT)
            if (trimmed.isEmpty()) return ""

            val cleanHex = trimmed.replace(Regex("[^0-9A-F]"), "")
            if (cleanHex.length == 12) {
                return cleanHex.chunked(2).joinToString(":")
            }

            return trimmed.replace("-", ":").replace(" ", ":")
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val systemManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? SystemBluetoothManager
        systemManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _bikeState = MutableStateFlow(BikeState())
    val bikeState: StateFlow<BikeState> = _bikeState.asStateFlow()

    private val _logMessages = MutableStateFlow<List<LogMessage>>(emptyList())
    val logMessages: StateFlow<List<LogMessage>> = _logMessages.asStateFlow()
    val logs: StateFlow<List<LogMessage>> = _logMessages.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null
    private var connectionScope = CoroutineScope(Dispatchers.IO + Job())

    // Buffer to reconstruct newline-delimited packets across BLE chunks
    private val rxBuffer = StringBuilder()

    /**
     * Checks if runtime Bluetooth permissions are granted depending on Android SDK version.
     */
    fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scanGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            return connectGranted && scanGranted
        }
        return true
    }

    /**
     * Retrieves the list of bonded (paired) Bluetooth devices.
     */
    fun getBondedDevices(): List<BluetoothDeviceInfo> {
        if (!hasBluetoothPermissions()) return emptyList()
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.map { device ->
                val devName = try {
                    device.name?.ifBlank { null } ?: "Appareil Bluetooth"
                } catch (e: SecurityException) {
                    "Appareil Bluetooth"
                }
                BluetoothDeviceInfo(
                    name = devName,
                    address = normalizeMacAddress(device.address)
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permissions limitées pour la liste des appareils appairés: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur récupération appareils appairés: ${e.message}")
            emptyList()
        }
    }

    /**
     * Connects to the ESP32 via Bluetooth Low Energy (GATT).
     */
    suspend fun connect(macAddress: String) = withContext(Dispatchers.IO) {
        val cleanedMac = normalizeMacAddress(macAddress)

        if (cleanedMac.isEmpty()) {
            val errorMsg = "Adresse MAC vide. Veuillez saisir une adresse MAC valide."
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            return@withContext
        }

        if (!BluetoothAdapter.checkBluetoothAddress(cleanedMac)) {
            val errorMsg = "Adresse MAC invalide: '$macAddress' (format attendu AA:BB:CC:DD:EE:FF)"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            return@withContext
        }

        if (!hasBluetoothPermissions()) {
            val errorMsg = "Permission Bluetooth manquante (BLUETOOTH_CONNECT / BLUETOOTH_SCAN non accordée)"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            return@withContext
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            val errorMsg = "Bluetooth non disponible ou désactivé sur le smartphone"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            return@withContext
        }

        // Close any previous GATT connection
        disconnectInternal()

        _connectionState.value = ConnectionState.Connecting
        addLog("Connexion BLE (GATT) vers $cleanedMac...", isIncoming = false)

        try {
            val device = adapter.getRemoteDevice(cleanedMac)
            val devName = try { device.name ?: "ESP32-Bike" } catch (e: SecurityException) { "ESP32-Bike" }

            addLog("Ouverture connexion GATT (TRANSPORT_LE)...", isIncoming = false)

            // Connect using TRANSPORT_LE to force Bluetooth Low Energy on modern Android
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }

            if (bluetoothGatt == null) {
                val errorMsg = "Échec d'initialisation de connectGatt()"
                _connectionState.value = ConnectionState.Error(errorMsg)
                addLog(errorMsg, isIncoming = false)
            }
        } catch (e: SecurityException) {
            val errorMsg = "Permission Bluetooth refusée: ${e.localizedMessage}"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
        } catch (e: Exception) {
            val errorMsg = "Erreur connexion BLE: ${e.localizedMessage}"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val devAddress = gatt.device.address
            val devName = try { gatt.device.name ?: "ESP32-Bike" } catch (e: SecurityException) { "ESP32-Bike" }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addLog("Connecté au serveur BLE GATT ($devAddress). Découverte des services...", isIncoming = false)
                try {
                    // Discover GATT services on the ESP32
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    addLog("Erreur discoverServices (permission): ${e.message}", isIncoming = false)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                addLog("Déconnecté du périphérique BLE (status=$status)", isIncoming = false)
                targetCharacteristic = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Services BLE découverts avec succès.", isIncoming = false)
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    val errorMsg = "Service BLE introuvable: $SERVICE_UUID"
                    addLog(errorMsg, isIncoming = false)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                    return
                }

                val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic == null) {
                    val errorMsg = "Caractéristique BLE introuvable: $CHARACTERISTIC_UUID"
                    addLog(errorMsg, isIncoming = false)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                    return
                }

                targetCharacteristic = characteristic

                // Enable local notifications
                try {
                    val notifySet = gatt.setCharacteristicNotification(characteristic, true)
                    addLog("Notification locale activée: $notifySet", isIncoming = false)

                    // Write CCCD descriptor to enable ESP32 notifications/indications
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        val descriptorValue = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        } else {
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, descriptorValue)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = descriptorValue
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                        addLog("Souscription CCCD envoyée à l'ESP32.", isIncoming = false)
                    }

                    // Request higher MTU (up to 512) for large packets
                    gatt.requestMtu(512)

                } catch (e: SecurityException) {
                    addLog("Erreur activation notifications (permission): ${e.message}", isIncoming = false)
                }

                val devName = try { gatt.device.name ?: "ESP32-Bike" } catch (e: SecurityException) { "ESP32-Bike" }
                _connectionState.value = ConnectionState.Connected(devName, gatt.device.address)
                addLog("Prêt ! Connexion active avec l'ESP32.", isIncoming = false)

            } else {
                val errorMsg = "Échec de la découverte des services GATT (status=$status)"
                addLog(errorMsg, isIncoming = false)
                _connectionState.value = ConnectionState.Error(errorMsg)
            }
        }

        // Android 13+ (API 33+) callback for characteristic changes
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }

        // Android 12 and below callback
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleIncomingBytes(value)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val text = String(bytes, Charsets.UTF_8)
        synchronized(rxBuffer) {
            rxBuffer.append(text)
            var newlineIndex = rxBuffer.indexOf('\n')
            while (newlineIndex != -1) {
                val line = rxBuffer.substring(0, newlineIndex).trim()
                rxBuffer.delete(0, newlineIndex + 1)
                if (line.isNotEmpty()) {
                    processIncomingPacket(line)
                }
                newlineIndex = rxBuffer.indexOf('\n')
            }
        }
    }

    /**
     * Parses incoming Bluetooth key-value string lines and updates [BikeState].
     */
    fun processIncomingPacket(packet: String) {
        addLog(packet, isIncoming = true)

        val colonIndex = packet.indexOf(':')
        if (colonIndex == -1) return

        val key = packet.substring(0, colonIndex).trim().lowercase()
        val value = packet.substring(colonIndex + 1).trim()
        val valueLower = value.lowercase()

        _bikeState.update { currentState ->
            when (key) {
                "speed" -> {
                    val speedVal = value.toFloatOrNull() ?: currentState.speed
                    currentState.copy(speed = speedVal, lastUpdated = System.currentTimeMillis())
                }
                "distance" -> {
                    val distVal = value.toFloatOrNull() ?: currentState.distance
                    currentState.copy(distance = distVal, lastUpdated = System.currentTimeMillis())
                }
                "lock" -> {
                    val isLocked = valueLower == "true" || valueLower == "1" || valueLower == "locked"
                    currentState.copy(isLocked = isLocked, rawState = if (isLocked) "locked" else "unlocked", lastUpdated = System.currentTimeMillis())
                }
                "break", "brake" -> {
                    val isBrake = valueLower == "true" || valueLower == "1" || valueLower == "on"
                    currentState.copy(isBrakeOn = isBrake, lastUpdated = System.currentTimeMillis())
                }
                "light", "lights" -> {
                    val isLight = valueLower == "true" || valueLower == "1" || valueLower == "on"
                    currentState.copy(isLightsOn = isLight, lastUpdated = System.currentTimeMillis())
                }
                "lastalarme", "lastalarm", "alarm" -> {
                    currentState.copy(lastAlarm = value, lastUpdated = System.currentTimeMillis())
                }
                "siren" -> {
                    val isSiren = valueLower == "true" || valueLower == "1" || valueLower == "on"
                    currentState.copy(isSirenOn = isSiren, lastUpdated = System.currentTimeMillis())
                }
                "color" -> {
                    currentState.copy(colorHex = value.uppercase(Locale.ROOT), lastUpdated = System.currentTimeMillis())
                }
                "brightness" -> {
                    val brightVal = value.toIntOrNull() ?: currentState.brightness
                    currentState.copy(brightness = brightVal, lastUpdated = System.currentTimeMillis())
                }
                "state" -> {
                    when (valueLower) {
                        "locked" -> currentState.copy(isLocked = true, rawState = "locked", lastUpdated = System.currentTimeMillis())
                        "unlocked" -> currentState.copy(isLocked = false, rawState = "unlocked", lastUpdated = System.currentTimeMillis())
                        "lights_on", "light_on" -> currentState.copy(isLightsOn = true, lastUpdated = System.currentTimeMillis())
                        "lights_off", "light_off" -> currentState.copy(isLightsOn = false, lastUpdated = System.currentTimeMillis())
                        "alarm_on", "siren_on" -> currentState.copy(isSirenOn = true, lastUpdated = System.currentTimeMillis())
                        "alarm_off", "siren_off" -> currentState.copy(isSirenOn = false, lastUpdated = System.currentTimeMillis())
                        else -> currentState.copy(rawState = value, lastUpdated = System.currentTimeMillis())
                    }
                }
                else -> currentState
            }
        }
    }

    /**
     * Sends a raw string command over BLE GATT, ensuring it ends with newline `\n`.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt
        val char = targetCharacteristic

        if (_connectionState.value !is ConnectionState.Connected || gatt == null || char == null) {
            addLog("Échec de l'envoi '$command': non connecté en BLE", isIncoming = false)
            return@withContext false
        }

        try {
            val formatted = if (command.endsWith("\n")) command else "$command\n"
            val payload = formatted.toByteArray(Charsets.UTF_8)

            val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, payload, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = payload
                @Suppress("DEPRECATION")
                char.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }

            if (success) {
                addLog(command.trim(), isIncoming = false)
                true
            } else {
                addLog("Échec writeCharacteristic pour '$command'", isIncoming = false)
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Erreur permission lors de l'envoi de la commande '$command'", e)
            addLog("Erreur permission envoi: ${e.localizedMessage}", isIncoming = false)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'envoi de la commande '$command'", e)
            addLog("Erreur envoi BLE: ${e.localizedMessage}", isIncoming = false)
            false
        }
    }

    suspend fun sendLock(): Boolean = sendCommand("CMD:LOCK")
    suspend fun sendUnlock(): Boolean = sendCommand("CMD:UNLOCK")
    suspend fun sendLightOn(): Boolean = sendCommand("CMD:LIGHT_ON")
    suspend fun sendLightOff(): Boolean = sendCommand("CMD:LIGHT_OFF")
    suspend fun sendSirenOn(): Boolean = sendCommand("CMD:SIREN_ON")
    suspend fun sendSirenOff(): Boolean = sendCommand("CMD:SIREN_OFF")
    suspend fun sendColor(hex: String): Boolean = sendCommand("color:$hex")
    suspend fun sendBrightness(value: Int): Boolean = sendCommand("brightness:$value")

    // Dashboard lighting controls
    suspend fun sendLightLow(): Boolean = sendCommand("CMD:LIGHT_LOW")
    suspend fun sendLightHigh(): Boolean = sendCommand("CMD:LIGHT_HIGH")
    suspend fun sendLightInhibit(): Boolean = sendCommand("CMD:LIGHT_INHIBIT")

    // Dashboard turn signal controls
    suspend fun sendSignalLeft(): Boolean = sendCommand("CMD:SIGNAL_LEFT")
    suspend fun sendSignalStop(): Boolean = sendCommand("CMD:SIGNAL_STOP")
    suspend fun sendSignalRight(): Boolean = sendCommand("CMD:SIGNAL_RIGHT")

    /**
     * Disconnects the current Bluetooth GATT connection.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "Erreur fermeture BluetoothGatt: permission", e)
        } catch (e: Exception) {
            Log.w(TAG, "Erreur fermeture BluetoothGatt", e)
        } finally {
            bluetoothGatt = null
            targetCharacteristic = null
            synchronized(rxBuffer) {
                rxBuffer.clear()
            }
        }

        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Adds a line to the terminal log stream.
     */
    private fun addLog(text: String, isIncoming: Boolean) {
        Log.d(TAG, text)
        _logMessages.update { currentLogs ->
            val newLog = LogMessage(text = text, isIncoming = isIncoming)
            (currentLogs + newLog).takeLast(MAX_LOG_ENTRIES)
        }
    }

    fun clearLogs() {
        _logMessages.value = emptyList()
    }
}
