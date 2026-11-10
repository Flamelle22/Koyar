package com.soft.koyar.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Screen routes for Navigation 3 architecture.
 */
@Serializable
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Dashboard : Screen
}
