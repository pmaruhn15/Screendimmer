package com.pmaruhn.screendimmer

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DimmerTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
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
            updateTileToState(false)
        } else {
            // Turn ON the dimmer
            DimmerService.start(this)
            updateTileToState(true)
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
