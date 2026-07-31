package com.github.vmsteiner.silenttimer.presentation.service

import android.app.AlarmManager
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
import com.github.vmsteiner.silenttimer.presentation.receiver.TimerReceiver
import com.github.vmsteiner.silenttimer.presentation.ui.MainActivity
import com.github.vmsteiner.silenttimer.presentation.utils.HALFTIME_ALERT_KEY
import com.github.vmsteiner.silenttimer.presentation.utils.TimerManager
import com.github.vmsteiner.silenttimer.presentation.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * TimerService is a foreground service that handles the countdown timer.
 * It starts, manages, and stops the timer while ensuring that the device does not enter sleep mode.
 * Additionally, it updates complications and vibrates at specific points in the countdown.
 */
class TimerService: Service(){
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationId: Int = 1

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
            Actions.START.toString() -> {
                serviceScope.launch {
                    start(intent)
                }
            }
            Actions.STOP.toString() -> stopTimerAndVibration()
            TimerReceiver.ACTION_HALFTIME -> handleHalftime()
            TimerReceiver.ACTION_FINISH -> handleFinish()
        }
        return START_NOT_STICKY
    }

    /**
     * Starts the countdown timer based on the selected time.
     *
     * @param intent The intent containing the selected time in ISO_LOCAL_TIME format.
     */
    private suspend fun start(intent: Intent) {
        if (TimerManager.timerState.value.isRunning) {
            Log.d("TimerService", "Timer already active, ignoring start request.")
            return
        }

        val timeString = intent.getStringExtra("selectedTime") ?: run {
            Log.e("TimerService", "No selectedTime extra found in intent")
            return
        }

        val selectedTime = try {
            LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME)
        } catch (e: DateTimeParseException) {
            Log.e("TimerService", "Failed to parse selectedTime: $timeString", e)
            return
        }

        val hours = selectedTime.hour
        val minutes = selectedTime.minute
        val seconds = selectedTime.second

        val initialTimeInMillis = TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())

        // Fetch preference before scheduling any alarms to ensure determinism
        val isHalftimeAlertEnabled = applicationContext.dataStore.data
            .map { prefs -> prefs[HALFTIME_ALERT_KEY] ?: true }
            .first()

        TimerManager.start(initialTimeInMillis)

        withContext(Dispatchers.Main) {
            startForeground(notificationId, buildNotification())
        }

        scheduleAlarms(initialTimeInMillis, isHalftimeAlertEnabled)
        triggerComplicationUpdate(applicationContext)
    }

    /**
     * Stops the timer and clears all related resources.
     */
    private fun stopTimer() {
        cancelAlarms()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Stops the timer and any ongoing vibrations, resets the timer state, and updates complications.
     */
    private fun stopTimerAndVibration() {
        stopVibration()
        stopTimer()
        TimerManager.stop()
        triggerComplicationUpdate(applicationContext)
    }

    /**
     * Builds a persistent notification for the foreground service to keep the timer running.
     */
    private fun buildNotification(isFinished: Boolean = false): Notification {
        // No ActivityOptions (e.g. pendingIntentCreatorBackgroundActivityStartMode) are needed here.
        // This PendingIntent is launched from an explicit user tap on the notification, which is an
        // allowed background activity launch and does not require BAL opt-in.
        val launchActivityIntent = Intent(this, MainActivity::class.java)
        val launchActivityPendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder  = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Silent Timer")
            .setContentText(if (isFinished) "Timer finished!" else "Timer is active")
            .setSmallIcon(R.drawable.baseline_hourglass_empty_24)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(launchActivityPendingIntent)

        // Calculate the timestamp when the timer should hit zero
        val timerState = TimerManager.timerState.value

        val ongoingActivityStatus = if (isFinished) {
            Status.Builder()
                .addPart("status", Status.TextPart("Finished"))
                .build()
        } else {
            val timerPart = Status.TimerPart(
                timerState.endTimeElapsedRealtime,    // When the timer will hit zero
                -1L,                                  // -1L if not paused
                timerState.durationMillis             // The total duration of the timer
            )
            Status.Builder()
                .addPart("timer", timerPart)
                .build()
        }

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

    private fun createTimerIntent(action: String): Intent {
        return Intent(this, TimerReceiver::class.java).apply {
            this.action = action
        }
    }

    /**
     * Schedules exact alarms for the halfway point and completion.
     */
    private fun scheduleAlarms(durationMillis: Long, isHalftimeAlertEnabled: Boolean) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val endTime = SystemClock.elapsedRealtime() + durationMillis
        val halfwayTime = SystemClock.elapsedRealtime() + (durationMillis / 2)

        // Finish Alarm
        val finishPendingIntent = PendingIntent.getBroadcast(
            this, 1, createTimerIntent(TimerReceiver.ACTION_FINISH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endTime, finishPendingIntent)

        // Halftime Alarm
        if (isHalftimeAlertEnabled && durationMillis > 2000) { // Only if timer > 2s
            val halftimePendingIntent = PendingIntent.getBroadcast(
                this, 2, createTimerIntent(TimerReceiver.ACTION_HALFTIME),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, halfwayTime, halftimePendingIntent)
        }
    }

    private fun cancelAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(1 to TimerReceiver.ACTION_FINISH, 2 to TimerReceiver.ACTION_HALFTIME).forEach { (id, action) ->
            PendingIntent.getBroadcast(
                this, id, createTimerIntent(action),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it) }
        }
    }

    private fun handleHalftime() {
        Log.d("TimerService", "Halftime alarm received")
        vibrateShort()
    }

    private fun handleFinish() {
        Log.d("TimerService", "Finish alarm received")
        vibrate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(notificationId, buildNotification(isFinished = true))
        triggerComplicationUpdate(applicationContext)
    }

    /**
     * Triggers a long vibration when the timer ends.
     */
    private fun vibrate() {
        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator// Vibrate for 500ms, pause for 500ms, repeat indefinitely
        val pattern = longArrayOf(0, 500, 500, 500)
        val vibrationEffect = VibrationEffect.createWaveform(pattern, 0) // 0 means repeat indefinitely
        vibrator.vibrate(vibrationEffect)
    }

    /**
     * Triggers a short vibration in the half-time of the countdown.
     */
    private fun vibrateShort() {
        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
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
        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
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

    override fun onDestroy() {
        serviceScope.cancel()
        stopVibration()
        super.onDestroy()
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
