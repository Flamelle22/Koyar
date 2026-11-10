package com.soft.koyar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite

/**
 * Ultra-minimalist geometric container with sharp lines and high-contrast monochrome border.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PureWhite,
    backgroundColor: Color = PureBlack,
    cutCornerSize: Dp = 0.dp,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = CutCornerShape(cutCornerSize)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
