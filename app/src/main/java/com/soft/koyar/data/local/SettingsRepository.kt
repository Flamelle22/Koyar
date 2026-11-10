package com.soft.koyar.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soft.koyar.data.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bike_settings")

/**
 * Repository responsible for persisting user settings such as ESP32 MAC Address, Auto-Connect toggle,
 * and the 4 Configurable Command Presets.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_MAC_ADDRESS = stringPreferencesKey("esp32_mac_address")
        val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        const val DEFAULT_MAC_ADDRESS = ""

        val KEY_PRESET_1_NAME = stringPreferencesKey("preset_1_name")
        val KEY_PRESET_1_CMD = stringPreferencesKey("preset_1_cmd")

        val KEY_PRESET_2_NAME = stringPreferencesKey("preset_2_name")
        val KEY_PRESET_2_CMD = stringPreferencesKey("preset_2_cmd")

        val KEY_PRESET_3_NAME = stringPreferencesKey("preset_3_name")
        val KEY_PRESET_3_CMD = stringPreferencesKey("preset_3_cmd")

        val KEY_PRESET_4_NAME = stringPreferencesKey("preset_4_name")
        val KEY_PRESET_4_CMD = stringPreferencesKey("preset_4_cmd")

        val DEFAULT_PRESETS = listOf(
            Preset(1, "Eco Light", "CMD:PRESET1\n"),
            Preset(2, "Sport Flash", "CMD:PRESET2\n"),
            Preset(3, "City Beacon", "CMD:PRESET3\n"),
            Preset(4, "Emergency Siren", "CMD:PRESET4\n")
        )
    }

    val macAddressFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_MAC_ADDRESS] ?: DEFAULT_MAC_ADDRESS
    }

    val autoConnectFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_CONNECT] ?: false
    }

    val presetsFlow: Flow<List<Preset>> = context.dataStore.data.map { preferences ->
        listOf(
            Preset(
                id = 1,
                name = preferences[KEY_PRESET_1_NAME] ?: DEFAULT_PRESETS[0].name,
                command = preferences[KEY_PRESET_1_CMD] ?: DEFAULT_PRESETS[0].command
            ),
            Preset(
                id = 2,
                name = preferences[KEY_PRESET_2_NAME] ?: DEFAULT_PRESETS[1].name,
                command = preferences[KEY_PRESET_2_CMD] ?: DEFAULT_PRESETS[1].command
            ),
            Preset(
                id = 3,
                name = preferences[KEY_PRESET_3_NAME] ?: DEFAULT_PRESETS[2].name,
                command = preferences[KEY_PRESET_3_CMD] ?: DEFAULT_PRESETS[2].command
            ),
            Preset(
                id = 4,
                name = preferences[KEY_PRESET_4_NAME] ?: DEFAULT_PRESETS[3].name,
                command = preferences[KEY_PRESET_4_CMD] ?: DEFAULT_PRESETS[3].command
            )
        )
    }

    suspend fun saveMacAddress(macAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAC_ADDRESS] = com.soft.koyar.data.bluetooth.BluetoothManager.normalizeMacAddress(macAddress)
        }
    }

    suspend fun saveAutoConnect(autoConnect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_CONNECT] = autoConnect
        }
    }

    suspend fun savePreset(preset: Preset) {
        context.dataStore.edit { preferences ->
            when (preset.id) {
                1 -> {
                    preferences[KEY_PRESET_1_NAME] = preset.name
                    preferences[KEY_PRESET_1_CMD] = preset.command
                }
                2 -> {
                    preferences[KEY_PRESET_2_NAME] = preset.name
                    preferences[KEY_PRESET_2_CMD] = preset.command
                }
                3 -> {
                    preferences[KEY_PRESET_3_NAME] = preset.name
                    preferences[KEY_PRESET_3_CMD] = preset.command
                }
                4 -> {
                    preferences[KEY_PRESET_4_NAME] = preset.name
                    preferences[KEY_PRESET_4_CMD] = preset.command
                }
            }
        }
    }

    suspend fun saveAllPresets(presets: List<Preset>) {
        context.dataStore.edit { preferences ->
            presets.forEach { preset ->
                when (preset.id) {
                    1 -> {
                        preferences[KEY_PRESET_1_NAME] = preset.name
                        preferences[KEY_PRESET_1_CMD] = preset.command
                    }
                    2 -> {
                        preferences[KEY_PRESET_2_NAME] = preset.name
                        preferences[KEY_PRESET_2_CMD] = preset.command
                    }
                    3 -> {
                        preferences[KEY_PRESET_3_NAME] = preset.name
                        preferences[KEY_PRESET_3_CMD] = preset.command
                    }
                    4 -> {
                        preferences[KEY_PRESET_4_NAME] = preset.name
                        preferences[KEY_PRESET_4_CMD] = preset.command
                    }
                }
            }
        }
    }

    suspend fun getMacAddress(): String = macAddressFlow.first()
    suspend fun getAutoConnect(): Boolean = autoConnectFlow.first()
    suspend fun getPresets(): List<Preset> = presetsFlow.first()
}
