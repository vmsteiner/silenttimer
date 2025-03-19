package com.github.vmsteiner.silenttimer.presentation.ui

import android.Manifest
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.github.vmsteiner.silenttimer.AppNavigation
import com.github.vmsteiner.silenttimer.presentation.service.TimerComplicationDataSourceService
import com.github.vmsteiner.silenttimer.presentation.theme.SilentTimerTheme

/**
 * The main entry point of the Silent Timer app.
 *
 * This activity initializes the app UI, requests necessary permissions,
 * and updates complications when the app is reopened (in case of force stop).
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is first created.
     * Initializes the UI, requests permissions, and updates the complications.
     *
     * @param savedInstanceState A Bundle containing the activity’s previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request notification permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            0
        )

        setTheme(android.R.style.Theme_DeviceDefault)

        // Update complications on app reopen
        updateComplication()

        setContent {
            SilentTimerTheme {
                AppNavigation()
            }
        }
    }

    /**
     * Updates watch face complications.
     *
     * This ensures that the complications reflect the correct state
     *  when the app is reopened (important after a force stop).
     */
    private fun updateComplication() {
        // Request complication update
        val updateRequester = ComplicationDataSourceUpdateRequester.create(
            this,
            ComponentName(this, TimerComplicationDataSourceService::class.java)
        )
        updateRequester.requestUpdateAll()
    }
}