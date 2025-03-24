package com.github.vmsteiner.silenttimer.presentation.service

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.github.vmsteiner.silenttimer.R
import com.github.vmsteiner.silenttimer.presentation.ui.MainActivity
import com.github.vmsteiner.silenttimer.presentation.utils.CountdownManager
import com.github.vmsteiner.silenttimer.presentation.utils.TimerStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * TimerService is a foreground service that handles the countdown timer.
 * It starts, manages, and stops the timer while ensuring that the device does not enter sleep mode.
 * Additionally, it updates complications and vibrates at specific points in the countdown.
 */
class TimerService: Service(){
    private var countdownJob: Job? = null
    private var timeLeftInMillis: Long = 0
    private var initialTimeInMillis: Long = 0
    private val notificationId: Int = 1
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Handles start and stop commands for the timer.
     *
     * @param intent The intent containing the action to be performed.
     * @param flags Additional data about the start request.
     * @param startId Unique ID for the request.
     * @return START_NOT_STICKY ensures the service does not restart automatically if killed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            Actions.START.toString() -> start(intent)
            Actions.STOP.toString() -> stopTimerAndVibration()
        }
        return START_NOT_STICKY
    }

    /**
     * Starts the countdown timer based on the selected time.
     *
     * @param intent The intent containing the selected time in ISO_LOCAL_TIME format.
     */
    private fun start(intent: Intent) {
        val timeString = intent.getStringExtra("selectedTime")
        val selectedTime = LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME)

        val hours = selectedTime.hour
        val minutes = selectedTime.minute
        val seconds = selectedTime.second

        initialTimeInMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
        timeLeftInMillis = initialTimeInMillis

        TimerStateManager.setTimerActive(true)

        startForeground(notificationId, buildNotification())
        startCountdown()
    }

    /**
     * Stops the timer and clears all related resources.
     */
    private fun stopTimer() {
        countdownJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Stops the timer and any ongoing vibrations, resets the timer state, and updates complications.
     */
    private fun stopTimerAndVibration() {
        stopVibration()
        stopTimer()
        TimerStateManager.setTimerActive(false)
        triggerComplicationUpdate(applicationContext)
        CountdownManager.updateCountdownTime(0L) // Reset the countdown time
    }

    /**
     * Builds a persistent notification for the foreground service to keep the timer running.
     */
    private fun buildNotification(): Notification {

        // Check for API level 34+
        val activityOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                // This *should* avoid BAL activity warning, but doesn't work...
                // https://developer.android.com/guide/components/activities/background-starts#exceptions
                setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }.toBundle()
        } else {
            null // API < 34, so no activityOptions
        }

        val launchActivityPendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptions
        )

        val notificationBuilder  = NotificationCompat.Builder(this, "timer_channel")
            .setSmallIcon(R.drawable.baseline_hourglass_empty_24)
            .setContentText("Timer is active")
            .setOngoing(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(launchActivityPendingIntent)

        // Calculate the timestamp when the timer should hit zero
        // Note the use of SystemClock.elapsedRealtime(), not System.currentTimeMillis() to avoid millis
        val timeZeroMillis = SystemClock.elapsedRealtime() + timeLeftInMillis

        // Create a TimerPart for the ongoing activity
        val timerPart = Status.TimerPart(
            timeZeroMillis,    // When the timer will hit zero
            -1L,               // -1L if not paused
            initialTimeInMillis  // The total duration of the timer
        )

        val ongoingActivityStatus = Status.Builder()
            // Sets the text used across various surfaces.
            .addPart("timer", timerPart)
            .build()

        val ongoingActivity = OngoingActivity.Builder(this, notificationId, notificationBuilder)
            .setStatus(ongoingActivityStatus)
            .setCategory(Notification.CATEGORY_ALARM)
            .setAnimatedIcon(R.drawable.animated_hourglass)
            .setStaticIcon(R.drawable.baseline_hourglass_empty_lightgray_24)
            .build()

        ongoingActivity.apply(this)

        val notification = notificationBuilder.build()
        return notification
    }

    /**
     * Starts the countdown and ensures the device stays awake.
     */
    private fun startCountdown() {
        val halfwayPoint = initialTimeInMillis / 2
        var hasBuzzedAtHalfway = false

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SilentTimer::Wakelock").apply {
                try {
                    acquire(timeLeftInMillis + 10000)
                } catch (e: Exception){
                    Log.e("TimerService", "WakeLock acquisition failed", e)
                }
            }
        }

        countdownJob = CoroutineScope(Dispatchers.Default).launch {
            Log.d("TimerService", "Timer started")
            triggerComplicationUpdate(applicationContext)
            try{
                while (timeLeftInMillis > 0) {
                    timeLeftInMillis -= 1000 // Decrease by 1 second
                    CountdownManager.updateCountdownTime(timeLeftInMillis)

                    // Check if the timer has reached the halfway point and buzz if it hasn't buzzed yet
                    if (!hasBuzzedAtHalfway && timeLeftInMillis <= halfwayPoint) {
                        withContext(Dispatchers.Main) {
                            vibrateShort()
                        }
                        hasBuzzedAtHalfway = true
                    }

                    delay(1000) // Delay for 1 second
                }
                Log.d("TimerService", "Timer ended")
                withContext(Dispatchers.Main) {
                    vibrate()
                }
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                wakeLock = null
            }
        }
    }

    /**
     * Triggers a long vibration when the timer ends.
     */
    private fun vibrate() {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator// Vibrate for 500ms, pause for 500ms, repeat indefinitely
        val pattern = longArrayOf(0, 500, 500, 500)
        val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // 0 means repeat indefinitely
        vibrator.vibrate(vibrationEffect)
    }

    /**
     * Triggers a short vibration in the half-time of the countdown.
     */
    private fun vibrateShort() {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        // Define a single, short vibration (e.g., 200ms)
        val vibrationDuration = 400L // Adjust the duration as needed

        // Create a one-time vibration effect
        val vibrationEffect = VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE)

        // Trigger the vibration
        vibrator.vibrate(vibrationEffect)
    }

    /**
     * Stops ongoing vibration.
     */
    private fun stopVibration() {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.cancel()
    }

    /**
     * Handles the scenario when the user removes the app from the recent tasks list
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopTimerAndVibration()
    }

    /**
     * Requests an update for all complications.
     */
    private fun triggerComplicationUpdate(context: Context) {
        val updateRequester = ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, TimerComplicationDataSourceService::class.java)
        )
        updateRequester.requestUpdateAll()
    }

    enum class Actions {
        START, STOP
    }
}
