package com.github.vmsteiner.silenttimer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

/**
 * ViewModel for managing the timer setup state.
 *
 * This ViewModel handles the state related to the timer setup, specifically
 * the selected time that the user picks. It provides a flow of the selected time
 * that can be observed by the UI, allowing the timer setup screen to update dynamically.
 */
class TimerSetupViewModel: ViewModel(){

    /**
     * A private mutable state flow that holds the selected time for the timer.
     *
     * This property is used internally to store the user's selected time in
     * the timer setup screen. It is initialized with a default value of 00:00:00
     * (midnight).
     */
    private val _selectedTime = MutableStateFlow(LocalTime.of(0, 0, 0))

    /**
     * A public immutable state flow that exposes the selected time.
     *
     * This property is used to allow other components (such as the UI) to
     * observe the selected time without directly modifying it.
     * It is a read-only version of [_selectedTime] that is safe to use externally.
     */
    val selectedTime = _selectedTime.asStateFlow()

    /**
     * Updates the selected time.
     *
     * This method is used to update the value of the selected time when the user
     * interacts with the timer setup UI (e.g., changing hours, minutes, or seconds).
     *
     * @param newSelectedTime The new selected time to update to.
     */
    fun updateSelectedTime(newSelectedTime: LocalTime) {
        _selectedTime.value = newSelectedTime
    }
}



