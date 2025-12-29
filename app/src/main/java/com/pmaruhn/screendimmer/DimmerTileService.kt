package com.pmaruhn.screendimmer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DimmerTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DimmerService.ACTION_STATE_CHANGED) {
                val isEnabled = intent.getBooleanExtra(DimmerService.EXTRA_IS_ENABLED, false)
                updateTileToState(isEnabled)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)

        // Register receiver for state changes
        val filter = IntentFilter(DimmerService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        val isCurrentlyEnabled = prefsManager.isDimmerEnabled
        if (isCurrentlyEnabled) {
            // Turn OFF the dimmer
            DimmerService.stop(this)
            // State will be updated via broadcast
        } else {
            // Turn ON the dimmer
            DimmerService.start(this)
            // State will be updated via broadcast
        }
    }

    private fun updateTileState() {
        val isEnabled = prefsManager.isDimmerEnabled
        updateTileToState(isEnabled)
    }

    private fun updateTileToState(isEnabled: Boolean) {
        val tile = qsTile ?: return
        val hasPermission = Settings.canDrawOverlays(this)

        tile.state = when {
            !hasPermission -> Tile.STATE_UNAVAILABLE
            isEnabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }

        tile.label = getString(R.string.tile_label)
        tile.subtitle = when {
            !hasPermission -> getString(R.string.tile_no_permission)
            isEnabled -> getString(R.string.tile_on, prefsManager.dimmerLevel)
            else -> getString(R.string.tile_off)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.icon = Icon.createWithResource(this, R.drawable.ic_brightness)
        }

        tile.updateTile()
    }
}
