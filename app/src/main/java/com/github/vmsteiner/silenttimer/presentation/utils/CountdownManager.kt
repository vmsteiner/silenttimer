package com.github.vmsteiner.silenttimer.presentation.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton object responsible for managing the countdown timer state.
 *
 * This object uses a `StateFlow` to hold and manage the current countdown time (in milliseconds).
 * It provides a centralized and easily accessible place to update and observe the countdown timer.
 *  * The reasons for using a singleton for this purpose are:
 *  * 1. Simplicity: Since we only have a single countdown timer, a singleton pattern
 *  *    provides a straightforward way to manage and access this shared state.
 *  * 2. Global Access: The singleton ensures that the countdown timer can be accessed from
 *  *    anywhere in the app, such as from the TimerService and various Composables.
 *  * 3. Thread-Safety: Kotlin's singleton implementation ensures that the object is
 *  *    instantiated in a thread-safe manner, making it suitable for concurrent updates.
 *  * 4. Minimal Overhead: A singleton pattern avoids the complexity and overhead of
 *  *    additional layers like a repository, which would be unnecessary for a simple
 *  *    shared state management.
 *  *
 *  * Usage:
 *  * - To update the countdown timer value, call `CountdownManager.updateCountdownTime(newTime)`.
 *  * - To observe the countdown timer value, collect the `countdownTime` StateFlow.
 */
object CountdownManager {
    /**
     * A private mutable state flow that holds the current countdown time in milliseconds.
     *
     * This is a private mutable flow used internally to store the countdown time. The value can
     * be updated by calling the `updateCountdownTime` function, but it is only exposed as a
     * read-only flow for external observers.
     */
    private val _countdownTime = MutableStateFlow(0L)

    /**
     * A public immutable state flow that exposes the current countdown time.
     *
     * This flow is exposed publicly as a read-only `StateFlow` to allow other components of the app
     * to observe changes in the countdown time. Observers can subscribe to this flow to react to
     * changes in the timer.
     */
    val countdownTime: StateFlow<Long> get() = _countdownTime

    /**
     * Updates the countdown time with a new value.
     *
     * This function is used to update the countdown time stored in the internal `MutableStateFlow`.
     * It allows other components to modify the current countdown time.
     *
     * @param newTime The new countdown time in milliseconds.
     */
    fun updateCountdownTime(newTime: Long) {
        _countdownTime.value = newTime
    }
}
