package com.soft.koyar.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.data.model.ConnectionState
import com.soft.koyar.ui.components.IsometricBikePreview
import com.soft.koyar.ui.components.NeonButton
import com.soft.koyar.ui.components.NeonCard
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.MediumGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import com.soft.koyar.ui.viewmodel.MainViewModel

/**
 * Page 1: Home (Portrait)
 * - Top Bar: Bluetooth link icon, MAC address display, Lock status indicator icon.
 * - Central Area: Minimal bike schema with dynamic lights & brake stop light indicators.
 * - Navigation: Prominent "DASHBOARD" button to switch to landscape dashboard screen.
 * - Controls: LOCK / UNLOCK, LIGHT / OFF, SIREN / OFF.
 * - Sliders: FULL BIKE COLOR (real-time `color:HEX\n`), FULL BIKE BRIGHTNESS (real-time `brightness:VAL\n`).
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Enforce Portrait mode for Home screen
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val connectionState by viewModel.connectionState.collectAsState()
    val bikeState by viewModel.bikeState.collectAsState()
    val macAddress by viewModel.macAddress.collectAsState()

    var colorHue by remember { mutableFloatStateOf(0f) }
    var brightnessVal by remember { mutableFloatStateOf(255f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP BAR: Bluetooth Icon, MAC Address, Lock Status Padlock Indicator
        HomeTopBar(
            connectionState = connectionState,
            macAddress = macAddress,
            isLocked = bikeState.isLocked,
            onOpenSettings = onNavigateToSettings
        )

        // 2. CENTRAL AREA: Bike Visual Schema with dynamic front beam & rear brake light
        IsometricBikePreview(bikeState = bikeState)

        // 3. NAVIGATION: Prominent "DASHBOARD" button
        NeonButton(
            text = "DASHBOARD [LANDSCAPE]",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = onNavigateToDashboard,
            isFilled = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            cutCornerSize = 0.dp
        )

        // 4. CONTROLS & STATES
        Text(
            text = "HARDWARE CONTROLS",
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Command Buttons Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            val buttonModifier = Modifier.weight(1f)

            // LOCK / UNLOCK
            NeonButton(
                text = "LOCK",
                icon = Icons.Rounded.Lock,
                onClick = { viewModel.sendLock() },
                isFilled = bikeState.isLocked,
                modifier = buttonModifier
            )

            NeonButton(
                text = "UNLOCK",
                icon = Icons.Rounded.LockOpen,
                onClick = { viewModel.sendUnlock() },
                isFilled = !bikeState.isLocked,
                modifier = buttonModifier
            )

            // LIGHT ON / OFF
            NeonButton(
                text = "LIGHT ON",
                icon = Icons.Rounded.FlashOn,
                onClick = { viewModel.sendLightOn() },
                isFilled = bikeState.isLightsOn,
                modifier = buttonModifier
            )

            NeonButton(
                text = "LIGHT OFF",
                icon = Icons.Rounded.FlashOff,
                onClick = { viewModel.sendLightOff() },
                isFilled = !bikeState.isLightsOn,
                modifier = buttonModifier
            )

            // SIREN ON / OFF
            NeonButton(
                text = "SIREN ON",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                onClick = { viewModel.sendSirenOn() },
                isFilled = bikeState.isSirenOn,
                modifier = buttonModifier
            )

            NeonButton(
                text = "SIREN OFF",
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                onClick = { viewModel.sendSirenOff() },
                isFilled = !bikeState.isSirenOn,
                modifier = buttonModifier
            )
        }

        // 5. SLIDERS
        // Slider 1: FULL BIKE COLOR (sends color:HEX\n in real time)
        val currentHex = remember(colorHue) { hueToHex(colorHue) }

        NeonCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FULL BIKE COLOR",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "#$currentHex",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = colorHue,
                    onValueChange = { newHue ->
                        colorHue = newHue
                        val hex = hueToHex(newHue)
                        viewModel.sendColor(hex)
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = PureWhite,
                        activeTrackColor = PureWhite,
                        inactiveTrackColor = DarkGray
                    )
                )
            }
        }

        // Slider 2: FULL BIKE BRIGHTNESS (sends brightness:VAL\n in real time)
        NeonCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FULL BIKE BRIGHTNESS",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = brightnessVal.toInt().toString(),
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = brightnessVal,
                    onValueChange = { newBright ->
                        brightnessVal = newBright
                        viewModel.sendBrightness(newBright.toInt())
                    },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(
                        thumbColor = PureWhite,
                        activeTrackColor = PureWhite,
                        inactiveTrackColor = DarkGray
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    connectionState: ConnectionState,
    macAddress: String,
    isLocked: Boolean,
    onOpenSettings: () -> Unit
) {
    val btIcon = when (connectionState) {
        is ConnectionState.Connected -> Icons.Rounded.BluetoothConnected
        is ConnectionState.Connecting -> Icons.AutoMirrored.Rounded.BluetoothSearching
        else -> Icons.Rounded.BluetoothDisabled
    }

    NeonCard(
        modifier = Modifier.clickable { onOpenSettings() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Bluetooth icon & MAC display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = btIcon,
                    contentDescription = "Bluetooth",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = macAddress.ifBlank { "MAC NOT SET" },
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (val state = connectionState) {
                            is ConnectionState.Connected -> "CONNECTED"
                            is ConnectionState.Connecting -> "CONNECTING..."
                            is ConnectionState.Error -> "ERROR: ${state.message}"
                            else -> "DISCONNECTED"
                        },
                        color = MediumGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Padlock Link lock status indicator icon
            Box(
                modifier = Modifier
                    .background(PureBlack, CutCornerShape(0.dp))
                    .border(1.dp, PureWhite, CutCornerShape(0.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = "Lock Status",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLocked) "LOCKED" else "UNLOCKED",
                        color = PureWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun hueToHex(hue: Float): String {
    val hsv = floatArrayOf(hue, 1.0f, 1.0f)
    val colorInt = android.graphics.Color.HSVToColor(hsv)
    return String.format("%06X", 0xFFFFFF and colorInt)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
