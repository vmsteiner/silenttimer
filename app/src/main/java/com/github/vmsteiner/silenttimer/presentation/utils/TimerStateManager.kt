package com.github.vmsteiner.silenttimer.presentation.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *
 * A singleton object that manages the state of a timer using Kotlin's **StateFlow**.
 *
 * ### We Can Use This Without a ViewModel:
 * - **Singleton Lifecycle**: As an `object`, it persists throughout the app's lifecycle,
 *   similar to how a `ViewModel` would.
 * - **StateFlow for Reactive UI**: `StateFlow` is lifecycle-aware in Compose and can be
 *   directly collected in UI (`collectAsState()`) without requiring a `ViewModel`.
 * - **Avoids Unnecessary Indirection**: Since `TimerStateManager` already exposes state
 *   reactively, wrapping it in a ViewModel would add no real benefit.
 *
 * ### Usage:
 * - **Observe `isTimerActive`** in a composable:
 *   val isTimerActive by TimerStateManager.isTimerActive.collectAsState()
 *
 * - **Update the timer state**:
 *   TimerStateManager.setTimerActive(true)
 */
object TimerStateManager {

    /**
     * A private **MutableStateFlow** that holds the current state of the timer.
     * - `true` if the timer is active.
     * - `false` if the timer is inactive.
     */
    private val _isTimerActive = MutableStateFlow(false)

    /**
     * A public **StateFlow** that exposes the current timer state.
     * This allows external components to observe whether the timer is active or not.
     */
    val isTimerActive: StateFlow<Boolean> get() = _isTimerActive

    /**
     * Updates the timer state.
     *
     * @param isActive `true` to set the timer as active, `false` to deactivate it.
     */
    fun setTimerActive(isActive: Boolean) {
        _isTimerActive.value = isActive
    }
}
