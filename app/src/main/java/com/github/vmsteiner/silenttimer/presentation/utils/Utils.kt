package com.github.vmsteiner.silenttimer.presentation.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for time formatting and other general operations.
 */
object Utils {

    /**
     * Formats a given time in milliseconds into a human-readable string.
     *
     * ## Format:
     * - If the time is **1 hour or more** → `"HH:MM:SS"`
     * - If the time is **less than an hour** → `"MM:SS"`
     *
     * ## Example Usage:
     * ```kotlin
     * val formattedTime = Utils.formatTime(3661000) // "01:01:01"
     * ```
     *
     * @param millis The time duration in milliseconds.
     * @return A formatted string representing the time.
     */
    fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}