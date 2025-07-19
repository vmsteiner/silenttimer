package com.github.vmsteiner.silenttimer.presentation.service

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.github.vmsteiner.silenttimer.R
import com.github.vmsteiner.silenttimer.presentation.ui.MainActivity
import com.github.vmsteiner.silenttimer.presentation.utils.CountdownManager
import com.github.vmsteiner.silenttimer.presentation.utils.TimerStateManager
import java.time.Instant

/**
 * A complication data source service that provides countdown timer information
 * for watch face complications.
 */
class TimerComplicationDataSourceService : SuspendingComplicationDataSourceService() {
    /**
     * Called when a complication is activated. Logs the activation event.
     *
     * @param complicationInstanceId The ID of the activated complication instance.
     * @param type The type of the activated complication.
     */
    override fun onComplicationActivated(
        complicationInstanceId: Int,
        type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")
    }

    /**
     * Handles a request for complication data.
     *
     * @param request The complication request containing the instance ID and type.
     * @return A [ComplicationData] object containing the requested complication information.
     */
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return createComplicationData(request.complicationType)
    }

    /**
     * Called when a complication is deactivated. Logs the deactivation event.
     *
     * @param complicationInstanceId The ID of the deactivated complication instance.
     */
    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): $complicationInstanceId")
    }

    /**
     * Provides static preview data for complications in the editor UI.
     *
     * @param type The type of complication for which preview data is requested.
     * @return A [ComplicationData] object containing preview data.
     */
    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val previewTextShort = PlainComplicationText.Builder(
            text = "04:32"
        ).build()
        val previewTextLong = PlainComplicationText.Builder(
            text = "04:32:25"
        ).build()
        val contentDescription = PlainComplicationText.Builder(
            text = "Countdown remaining time: 04:32:25"
        ).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = previewTextShort,
                contentDescription = contentDescription
            )
                .setTapAction(null)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = previewTextLong,
                contentDescription = contentDescription
            )
                .setTapAction(null)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 50f, // Example value
                min = 0f,
                max = 100f, // Example max value
                contentDescription = contentDescription
            )
                .setText(previewTextShort)
                .setTapAction(null)
                .build()

            else -> throw IllegalArgumentException("Unsupported complication type: $type")
        }
    }

    /**
     * Creates ComplicationData for the given ComplicationType.
     *
     * This function determines the appropriate complication data based on the current
     * state of the timer (active or inactive). It builds the necessary text,
     * content description, and monochromatic image, and then constructs the final
     * ComplicationData object.
     *
     * @param complicationType The type of complication for which to create data.
     *                         This determines the shape and size of the complication.
     * @return ComplicationData for the specified type, or null if no data can be
     *         created for the given type.
     *
     *         When the timer is active, the complication will display a countdown
     *         time remaining using TimeDifferenceComplicationText, styled as a
     *         stopwatch. The content description will indicate "Countdown ongoing."
     *         The monochromatic image will be an hourglass icon.
     *
     *         When the timer is not active, the complication will display the text
     *         "Set" using PlainComplicationText. The content description will indicate
     *         "Timer is not set." The monochromatic image will still be an hourglass icon.
     *
     *         The complication text and content description are generated dynamically
     *         based on the timer's state. The monochromatic image is currently static.
     */
    private fun createComplicationData(complicationType: ComplicationType): ComplicationData? {
        val monochromaticImage = MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.baseline_hourglass_empty_lightgray_24)
        ).build()

        val (complicationText, contentDescription) = if (TimerStateManager.isTimerActive.value) {
            val timeLeftInMillis = CountdownManager.countdownTime.value
            val instant = Instant.now()
            val targetInstant = instant.plusMillis(timeLeftInMillis)
            val countDownTimeReference = CountDownTimeReference(targetInstant)

            val complicationText = TimeDifferenceComplicationText.Builder(
                TimeDifferenceStyle.STOPWATCH,
                countDownTimeReference
            ).build()
            val contentDescription = PlainComplicationText.Builder(text = "Countdown ongoing").build()

            complicationText to contentDescription
        } else {
            val complicationText = PlainComplicationText.Builder(text = "Set").build()
            val contentDescription = PlainComplicationText.Builder(text = "Timer is not set").build()

            complicationText to contentDescription
        }

        return buildComplicationData(complicationType, complicationText, contentDescription, monochromaticImage)
    }

    /**
     * Builds a [ComplicationData] object for the given type and values.
     *
     * @param complicationType The type of complication being requested.
     * @param complicationText The main text for the complication.
     * @param contentDescription The content description for accessibility.
     * @param monochromaticImage The monochromatic image to display.
     * @return A [ComplicationData] object or null if the type is unsupported.
     */
    private fun buildComplicationData(
        complicationType: ComplicationType,
        complicationText: ComplicationText,
        contentDescription: ComplicationText,
        monochromaticImage: MonochromaticImage
    ): ComplicationData? {
        return when (complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = complicationText,
                contentDescription = contentDescription
            )
                .setTapAction(createComplicationTapAction())
                .setMonochromaticImage(monochromaticImage)
                .build()

            else -> null
        }
    }

    /**
     * Creates a PendingIntent for the complication tap action to open the main activity.
     *
     * @return A [PendingIntent] for launching [MainActivity].
     */
    private fun createComplicationTapAction(): PendingIntent {

        // Check for API level 36+
        val activityOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityOptions.makeBasic().apply {
                // Avoid BAL activity warning
                // https://developer.android.com/guide/components/activities/background-starts#exceptions
                pendingIntentCreatorBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            }.toBundle()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                pendingIntentCreatorBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
        }
        else {
            null // API < 34, so no activityOptions
        }

        return PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptions
        )
    }

    /**
     * Companion object holding constants for logging.
     */
    companion object {
        private const val TAG = "TimerCompDataSourceServ"
    }
}