package com.github.vmsteiner.silenttimer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun SilentTimerTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize the app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        content = content
    )
}