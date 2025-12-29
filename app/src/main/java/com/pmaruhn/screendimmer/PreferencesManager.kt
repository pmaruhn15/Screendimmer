package com.pmaruhn.screendimmer

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "screen_dimmer_prefs"
        private const val KEY_DIMMER_ENABLED = "dimmer_enabled"
        private const val KEY_DIMMER_LEVEL = "dimmer_level"
        private const val KEY_START_ON_BOOT = "start_on_boot"

        const val DEFAULT_DIMMER_LEVEL = 50
        const val MIN_DIMMER_LEVEL = 10
        const val MAX_DIMMER_LEVEL = 90
    }

    var isDimmerEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIMMER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DIMMER_ENABLED, value).apply()

    var dimmerLevel: Int
        get() = prefs.getInt(KEY_DIMMER_LEVEL, DEFAULT_DIMMER_LEVEL)
        set(value) {
            val clampedValue = value.coerceIn(MIN_DIMMER_LEVEL, MAX_DIMMER_LEVEL)
            prefs.edit().putInt(KEY_DIMMER_LEVEL, clampedValue).apply()
        }

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
