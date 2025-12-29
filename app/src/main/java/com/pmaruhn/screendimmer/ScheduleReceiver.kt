package com.pmaruhn.screendimmer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_AUTO_OFF = "com.pmaruhn.screendimmer.ACTION_AUTO_OFF"
        const val ACTION_AUTO_ON = "com.pmaruhn.screendimmer.ACTION_AUTO_ON"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefsManager = PreferencesManager(context)

        when (intent.action) {
            ACTION_AUTO_OFF -> {
                if (prefsManager.isAutoOffEnabled && prefsManager.isDimmerEnabled) {
                    DimmerService.stop(context)
                }
                // Alarm für den nächsten Tag neu planen
                ScheduleManager(context).scheduleAutoOff()
            }
            ACTION_AUTO_ON -> {
                if (prefsManager.isAutoOnEnabled &&
                    !prefsManager.isDimmerEnabled &&
                    Settings.canDrawOverlays(context)) {
                    DimmerService.start(context)
                }
                // Alarm für den nächsten Tag neu planen
                ScheduleManager(context).scheduleAutoOn()
            }
        }
    }
}
