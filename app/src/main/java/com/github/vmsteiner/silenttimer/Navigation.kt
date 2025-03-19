package com.github.vmsteiner.silenttimer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.github.vmsteiner.silenttimer.presentation.ui.TimerDisplayScreen
import com.github.vmsteiner.silenttimer.presentation.ui.TimerSetupScreen
import com.github.vmsteiner.silenttimer.presentation.utils.TimerStateManager

/**
 * Manages the navigation for the Silent Timer app.
 * Uses a swipe-dismissable navigation system for better user experience on Wear OS.
 */
@Composable
fun AppNavigation() {
    val navController = rememberSwipeDismissableNavController()
    val isTimerActive by TimerStateManager.isTimerActive.collectAsState(initial = false)

    /**
     * Watches for changes in the timer state and navigates to the appropriate screen.
     * If the timer is active, it navigates to the TimerDisplayScreen.
     * If inactive, it navigates to the TimerSetupScreen.
     */
    LaunchedEffect(isTimerActive) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val targetRoute = if (isTimerActive) Routes.TIMER_DISPLAY else Routes.TIMER_SETUP

        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(targetRoute) { inclusive = true }
            }
        }
    }

    /**
     * Defines the navigation structure of the app.
     * The start destination depends on whether a timer is active.
     */
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = if (isTimerActive) Routes.TIMER_DISPLAY else Routes.TIMER_SETUP
    ) {
        composable(Routes.TIMER_SETUP) { TimerSetupScreen() }
        composable(Routes.TIMER_DISPLAY) { TimerDisplayScreen() }
    }
}

/**
 * Defines the route names used in the navigation system.
 */
object Routes {
    const val TIMER_SETUP = "TimerSetupScreen"
    const val TIMER_DISPLAY = "TimerDisplayScreen"
}