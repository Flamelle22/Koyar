package com.soft.koyar.ui.dashboard

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.soft.koyar.data.model.Preset
import com.soft.koyar.ui.components.NeonButton
import com.soft.koyar.ui.components.NeonCard
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.MediumGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import com.soft.koyar.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Page 3: Dashboard (Landscape / Full Screen)
 * Organized into three distinct blocks left to right:
 * - Left Block (Metrics & Navigation): Real-time digital clock, Speed & Distance side-by-side, Discrete exit button.
 * - Central Block (Presets): Single column with 4 preset buttons stacked vertically.
 * - Right Block (Controls & Turn Signals):
 *   - Column just left of right edge: Code (Low), Phares (High), Éteint (Off), Inhibe (Zero light, including brake).
 *   - Column on far right edge: Left Arrow (←), Central STOP button, Right Arrow (→).
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Enforce Landscape & Hide System Bars for Full Screen Dashboard
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        insetsController?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val bikeState by viewModel.bikeState.collectAsState()
    val presets by viewModel.presets.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BLOCK 1: LEFT BLOCK (Metrics & Navigation)
            LeftMetricsBlock(
                speed = bikeState.speed,
                distance = bikeState.distance,
                onExit = onBackToHome,
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            )

            // BLOCK 2: CENTRAL BLOCK (Presets)
            CentralPresetsBlock(
                presets = presets,
                onPresetClick = { cmd -> viewModel.sendPresetCommand(cmd) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // BLOCK 3: RIGHT BLOCK (Controls & Turn Signals)
            RightControlsBlock(
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Left Block (Metrics & Navigation):
 * - Top: Real-time digital clock (`HH:mm:ss`).
 * - Center: Speed (`speed:value` in km/h) and distance (`distance:value` in km) displayed side-by-side in large crisp monospaced white text.
 * - Bottom-Left: Discrete exit button to return to Home menu.
 */
@Composable
private fun LeftMetricsBlock(
    speed: Float,
    distance: Float,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Real-time digital clock (HH:mm:ss)
            RealTimeClock()

            // Center: Speed & Distance side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPEED",
                        color = MediumGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f".format(speed),
                        color = PureWhite,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KM/H",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .width(1.dp)
                        .background(MediumGray)
                )

                // Distance Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DISTANCE",
                        color = MediumGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f".format(distance),
                        color = PureWhite,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KM",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom-Left: Discrete exit button
            Box(
                modifier = Modifier
                    .background(PureBlack, CutCornerShape(0.dp))
                    .border(1.dp, PureWhite, CutCornerShape(0.dp))
                    .clickable { onExit() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Exit",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXIT TO HOME",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Central Block (Presets):
 * Single column containing the 4 preset buttons stacked vertically (showing their custom configured names,
 * clicking sends the configured shell command).
 */
@Composable
private fun CentralPresetsBlock(
    presets: List<Preset>,
    onPresetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "PRESETS",
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            val displayPresets = if (presets.size == 4) presets else com.soft.koyar.data.local.SettingsRepository.DEFAULT_PRESETS

            displayPresets.forEach { preset ->
                NeonButton(
                    text = preset.name.ifBlank { "Preset ${preset.id}" },
                    onClick = { onPresetClick(preset.command) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    cutCornerSize = 0.dp
                )
            }
        }
    }
}

/**
 * Right Block (Controls & Turn Signals):
 * Organized into two columns:
 * - Left column (Lighting controls top-to-bottom):
 *   1. Code (Low) (sends CMD:LIGHT_LOW\n)
 *   2. Phares (High) (sends CMD:LIGHT_HIGH\n)
 *   3. Éteint (Off) (sends CMD:LIGHT_OFF\n)
 *   4. Inhibe (Zero light, including brake) (sends CMD:LIGHT_INHIBIT\n)
 * - Right column (Turn signals):
 *   - Left Arrow (←) (sends CMD:SIGNAL_LEFT\n)
 *   - Central STOP button (sends CMD:SIGNAL_STOP\n)
 *   - Right Arrow (→) (sends CMD:SIGNAL_RIGHT\n)
 */
@Composable
private fun RightControlsBlock(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NeonCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Lighting Controls Column (left of right edge)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "LIGHTING",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 1. Code (Low)
                NeonButton(
                    text = "Code (Low)",
                    icon = Icons.Rounded.FlashOn,
                    onClick = { viewModel.sendLightLow() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )

                // 2. Phares (High)
                NeonButton(
                    text = "Phares (High)",
                    icon = Icons.Rounded.FlashOn,
                    onClick = { viewModel.sendLightHigh() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )

                // 3. Éteint (Off)
                NeonButton(
                    text = "Éteint (Off)",
                    icon = Icons.Rounded.FlashOff,
                    onClick = { viewModel.sendLightOff() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )

                // 4. Inhibe (Zero light, including brake)
                NeonButton(
                    text = "Inhibe",
                    onClick = { viewModel.sendLightInhibit() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )
            }

            // Vertical divider line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(DarkGray)
            )

            // Turn Signals Column (far right edge)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SIGNALS",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Left Arrow (←)
                NeonButton(
                    text = "← LEFT",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = { viewModel.sendSignalLeft() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )

                // Central STOP button
                NeonButton(
                    text = "STOP",
                    icon = Icons.Rounded.Stop,
                    onClick = { viewModel.sendSignalStop() },
                    isFilled = true,
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )

                // Right Arrow (→)
                NeonButton(
                    text = "RIGHT →",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { viewModel.sendSignalRight() },
                    modifier = Modifier.fillMaxWidth(),
                    cutCornerSize = 0.dp
                )
            }
        }
    }
}

@Composable
private fun RealTimeClock(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getFormattedTime()
            delay(1000L)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(PureBlack, CutCornerShape(0.dp))
            .border(1.dp, PureWhite, CutCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = "Clock",
            tint = PureWhite,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = currentTime,
            color = PureWhite,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun getFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}
