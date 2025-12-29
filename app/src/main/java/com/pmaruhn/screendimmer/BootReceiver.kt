package com.pmaruhn.screendimmer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefsManager = PreferencesManager(context)

            if (prefsManager.startOnBoot &&
                prefsManager.isDimmerEnabled &&
                Settings.canDrawOverlays(context)) {
                DimmerService.start(context)
            }
        }
    }
}
