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
        private const val KEY_AUTO_OFF_ENABLED = "auto_off_enabled"
        private const val KEY_AUTO_OFF_HOUR = "auto_off_hour"
        private const val KEY_AUTO_OFF_MINUTE = "auto_off_minute"
        private const val KEY_AUTO_ON_ENABLED = "auto_on_enabled"
        private const val KEY_AUTO_ON_HOUR = "auto_on_hour"
        private const val KEY_AUTO_ON_MINUTE = "auto_on_minute"

        const val DEFAULT_DIMMER_LEVEL = 50
        const val MIN_DIMMER_LEVEL = 10
        const val MAX_DIMMER_LEVEL = 90
        const val DEFAULT_AUTO_OFF_HOUR = 7
        const val DEFAULT_AUTO_OFF_MINUTE = 0
        const val DEFAULT_AUTO_ON_HOUR = 22
        const val DEFAULT_AUTO_ON_MINUTE = 0
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

    // Auto-Off Einstellungen
    var isAutoOffEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OFF_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OFF_ENABLED, value).apply()

    var autoOffHour: Int
        get() = prefs.getInt(KEY_AUTO_OFF_HOUR, DEFAULT_AUTO_OFF_HOUR)
        set(value) = prefs.edit().putInt(KEY_AUTO_OFF_HOUR, value).apply()

    var autoOffMinute: Int
        get() = prefs.getInt(KEY_AUTO_OFF_MINUTE, DEFAULT_AUTO_OFF_MINUTE)
        set(value) = prefs.edit().putInt(KEY_AUTO_OFF_MINUTE, value).apply()

    // Auto-On Einstellungen
    var isAutoOnEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ON_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ON_ENABLED, value).apply()

    var autoOnHour: Int
        get() = prefs.getInt(KEY_AUTO_ON_HOUR, DEFAULT_AUTO_ON_HOUR)
        set(value) = prefs.edit().putInt(KEY_AUTO_ON_HOUR, value).apply()

    var autoOnMinute: Int
        get() = prefs.getInt(KEY_AUTO_ON_MINUTE, DEFAULT_AUTO_ON_MINUTE)
        set(value) = prefs.edit().putInt(KEY_AUTO_ON_MINUTE, value).apply()

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
