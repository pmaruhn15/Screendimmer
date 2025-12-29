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

        val isEnabled = prefsManager.isDimmerEnabled
        if (isEnabled) {
            DimmerService.stop(this)
        } else {
            DimmerService.start(this)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = prefsManager.isDimmerEnabled
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
