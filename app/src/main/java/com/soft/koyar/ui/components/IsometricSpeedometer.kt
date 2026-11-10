package com.soft.koyar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas Central Speedometer gauge with minimalist geometric layout,
 * crisp white tick marks, and monochrome needle.
 */
@Composable
fun IsometricSpeedometer(
    speed: Float,
    modifier: Modifier = Modifier,
    maxSpeed: Float = 60.0f
) {
    var isMph by remember { mutableStateOf(false) }
    val displaySpeed = if (isMph) speed * 0.621371f else speed
    val unitText = if (isMph) "MPH" else "KM/H"

    val animatedSpeed by animateFloatAsState(
        targetValue = speed.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 300),
        label = "SpeedNeedleAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(PureBlack, CutCornerShape(0.dp))
            .border(1.dp, PureWhite, CutCornerShape(0.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.65f
            val radius = size.height * 0.52f

            // 1. Ticks and spoke layout
            drawSpeedometerBackground(centerX, centerY, radius)

            // 2. Outer Arc Track
            drawSpeedometerArc(centerX, centerY, radius, animatedSpeed, maxSpeed)

            // 3. Monochrome Needle Pointer
            drawNeedleAndHub(centerX, centerY, radius, animatedSpeed, maxSpeed)
        }

        // Digital Speedometer Reading Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 18.dp)
        ) {
            Text(
                text = "SPEED",
                color = PureWhite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "%.1f".format(displaySpeed),
                color = PureWhite,
                fontSize = 42.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Unit Toggle Badge
            Box(
                modifier = Modifier
                    .background(PureBlack, CutCornerShape(0.dp))
                    .border(1.dp, PureWhite, CutCornerShape(0.dp))
                    .clickable { isMph = !isMph }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$unitText ⟳",
                    color = PureWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun DrawScope.drawSpeedometerBackground(centerX: Float, centerY: Float, radius: Float) {
    val startAngle = 140f
    val sweepAngle = 260f

    val totalTicks = 12
    for (i in 0..totalTicks) {
        val fraction = i / totalTicks.toFloat()
        val angleRad = Math.toRadians((startAngle + fraction * sweepAngle).toDouble())
        val isMajor = i % 2 == 0

        val tickInnerRadius = if (isMajor) radius * 0.78f else radius * 0.84f
        val tickOuterRadius = radius * 0.92f

        val x1 = centerX + tickInnerRadius * cos(angleRad).toFloat()
        val y1 = centerY + tickInnerRadius * sin(angleRad).toFloat()
        val x2 = centerX + tickOuterRadius * cos(angleRad).toFloat()
        val y2 = centerY + tickOuterRadius * sin(angleRad).toFloat()

        drawLine(
            color = PureWhite,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = if (isMajor) 2f else 1f
        )
    }
}

private fun DrawScope.drawSpeedometerArc(
    centerX: Float,
    centerY: Float,
    radius: Float,
    currentSpeed: Float,
    maxSpeed: Float
) {
    val startAngle = 140f
    val sweepAngle = 260f
    val arcRadius = radius * 0.88f
    val topLeft = Offset(centerX - arcRadius, centerY - arcRadius)
    val size = Size(arcRadius * 2, arcRadius * 2)

    // Background Arc Track
    drawArc(
        color = DarkGray,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = 8f, cap = StrokeCap.Butt)
    )

    // Active White Speed Arc
    val speedFraction = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
    val activeSweep = sweepAngle * speedFraction

    if (activeSweep > 0.5f) {
        drawArc(
            color = PureWhite,
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 10f, cap = StrokeCap.Square)
        )
    }
}

private fun DrawScope.drawNeedleAndHub(
    centerX: Float,
    centerY: Float,
    radius: Float,
    currentSpeed: Float,
    maxSpeed: Float
) {
    val startAngle = 140f
    val sweepAngle = 260f
    val speedFraction = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
    val needleAngle = startAngle + speedFraction * sweepAngle
    val needleRad = Math.toRadians(needleAngle.toDouble())

    val needleLength = radius * 0.82f
    val needleTip = Offset(
        centerX + needleLength * cos(needleRad).toFloat(),
        centerY + needleLength * sin(needleRad).toFloat()
    )

    val perpRad = needleRad + Math.PI / 2
    val baseWidth = 6f
    val base1 = Offset(
        centerX + baseWidth * cos(perpRad).toFloat(),
        centerY + baseWidth * sin(perpRad).toFloat()
    )
    val base2 = Offset(
        centerX - baseWidth * cos(perpRad).toFloat(),
        centerY - baseWidth * sin(perpRad).toFloat()
    )

    val needlePath = Path().apply {
        moveTo(base1.x, base1.y)
        lineTo(needleTip.x, needleTip.y)
        lineTo(base2.x, base2.y)
        close()
    }

    drawPath(path = needlePath, color = PureWhite)

    // Center Hub
    drawCircle(color = PureBlack, radius = 8f, center = Offset(centerX, centerY))
    drawCircle(color = PureWhite, radius = 8f, center = Offset(centerX, centerY), style = Stroke(width = 1.5f))
}
