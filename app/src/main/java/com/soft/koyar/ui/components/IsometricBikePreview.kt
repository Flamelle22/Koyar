package com.soft.koyar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.data.model.BikeState
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Canvas composable rendering an ultra-minimalist geometric bike schema.
 * Dynamically renders front light beam when [BikeState.isLightsOn] (`light:true`)
 * and rear brake stop light glow when [BikeState.isBrakeOn] (`break:true`).
 */
@Composable
fun IsometricBikePreview(
    bikeState: BikeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(PureBlack, CutCornerShape(0.dp))
            .border(1.dp, PureWhite, CutCornerShape(0.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + 10f

            // 1. Draw Minimalist Grid Floor
            drawMinimalGrid(centerX, centerY)

            // 2. Draw Front Headlight Beam (when light:true)
            if (bikeState.isLightsOn) {
                drawFrontLightBeam(centerX, centerY)
            }

            // 3. Draw Rear Brake Stop Light Glow (when break:true)
            if (bikeState.isBrakeOn) {
                drawRearBrakeGlow(centerX, centerY)
            }

            // 4. Draw Geometric Minimalist Bike Schema
            drawGeometricBikeSchema(centerX, centerY, bikeState)

            // 5. Draw Lock Ring Indicator
            drawLockIndicator(centerX, centerY, bikeState.isLocked)
        }

        // Status Tag Overlay
        val statusText = when {
            bikeState.isSirenOn -> "SIREN ACTIVE"
            bikeState.isLocked -> "LOCKED"
            else -> "UNLOCKED"
        }

        Text(
            text = statusText,
            color = PureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(PureBlack, CutCornerShape(0.dp))
                .border(1.dp, PureWhite, CutCornerShape(0.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun DrawScope.drawMinimalGrid(centerX: Float, centerY: Float) {
    val gridRadius = 130f
    val step = 26f
    val gridColor = Color.White.copy(alpha = 0.15f)

    for (i in -4..4) {
        val offset = i * step
        drawLine(
            color = gridColor,
            start = Offset(centerX - gridRadius + offset, centerY - (gridRadius - offset) * 0.4f),
            end = Offset(centerX + gridRadius + offset, centerY + (gridRadius + offset) * 0.4f),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(centerX - gridRadius - offset, centerY + (gridRadius - offset) * 0.4f),
            end = Offset(centerX + gridRadius - offset, centerY - (gridRadius + offset) * 0.4f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawFrontLightBeam(centerX: Float, centerY: Float) {
    val hlX = centerX + 55f
    val hlY = centerY - 38f

    val beamPath = Path().apply {
        moveTo(hlX, hlY)
        lineTo(hlX + 160f, hlY - 35f)
        lineTo(hlX + 175f, hlY + 30f)
        lineTo(hlX + 35f, hlY + 30f)
        close()
    }

    val beamBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.2f),
            Color.Transparent
        ),
        start = Offset(hlX, hlY),
        end = Offset(hlX + 175f, hlY)
    )

    drawPath(path = beamPath, brush = beamBrush)

    for (i in -2..2) {
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(hlX, hlY),
            end = Offset(hlX + 160f, hlY + i * 14f),
            strokeWidth = 1.5f
        )
    }
}

private fun DrawScope.drawRearBrakeGlow(centerX: Float, centerY: Float) {
    val rearX = centerX - 55f
    val rearY = centerY - 35f

    // Concentric glowing stop light rings at rear
    for (r in 1..4) {
        drawCircle(
            color = Color.White.copy(alpha = 0.9f / r),
            radius = r * 8f,
            center = Offset(rearX, rearY),
            style = Stroke(width = 2f)
        )
    }

    // Expanding rear stop light beam lines
    for (i in -2..2) {
        drawLine(
            color = Color.White,
            start = Offset(rearX, rearY),
            end = Offset(rearX - 50f, rearY + i * 10f),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawGeometricBikeSchema(centerX: Float, centerY: Float, bikeState: BikeState) {
    val rearWheel = Offset(centerX - 65f, centerY + 10f)
    val frontWheel = Offset(centerX + 65f, centerY - 15f)
    val bottomBracket = Offset(centerX - 10f, centerY + 5f)
    val seatPost = Offset(centerX - 35f, centerY - 45f)
    val seatSaddle = Offset(centerX - 38f, centerY - 55f)
    val headTube = Offset(centerX + 40f, centerY - 40f)
    val handlebars = Offset(centerX + 42f, centerY - 58f)

    // Wheels
    drawOctagonWheel(rearWheel, radius = 28f)
    drawOctagonWheel(frontWheel, radius = 28f)

    // Frame tubes (monochrome white sharp lines)
    val framePath = Path().apply {
        moveTo(rearWheel.x, rearWheel.y)
        lineTo(bottomBracket.x, bottomBracket.y)
        lineTo(seatPost.x, seatPost.y)
        lineTo(rearWheel.x, rearWheel.y)
        moveTo(bottomBracket.x, bottomBracket.y)
        lineTo(headTube.x, headTube.y)
        moveTo(seatPost.x, seatPost.y)
        lineTo(headTube.x, headTube.y)
        moveTo(headTube.x, headTube.y)
        lineTo(frontWheel.x, frontWheel.y)
        moveTo(headTube.x, headTube.y)
        lineTo(handlebars.x, handlebars.y)
        moveTo(seatPost.x, seatPost.y)
        lineTo(seatSaddle.x, seatSaddle.y)
    }

    drawPath(
        path = framePath,
        color = PureWhite,
        style = Stroke(width = 3f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    )

    // Handlebar bar end line
    drawLine(
        color = PureWhite,
        start = Offset(handlebars.x - 12f, handlebars.y + 4f),
        end = Offset(handlebars.x + 12f, handlebars.y - 4f),
        strokeWidth = 3f
    )

    // Saddle polygon
    val saddlePath = Path().apply {
        moveTo(seatSaddle.x - 12f, seatSaddle.y + 2f)
        lineTo(seatSaddle.x + 8f, seatSaddle.y - 4f)
        lineTo(seatSaddle.x - 2f, seatSaddle.y - 6f)
        close()
    }
    drawPath(saddlePath, color = PureWhite)

    // Joint Nodes
    val nodes = listOf(rearWheel, frontWheel, bottomBracket, seatPost, headTube, handlebars)
    for (node in nodes) {
        drawCircle(color = PureWhite, radius = 4f, center = node)
        drawCircle(color = PureBlack, radius = 2f, center = node)
    }
}

private fun DrawScope.drawOctagonWheel(center: Offset, radius: Float) {
    val sides = 8
    val path = Path()
    for (i in 0 until sides) {
        val angle = i * (2 * Math.PI / sides)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + (radius * 0.6f) * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(path = path, color = PureWhite, style = Stroke(width = 2f))
    drawCircle(color = PureWhite, radius = 3f, center = center)
}

private fun DrawScope.drawLockIndicator(centerX: Float, centerY: Float, isLocked: Boolean) {
    val shieldPath = Path()
    val radius = 70f
    val sides = 6
    for (i in 0 until sides) {
        val angle = i * (2 * Math.PI / sides) - Math.PI / 2
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY - 10f + (radius * 0.55f) * sin(angle).toFloat()
        if (i == 0) shieldPath.moveTo(x, y) else shieldPath.lineTo(x, y)
    }
    shieldPath.close()

    drawPath(
        path = shieldPath,
        color = PureWhite,
        style = Stroke(
            width = 1.5f,
            pathEffect = if (isLocked) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    )
}
