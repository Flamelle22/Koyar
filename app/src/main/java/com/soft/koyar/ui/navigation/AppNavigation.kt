package com.soft.koyar.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soft.koyar.ui.dashboard.DashboardScreen
import com.soft.koyar.ui.home.HomeScreen
import com.soft.koyar.ui.settings.SettingsScreen
import com.soft.koyar.ui.theme.DarkGray
import com.soft.koyar.ui.theme.MediumGray
import com.soft.koyar.ui.theme.PureBlack
import com.soft.koyar.ui.theme.PureWhite
import com.soft.koyar.ui.viewmodel.MainViewModel

/**
 * Main Navigation host managing transitions between Home, Settings, and Dashboard screens.
 * Ultra-minimalist monochrome styling. Ensures Dashboard NEVER shows the bottom navigation bar.
 */
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    // Handle system back button when stack > 1
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    val showBottomBar = currentScreen != Screen.Dashboard

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PureBlack,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it }
            ) {
                MonochromeBottomNavBar(
                    currentScreen = currentScreen,
                    onSelectTab = { targetScreen ->
                        if (currentScreen != targetScreen) {
                            backStack.clear()
                            backStack.add(targetScreen)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { width -> width }) togetherWith
                            (fadeOut() + slideOutHorizontally { width -> -width })
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToDashboard = {
                                backStack.add(Screen.Dashboard)
                            },
                            onNavigateToSettings = {
                                backStack.clear()
                                backStack.add(Screen.Settings)
                            }
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel
                        )
                    }

                    is Screen.Dashboard -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onBackToHome = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonochromeBottomNavBar(
    currentScreen: Screen,
    onSelectTab: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = PureBlack,
        contentColor = PureWhite,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, PureWhite, CutCornerShape(0.dp))
    ) {
        val items = listOf(
            NavTabItem(Screen.Home, "HOME", Icons.AutoMirrored.Rounded.DirectionsBike),
            NavTabItem(Screen.Settings, "SETTINGS", Icons.Rounded.Settings)
        )

        items.forEach { tab ->
            val isSelected = currentScreen == tab.screen

            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab.screen) },
                icon = {
                    Box(
                        modifier = if (isSelected) {
                            Modifier
                                .background(PureWhite, CutCornerShape(0.dp))
                                .border(1.dp, PureWhite, CutCornerShape(0.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        } else Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) PureBlack else MediumGray
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    color = PureBlack,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                label = null,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = PureBlack,
                    unselectedIconColor = MediumGray
                )
            )
        }
    }
}

private data class NavTabItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)
