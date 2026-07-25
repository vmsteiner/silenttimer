package com.github.vmsteiner.silenttimer.presentation.utils

import android.os.SystemClock

data class TimerState(
    val isRunning: Boolean = false,
    val durationMillis: Long = 0L,
    val endTimeElapsedRealtime: Long = 0L
) {

    fun remainingTime(now: Long = SystemClock.elapsedRealtime()): Long {
        return maxOf(0L, endTimeElapsedRealtime - now)
    }
}