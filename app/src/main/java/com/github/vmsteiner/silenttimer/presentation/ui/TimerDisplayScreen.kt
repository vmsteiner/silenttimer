package com.github.vmsteiner.silenttimer.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
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
                        ),
                        modifier = Modifier
                    )
                }
            }

            // When the screen is in interactive mode (regular mode)
            is AmbientState.Interactive -> {
                // Display the time with full opacity in interactive mode
                TimeText()
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
                        modifier = Modifier
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
                        modifier = Modifier
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_stop_24),
                            contentDescription = "Select",
                            tint = Color.White
                        )
                    }
                }
            }

            // When the screen is off or inactive (ambient mode inactive)
            is AmbientState.Inactive -> {
                // No UI elements displayed when the screen is off
            }

        }
    }

}