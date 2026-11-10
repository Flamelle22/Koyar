package com.soft.koyar.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.data.model.LogMessage
import com.soft.koyar.data.model.Preset
import com.soft.koyar.ui.components.NeonButton
import com.soft.koyar.ui.components.NeonCard
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.MediumGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import com.soft.koyar.ui.viewmodel.MainViewModel

/**
 * Page 2: Settings (Portrait)
 * - Header & Connection: ESP32 MAC address field + UPDATE button, AUTO CONNECT toggle.
 * - Preset Configuration Grid (4 Presets 1 to 4): Name + Shell command text fields.
 * - Journaux & Logs: Terminal text box (*Shell Full History*), Custom command input + SEND button,
 *   Fixed text display showing Last Alarm (*Last Alarme: [valueminbefore]*).
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Enforce Portrait mode for Settings screen
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val savedMacAddress by viewModel.macAddress.collectAsState()
    val savedAutoConnect by viewModel.autoConnect.collectAsState()
    val presetsState by viewModel.presets.collectAsState()
    val bikeState by viewModel.bikeState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var inputMac by remember { mutableStateOf("") }
    var autoConnectEnabled by remember { mutableStateOf(false) }

    // Presets local editing state
    var presetList by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var customCommandText by remember { mutableStateOf("") }

    val logListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Sync input fields when saved values update
    LaunchedEffect(savedMacAddress, savedAutoConnect, presetsState) {
        if (inputMac.isEmpty() && savedMacAddress.isNotEmpty()) {
            inputMac = savedMacAddress
        }
        autoConnectEnabled = savedAutoConnect
        if (presetList.isEmpty() && presetsState.isNotEmpty()) {
            presetList = presetsState
        }
    }

    // Auto scroll log terminal to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "SETTINGS & LOGS",
                color = PureWhite,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // SECTION 1: HEADER & CONNECTION
        NeonCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ESP32 CONNECTION",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // MAC Address Field + UPDATE Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputMac,
                        onValueChange = { inputMac = it.uppercase().take(17) },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "MAC Address",
                                color = MediumGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        placeholder = {
                            Text(
                                text = "00:11:22:33:44:55",
                                color = DarkGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PureBlack,
                            unfocusedContainerColor = PureBlack,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = MediumGray
                        ),
                        shape = CutCornerShape(0.dp)
                    )

                    NeonButton(
                        text = "UPDATE",
                        onClick = {
                            keyboardController?.hide()
                            val formattedMac = com.soft.koyar.data.bluetooth.BluetoothManager.normalizeMacAddress(inputMac)
                            inputMac = formattedMac
                            viewModel.saveSettings(formattedMac, autoConnectEnabled)
                            viewModel.connect(formattedMac)
                        },
                        isFilled = true,
                        cutCornerSize = 0.dp
                    )
                }

                // AUTO CONNECT Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTO CONNECT ON STARTUP",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Switch(
                        checked = autoConnectEnabled,
                        onCheckedChange = { checked ->
                            autoConnectEnabled = checked
                            viewModel.saveSettings(inputMac, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = PureWhite,
                            uncheckedThumbColor = MediumGray,
                            uncheckedTrackColor = PureBlack,
                            uncheckedBorderColor = MediumGray
                        )
                    )
                }
            }
        }

        // SECTION 2: PRESET CONFIGURATION GRID (4 Presets 1 to 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESET CONFIGURATION GRID (1 to 4)",
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            NeonButton(
                text = "SAVE PRESETS",
                icon = Icons.Rounded.Save,
                onClick = {
                    keyboardController?.hide()
                    if (presetList.isNotEmpty()) {
                        viewModel.savePresets(presetList)
                    }
                },
                isFilled = true,
                cutCornerSize = 0.dp
            )
        }

        // 4 Configuration Blocks
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val displayPresets = if (presetList.size == 4) presetList else com.soft.koyar.data.local.SettingsRepository.DEFAULT_PRESETS

            displayPresets.forEachIndexed { index, preset ->
                PresetConfigBlock(
                    presetNumber = index + 1,
                    preset = preset,
                    onPresetChange = { updated ->
                        val newList = displayPresets.toMutableList()
                        newList[index] = updated
                        presetList = newList
                    }
                )
            }
        }

        // SECTION 3: JOURNAUX & LOGS
        Text(
            text = "JOURNAUX & LOGS",
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        // Fixed text display showing Last Alarm
        NeonCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Alarme:",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = bikeState.lastAlarm.ifBlank { "None" },
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Terminal text box (*Shell Full History*) showing scrolling serial logs
        NeonCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shell Full History",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = PureWhite
                        )
                    }
                }

                // Log Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(PureBlack, CutCornerShape(0.dp))
                        .border(1.dp, MediumGray, CutCornerShape(0.dp))
                        .padding(6.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "> Ready for incoming/outgoing serial packet logs...",
                            color = MediumGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(
                            state = logListState,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(logs) { log ->
                                LogLineItem(log = log)
                            }
                        }
                    }
                }

                // Custom command text input box with a "SEND" button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCommandText,
                        onValueChange = { customCommandText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Custom shell command...",
                                color = DarkGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (customCommandText.isNotBlank()) {
                                viewModel.sendCustomCommand(customCommandText)
                                customCommandText = ""
                                keyboardController?.hide()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PureBlack,
                            unfocusedContainerColor = PureBlack,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = MediumGray
                        ),
                        shape = CutCornerShape(0.dp)
                    )

                    NeonButton(
                        text = "SEND",
                        icon = Icons.AutoMirrored.Rounded.Send,
                        onClick = {
                            if (customCommandText.isNotBlank()) {
                                viewModel.sendCustomCommand(customCommandText)
                                customCommandText = ""
                                keyboardController?.hide()
                            }
                        },
                        isFilled = true,
                        cutCornerSize = 0.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetConfigBlock(
    presetNumber: Int,
    preset: Preset,
    onPresetChange: (Preset) -> Unit
) {
    NeonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PRESET $presetNumber",
                color = PureWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name text field
                OutlinedTextField(
                    value = preset.name,
                    onValueChange = { newName ->
                        onPresetChange(preset.copy(name = newName))
                    },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            text = "Name",
                            color = MediumGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureBlack,
                        unfocusedContainerColor = PureBlack,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = MediumGray
                    ),
                    shape = CutCornerShape(0.dp)
                )

                // Shell command text field
                OutlinedTextField(
                    value = preset.command,
                    onValueChange = { newCmd ->
                        onPresetChange(preset.copy(command = newCmd))
                    },
                    modifier = Modifier.weight(1.2f),
                    label = {
                        Text(
                            text = "Shell Command",
                            color = MediumGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureBlack,
                        unfocusedContainerColor = PureBlack,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = MediumGray
                    ),
                    shape = CutCornerShape(0.dp)
                )
            }
        }
    }
}

@Composable
private fun LogLineItem(log: LogMessage) {
    val prefix = if (log.isIncoming) "RX" else "TX"
    Text(
        text = "[${log.formattedTime}] $prefix: ${log.text}",
        color = PureWhite,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 13.sp
    )
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
