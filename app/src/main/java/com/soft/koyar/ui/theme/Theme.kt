package com.soft.koyar.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MonochromeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkGray,
    onPrimaryContainer = PureWhite,

    secondary = PureWhite,
    onSecondary = PureBlack,
    secondaryContainer = DarkGray,
    onSecondaryContainer = PureWhite,

    tertiary = PureWhite,
    onTertiary = PureBlack,

    error = PureWhite,
    onError = PureBlack,
    errorContainer = DarkGray,
    onErrorContainer = PureWhite,

    background = PureBlack,
    onBackground = PureWhite,

    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = DarkGray,
    onSurfaceVariant = PureWhite,

    outline = PureWhite,
    outlineVariant = MediumGray
)

@Composable
fun KoyarTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = PureBlack.toArgb()
                window.navigationBarColor = PureBlack.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
