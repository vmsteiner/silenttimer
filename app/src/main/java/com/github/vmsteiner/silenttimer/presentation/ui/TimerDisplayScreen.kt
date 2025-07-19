package com.github.vmsteiner.silenttimer.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.github.vmsteiner.silenttimer.R
import com.github.vmsteiner.silenttimer.presentation.service.TimerService
import com.github.vmsteiner.silenttimer.presentation.utils.CountdownManager
import com.github.vmsteiner.silenttimer.presentation.utils.HALFTIME_ALERT_KEY
import com.github.vmsteiner.silenttimer.presentation.utils.Utils
import com.github.vmsteiner.silenttimer.presentation.utils.dataStore
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Composable function for displaying the timer's remaining time.
 *
 * This composable is responsible for rendering the timer display screen. It shows the
 * remaining time of the countdown, and provides a button for stopping the timer.
 * The UI adapts to the ambient (always-on) mode on Wear OS devices.
 * It also has a second horizontal page where the halftime alert can be disabled.
 */
@Composable
fun TimerDisplayScreen() {
    // Context of the current composable, used to start services
    val context = LocalContext.current

    // Collects the current countdown time from the CountdownManager
    val timeLeft by CountdownManager.countdownTime.collectAsState()

    // Setup horizontal pager with 2 screen
    val pagerState = rememberPagerState(pageCount = { 2 })
    // Wrap PagerState in PageIndicatorState
    val pageIndicatorState: PageIndicatorState = remember(pagerState) {
        object : PageIndicatorState {
            override val selectedPage: Int
                get() = pagerState.currentPage
            override val pageOffset: Float
                get() = pagerState.currentPageOffsetFraction
            override val pageCount: Int
                get() = pagerState.pageCount
        }
    }

    val halftimeAlertFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[HALFTIME_ALERT_KEY] ?: true }

    val isHalftimeAlertOn by halftimeAlertFlow.collectAsState(initial = true)

    val scope = rememberCoroutineScope()

    val onHalftimeAlertChange: (Boolean) -> Unit = { isOn ->
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[HALFTIME_ALERT_KEY] = isOn
            }
        }
    }

    // AmbientAware ensures that we handle the different ambient states on Wear OS
    AmbientAware { ambientStateUpdate ->
        when (ambientStateUpdate) {
            // When the screen is in ambient (always-on) mode
            is AmbientState.Ambient -> AmbientTimerDisplay(timeLeft)

            // When the screen is in interactive mode (regular mode)
            is AmbientState.Interactive -> InteractiveTimerDisplay(
                timeLeft = timeLeft,
                pagerState = pagerState,
                pageIndicatorState = pageIndicatorState,
                isHalftimeAlertOn = isHalftimeAlertOn,
                onHalftimeAlertChange  = onHalftimeAlertChange,
                onStopTimer = {
                    Intent(context, TimerService::class.java).also {
                        it.action = TimerService.Actions.STOP.toString()
                        context.startService(it)
                    }
                }
            )

            // When the screen is off or inactive (ambient mode inactive)
            is AmbientState.Inactive -> Unit

        }
    }

}

/**
 * Composable function for displaying the timer in ambient (always-on) mode.
 *
 * @param timeLeft The remaining time of the countdown in milliseconds.
 */
@Composable
private fun AmbientTimerDisplay(timeLeft: Long) {
    TimeText(modifier = Modifier.alpha(0.5f))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = Utils.formatTime(timeLeft),
            fontSize = 32.sp,
            color = Color.LightGray,
            style = TextStyle.Default.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            )
        )
    }
}

/**
 * Composable function for displaying the interactive timer UI.
 *
 * This includes a pager for navigating between the timer page and the settings page,
 * as well as a horizontal page indicator for visual feedback.
 *
 * @param timeLeft The remaining time of the countdown in milliseconds.
 * @param pagerState State of the horizontal pager.
 * @param pageIndicatorState State of the page indicator tied to the pager.
 * @param isHalftimeAlertOn Whether the halftime alert toggle is enabled.
 * @param onHalftimeAlertChange Callback when the toggle state is changed.
 * @param onStopTimer Callback when the stop timer button is clicked.
 */
@Composable
private fun InteractiveTimerDisplay(
    timeLeft: Long,
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageIndicatorState: PageIndicatorState,
    isHalftimeAlertOn: Boolean,
    onHalftimeAlertChange: (Boolean) -> Unit,
    onStopTimer: () -> Unit
) {
    TimeText()
    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> TimerPage(timeLeft = timeLeft, onStopTimer = onStopTimer)
            1 -> SettingsPage(isHalftimeAlertOn = isHalftimeAlertOn, onHalftimeAlertChange = onHalftimeAlertChange)
        }
    }
    HorizontalPageIndicator(pageIndicatorState = pageIndicatorState)
}

/**
 * Composable function for rendering the main timer page.
 *
 * Displays the formatted remaining time and a stop button.
 *
 * @param timeLeft The remaining time of the countdown in milliseconds.
 * @param onStopTimer Callback when the stop button is pressed.
 */
@Composable
private fun TimerPage(timeLeft: Long, onStopTimer: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = Utils.formatTime(timeLeft),
            fontSize = 32.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStopTimer,
            modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_stop_24),
                contentDescription = "Stop Timer",
                tint = Color.White
            )
        }
    }
}

/**
 * Composable function for rendering the settings page.
 *
 * Allows the user to enable or disable the halftime alert via a toggle chip.
 *
 * @param isHalftimeAlertOn Whether the halftime alert is currently enabled.
 * @param onHalftimeAlertChange Callback when the toggle chip state is changed.
 */
@Composable
private fun SettingsPage(isHalftimeAlertOn: Boolean, onHalftimeAlertChange: (Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        ToggleChip(
            label = {
                Text("Halftime alert", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            checked = isHalftimeAlertOn,
            colors = ToggleChipDefaults.toggleChipColors(
                uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
            ),
            toggleControl = { Switch(checked = isHalftimeAlertOn, enabled = true) },
            onCheckedChange = onHalftimeAlertChange,
            appIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_settings_24),
                    contentDescription = "Halftime alert",
                    modifier = Modifier.size(24.dp)
                )
            },
            enabled = true,
        )
    }
}
