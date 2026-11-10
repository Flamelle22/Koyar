package com.soft.koyar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.MediumGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite

/**
 * Geometric, ultra-minimalist button with sharp lines and high-contrast monochrome style.
 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = PureWhite,
    textColor: Color? = null,
    icon: ImageVector? = null,
    isFilled: Boolean = false,
    enabled: Boolean = true,
    cutCornerSize: Dp = 0.dp,
    paddingValues: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shape = CutCornerShape(cutCornerSize)

    val currentBgColor = when {
        !enabled -> DarkGray
        isPressed -> PureWhite
        isFilled -> PureWhite
        else -> PureBlack
    }

    val currentBorderColor = when {
        !enabled -> MediumGray
        else -> borderColor
    }

    val contentColor = when {
        !enabled -> MediumGray
        textColor != null -> textColor
        isPressed || isFilled -> PureBlack
        else -> PureWhite
    }

    Box(
        modifier = modifier
            .background(currentBgColor, shape)
            .border(1.dp, currentBorderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
