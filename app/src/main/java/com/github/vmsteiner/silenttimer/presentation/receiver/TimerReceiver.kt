package com.github.vmsteiner.silenttimer.presentation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.vmsteiner.silenttimer.presentation.service.TimerService

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("TimerReceiver", "Received action: $action")

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            this.action = action
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_HALFTIME = "com.github.vmsteiner.silenttimer.ACTION_HALFTIME"
        const val ACTION_FINISH = "com.github.vmsteiner.silenttimer.ACTION_FINISH"
    }
}
