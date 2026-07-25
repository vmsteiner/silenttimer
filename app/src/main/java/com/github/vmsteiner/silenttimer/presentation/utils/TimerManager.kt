package com.github.vmsteiner.silenttimer.presentation.utils

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TimerManager {

    private val _timerState = MutableStateFlow(TimerState())

    val timerState: StateFlow<TimerState>
        get() = _timerState

    fun start(durationMillis: Long) {
        _timerState.value = TimerState(
            isRunning = true,
            durationMillis = durationMillis,
            endTimeElapsedRealtime = SystemClock.elapsedRealtime() + durationMillis
        )
    }

    fun stop() {
        _timerState.value = TimerState()
    }
}