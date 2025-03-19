package com.github.vmsteiner.silenttimer.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.vmsteiner.silenttimer.presentation.service.TimerService
import com.github.vmsteiner.silenttimer.presentation.viewmodel.TimerSetupViewModel
import com.google.android.horologist.composables.TimePicker
import java.time.LocalTime

/**
 * Timer setup screen for selecting and starting a countdown timer.
 *
 * This composable function provides a user interface to select a time for the countdown.
 * When the user confirms a valid time (non-zero), the selected time is stored in the
 * `TimerSetupViewModel` and a `TimerService` is started with the selected time.
 *
 * **Key Features:**
 * - Uses a `TimePicker` for selecting a time.
 * - Stores the selected time in `TimerSetupViewModel`.
 * - Starts the `TimerService` when a valid time is confirmed.
 */
@Composable
fun TimerSetupScreen() {
    // Get the current context
    val context = LocalContext.current

    // Obtain the ViewModel for managing the selected time
    val viewModel = viewModel<TimerSetupViewModel>()

    // Observe the selected time from the ViewModel
    val selectedTime by viewModel.selectedTime.collectAsStateWithLifecycle()

    /**
     * Lambda function to handle time confirmation.
     * - If the selected time is not 00:00:00, update the ViewModel and start the timer service.
     */
    val onTimeConfirm: (LocalTime) -> Unit = { time ->
        if (time != LocalTime.of(0, 0, 0)) {
            viewModel.updateSelectedTime(time)
            Intent(context, TimerService::class.java).also {
                it.action = TimerService.Actions.START.toString()
                it.putExtra("selectedTime", time.toString())
                context.startService(it)
            }
        }
    }

    // Display a TimePicker for selecting the countdown time
    TimePicker(
        onTimeConfirm = onTimeConfirm,
        time = selectedTime,
        showSeconds = true,
        modifier = Modifier.fillMaxSize()
    )
}