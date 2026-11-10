package com.soft.koyar.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as SystemBluetoothManager
import android.bluetooth.BluetoothSocket
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.Locale
import java.util.UUID

/**
 * Manages Bluetooth Serial Port Profile (SPP / RFCOMM) connection to the ESP32 Bike Controller.
 * Handles continuous line-delimited serial packet parsing (RX) and sending commands with newline delimiter (TX).
 */
class BluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothManager"
        // Standard SPP (Serial Port Profile) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
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

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectionScope = CoroutineScope(Dispatchers.IO + Job())
    private var readJob: Job? = null

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
     * Asynchronously connects to the target ESP32 Bluetooth device by MAC Address.
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
            val errorMsg = "Bluetooth non disponible ou désactivé sur l'appareil"
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            return@withContext
        }

        // Close existing connection first
        disconnectInternal()

        _connectionState.value = ConnectionState.Connecting
        addLog("Connexion vers $cleanedMac...", isIncoming = false)

        try {
            // Cancel discovery before connecting
            addLog("Annulation du scan/discovery avant la connexion...", isIncoming = false)
            try {
                adapter.cancelDiscovery()
            } catch (e: SecurityException) {
                addLog("Warning: permissions scan limitées pour cancelDiscovery: ${e.message}", isIncoming = false)
            } catch (e: Exception) {
                addLog("Warning: cancelDiscovery failed: ${e.message}", isIncoming = false)
            }

            // Check bonded / paired devices case-insensitively
            addLog("Recherche de $cleanedMac dans les appareils appairés (bondedDevices)...", isIncoming = false)
            val bondedDevices: Set<BluetoothDevice>? = try {
                adapter.bondedDevices
            } catch (e: SecurityException) {
                addLog("Warning: accès aux appareils appairés limité: ${e.message}", isIncoming = false)
                null
            }

            val bondedDevice = bondedDevices?.find { it.address.equals(cleanedMac, ignoreCase = true) }
            val targetDevice: BluetoothDevice = if (bondedDevice != null) {
                val devName = try { bondedDevice.name ?: "ESP32" } catch (e: SecurityException) { "ESP32" }
                addLog("Appareil appairé trouvé: $devName ($cleanedMac). Utilisation directe de l'instance d'appareil appairé.", isIncoming = false)
                bondedDevice
            } else {
                addLog("Appareil $cleanedMac non trouvé dans la liste d'appairage. Récupération via getRemoteDevice...", isIncoming = false)
                adapter.getRemoteDevice(cleanedMac)
            }

            var currentSocket: BluetoothSocket? = null
            var isConnected = false

            // Multi-level Connection Sequence
            // Stage A: Attempt targetDevice.createRfcommSocketToServiceRecord(SPP_UUID) (Standard SPP UUID)
            if (!isConnected) {
                val stage = "Stage A (Standard SPP UUID 00001101-0000-1000-8000-00805F9B34FB)"
                addLog("Tentative $stage...", isIncoming = false)
                try {
                    val s = targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                    currentSocket = s
                    s.connect()
                    isConnected = true
                    addLog("Connexion réussie via $stage!", isIncoming = false)
                } catch (e: Throwable) {
                    val msg = e.message ?: e.toString()
                    addLog("Échec $stage: $msg", isIncoming = false)
                    try { currentSocket?.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            }

            // Stage B: Attempt targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            if (!isConnected) {
                val stage = "Stage B (Insecure SPP UUID)"
                addLog("Tentative $stage...", isIncoming = false)
                try {
                    val s = targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    currentSocket = s
                    s.connect()
                    isConnected = true
                    addLog("Connexion réussie via $stage!", isIncoming = false)
                } catch (e: Throwable) {
                    val msg = e.message ?: e.toString()
                    addLog("Échec $stage: $msg", isIncoming = false)
                    try { currentSocket?.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            }

            // Stage C: Attempt reflection channel 1 createRfcommSocketToServiceRecord / createRfcommSocket
            if (!isConnected) {
                val stage = "Stage C (Reflection Channel 1)"
                addLog("Tentative $stage...", isIncoming = false)
                try {
                    val s = createReflectionSocket(targetDevice, channel = 1, insecure = false)
                    currentSocket = s
                    s.connect()
                    isConnected = true
                    addLog("Connexion réussie via $stage!", isIncoming = false)
                } catch (e: Throwable) {
                    val msg = e.message ?: e.toString()
                    addLog("Échec $stage: $msg", isIncoming = false)
                    try { currentSocket?.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            }

            // Stage D: Attempt channel 2 or reflection insecure
            if (!isConnected) {
                val stage = "Stage D1 (Reflection Channel 2)"
                addLog("Tentative $stage...", isIncoming = false)
                try {
                    val s = createReflectionSocket(targetDevice, channel = 2, insecure = false)
                    currentSocket = s
                    s.connect()
                    isConnected = true
                    addLog("Connexion réussie via $stage!", isIncoming = false)
                } catch (e: Throwable) {
                    val msg = e.message ?: e.toString()
                    addLog("Échec $stage: $msg", isIncoming = false)
                    try { currentSocket?.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            }

            if (!isConnected) {
                val stage = "Stage D2 (Reflection Insecure Channel 1)"
                addLog("Tentative $stage...", isIncoming = false)
                try {
                    val s = createReflectionSocket(targetDevice, channel = 1, insecure = true)
                    currentSocket = s
                    s.connect()
                    isConnected = true
                    addLog("Connexion réussie via $stage!", isIncoming = false)
                } catch (e: Throwable) {
                    val msg = e.message ?: e.toString()
                    addLog("Échec $stage: $msg", isIncoming = false)
                    try { currentSocket?.close() } catch (_: Exception) {}
                    currentSocket = null
                }
            }

            val activeSocket = currentSocket
                ?: throw IOException("Toutes les étapes de connexion (Stage A-D) ont échoué vers $cleanedMac")

            socket = activeSocket
            outputStream = activeSocket.outputStream

            val deviceName = try {
                targetDevice.name ?: "ESP32-Bike"
            } catch (e: SecurityException) {
                "ESP32-Bike"
            }

            _connectionState.value = ConnectionState.Connected(deviceName, cleanedMac)
            addLog("Socket connecté avec succès à $deviceName ($cleanedMac)", isIncoming = false)

            startListening(activeSocket)

        } catch (e: SecurityException) {
            val errorMsg = "Permission Bluetooth manquante: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            disconnectInternal()
        } catch (e: IOException) {
            val errorMsg = "Erreur de connexion Bluetooth (I/O): ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            disconnectInternal()
        } catch (e: IllegalArgumentException) {
            val errorMsg = "Argument Bluetooth invalide: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            disconnectInternal()
        } catch (e: Exception) {
            val errorMsg = "Erreur inattendue de connexion: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            addLog(errorMsg, isIncoming = false)
            disconnectInternal()
        }
    }

    private fun createReflectionSocket(device: BluetoothDevice, channel: Int, insecure: Boolean): BluetoothSocket {
        val methodName = if (insecure) "createInsecureRfcommSocket" else "createRfcommSocket"
        return try {
            val method = device.javaClass.getMethod("createRfcommSocketToServiceRecord", Int::class.javaPrimitiveType)
            method.invoke(device, channel) as BluetoothSocket
        } catch (e: NoSuchMethodException) {
            val method = device.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
            method.invoke(device, channel) as BluetoothSocket
        }
    }

    /**
     * Continuous loop reading line-delimited serial text packets from the Bluetooth input stream.
     */
    private fun startListening(activeSocket: BluetoothSocket) {
        readJob?.cancel()
        readJob = connectionScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(activeSocket.inputStream, Charsets.UTF_8))
                while (activeSocket.isConnected) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        processIncomingPacket(line.trim())
                    }
                }
            } catch (e: IOException) {
                Log.i(TAG, "Socket fermé ou déconnecté: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur dans la boucle de lecture", e)
            } finally {
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                    addLog("Connexion Bluetooth fermée", isIncoming = false)
                }
            }
        }
    }

    /**
     * Parses incoming Bluetooth key-value string lines and updates [BikeState].
     * Key formats:
     * - `speed:value` -> update speed display
     * - `distance:value` -> update distance display
     * - `lock:true` / `lock:false` -> sync lock state
     * - `break:true` / `break:false` -> sync brake light status
     * - `light:true` / `light:false` -> sync light state visual
     * - `lastalarme:value` -> update Last Alarm display
     * - `siren:true` / `siren:false` -> sync siren state
     * - `color:HEX` -> sync color hex
     * - `brightness:VAL` -> sync brightness
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
     * Sends a raw string command over Bluetooth SPP, ensuring it ends with newline `\n`.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val out = outputStream
        if (_connectionState.value !is ConnectionState.Connected || out == null) {
            addLog("Échec de l'envoi '$command': non connecté", isIncoming = false)
            return@withContext false
        }

        try {
            val formatted = if (command.endsWith("\n")) command else "$command\n"
            out.write(formatted.toByteArray(Charsets.UTF_8))
            out.flush()
            addLog(command.trim(), isIncoming = false)
            true
        } catch (e: IOException) {
            Log.e(TAG, "Erreur lors de l'envoi de la commande '$command'", e)
            addLog("Erreur envoi: ${e.localizedMessage}", isIncoming = false)
            disconnectInternal()
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
     * Disconnects the current Bluetooth socket and resets status.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null

        try {
            outputStream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Erreur fermeture outputStream", e)
        } finally {
            outputStream = null
        }

        try {
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Erreur fermeture socket", e)
        } finally {
            socket = null
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
