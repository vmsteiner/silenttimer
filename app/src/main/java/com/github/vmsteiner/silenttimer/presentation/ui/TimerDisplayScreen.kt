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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.github.vmsteiner.silenttimer.presentation.utils.Utils
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState

/**
 * Composable function for displaying the timer's remaining time.
 *
 * This composable is responsible for rendering the timer display screen. It shows the
 * remaining time of the countdown, and provides a button for stopping the timer.
 * The UI adapts to the ambient (always-on) mode on Wear OS devices.
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

    var checked by remember { mutableStateOf(true) }

    // AmbientAware ensures that we handle the different ambient states on Wear OS
    AmbientAware { ambientStateUpdate ->
        when (ambientStateUpdate) {
            // When the screen is in ambient (always-on) mode
            is AmbientState.Ambient -> {
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

            // When the screen is in interactive mode (regular mode)
            is AmbientState.Interactive -> {
                // Display the time with full opacity in interactive mode
                TimeText()

                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    when (page) {
                        0 -> Column(
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
                                onClick = {
                                    // Starts the TimerService to stop the timer when the button is clicked
                                    Intent(context, TimerService::class.java).also {
                                        it.action = TimerService.Actions.STOP.toString()
                                        context.startService(it)
                                    }
                                },
                                modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_stop_24),
                                    contentDescription = "Stop Timer",
                                    tint = Color.White
                                )
                            }
                        }

                        1 -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            // The primary label should have a maximum 3 lines of text
                            // and the secondary label should have max 2 lines of text.
                            ToggleChip(
                                label = {
                                    Text(
                                        "Halftime alert",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                checked = checked,
                                colors =
                                    ToggleChipDefaults.toggleChipColors(
                                        uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
                                    ),
                                toggleControl = { Switch(checked = checked, enabled = true) },
                                onCheckedChange = { checked = it },
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
                }

                HorizontalPageIndicator(
                    pageIndicatorState = pageIndicatorState
                )
            }

            // When the screen is off or inactive (ambient mode inactive)
            is AmbientState.Inactive -> {
                // No UI elements displayed when the screen is off
            }

        }
    }

}