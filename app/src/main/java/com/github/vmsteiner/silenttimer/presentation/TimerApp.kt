/**
 * TimerApp Class
 *
 * This class extends `Application` and is used for global initialization tasks.
 * It has to be defined in the <application> section of the AndroidManifest.xml
 * In this example, it creates a notification channel that will be used throughout the app.
 *
 * - `onCreate()`: Called when the application starts. Here, we create the notification channel.
 * - `createNotificationChannel()`: Sets up the notification channel with specific configurations.
 *
 * Why use `Application` class for this?
 * - Ensures global setup is done once before any other component (activities, services, etc.) is created.
 * - Centralizes configuration, reducing redundancy and ensuring consistency.
 */

package com.github.vmsteiner.silenttimer.presentation

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class TimerApp: Application() {

    /**
     * Called when the application starts.
     *
     * This method is called by the system when the app is launched. In this method,
     * we initialize the notification channel for timer-related notifications by calling
     * `createNotificationChannel()`.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for timer-related notifications.
     *
     * This method sets up the notification channel, which is required for showing notifications.
     * The channel is configured with a unique ID, a name for the channel,
     * and an importance level. It also disables vibration and sound
     * for the notifications associated with this channel.
     */
    private fun createNotificationChannel() {
        // Create a NotificationChannel with a unique ID and name
        val channel = NotificationChannel(
            "timer_channel",
            "Timer Notifications",
            NotificationManager.IMPORTANCE_DEFAULT // Importance level
        ).apply {
            enableVibration(false)
            //It's essential to also disable the sound
            // Otherwise the device will still vibrate with every update through the OS
            setSound(null, null)
        }

        // Get the NotificationManager system service and create the channel
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}