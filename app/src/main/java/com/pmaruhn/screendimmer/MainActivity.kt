package com.pmaruhn.screendimmer

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pmaruhn.screendimmer.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var scheduleManager: ScheduleManager

    companion object {
        private const val TAG = "ScreenDimmer"
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        private const val BATTERY_OPTIMIZATION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate started")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "Layout inflated")

            prefsManager = PreferencesManager(this)
            scheduleManager = ScheduleManager(this)
            Log.d(TAG, "Managers created")

            setupUI()
            Log.d(TAG, "UI setup complete")
            checkOverlayPermission()
            Log.d(TAG, "onCreate complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateUI()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    private fun setupUI() {
        binding.seekBarDimLevel.max = PreferencesManager.MAX_DIMMER_LEVEL - PreferencesManager.MIN_DIMMER_LEVEL
        binding.seekBarDimLevel.progress = prefsManager.dimmerLevel - PreferencesManager.MIN_DIMMER_LEVEL

        binding.seekBarDimLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val level = progress + PreferencesManager.MIN_DIMMER_LEVEL
                prefsManager.dimmerLevel = level
                binding.textDimLevel.text = getString(R.string.dim_level_value, level)

                if (prefsManager.isDimmerEnabled) {
                    DimmerService.update(this@MainActivity)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.switchDimmer.setOnCheckedChangeListener { _, isChecked ->
            if (!Settings.canDrawOverlays(this)) {
                binding.switchDimmer.isChecked = false
                requestOverlayPermission()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                DimmerService.start(this)
            } else {
                DimmerService.stop(this)
            }
        }

        binding.switchStartOnBoot.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.startOnBoot = isChecked
        }

        binding.buttonGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }

        // Battery Optimization Button
        binding.buttonDisableBatteryOptimization.setOnClickListener {
            requestDisableBatteryOptimization()
        }

        // Auto-Off Einstellungen
        binding.switchAutoOff.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.isAutoOffEnabled = isChecked
            scheduleManager.scheduleAutoOff()
            updateScheduleUI()
        }

        binding.layoutAutoOffTime.setOnClickListener {
            showTimePickerDialog(
                prefsManager.autoOffHour,
                prefsManager.autoOffMinute
            ) { hour, minute ->
                prefsManager.autoOffHour = hour
                prefsManager.autoOffMinute = minute
                scheduleManager.scheduleAutoOff()
                updateScheduleUI()
            }
        }

        // Auto-On Einstellungen
        binding.switchAutoOn.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.isAutoOnEnabled = isChecked
            scheduleManager.scheduleAutoOn()
            updateScheduleUI()
        }

        binding.layoutAutoOnTime.setOnClickListener {
            showTimePickerDialog(
                prefsManager.autoOnHour,
                prefsManager.autoOnMinute
            ) { hour, minute ->
                prefsManager.autoOnHour = hour
                prefsManager.autoOnMinute = minute
                scheduleManager.scheduleAutoOn()
                updateScheduleUI()
            }
        }
    }

    private fun updateUI() {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val isEnabled = prefsManager.isDimmerEnabled
        val isBatteryOptimized = isBatteryOptimizationEnabled()

        binding.switchDimmer.isChecked = isEnabled && hasOverlayPermission
        binding.switchStartOnBoot.isChecked = prefsManager.startOnBoot
        binding.textDimLevel.text = getString(R.string.dim_level_value, prefsManager.dimmerLevel)

        // Overlay Permission Card
        if (hasOverlayPermission) {
            binding.cardPermission.visibility = android.view.View.GONE
            binding.cardSettings.alpha = 1f
            binding.cardSchedule.alpha = 1f
            binding.seekBarDimLevel.isEnabled = true
            binding.switchDimmer.isEnabled = true
            binding.switchStartOnBoot.isEnabled = true
            binding.switchAutoOff.isEnabled = true
            binding.switchAutoOn.isEnabled = true
            binding.layoutAutoOffTime.isEnabled = true
            binding.layoutAutoOnTime.isEnabled = true
        } else {
            binding.cardPermission.visibility = android.view.View.VISIBLE
            binding.cardSettings.alpha = 0.5f
            binding.cardSchedule.alpha = 0.5f
            binding.seekBarDimLevel.isEnabled = false
            binding.switchDimmer.isEnabled = false
            binding.switchStartOnBoot.isEnabled = false
            binding.switchAutoOff.isEnabled = false
            binding.switchAutoOn.isEnabled = false
            binding.layoutAutoOffTime.isEnabled = false
            binding.layoutAutoOnTime.isEnabled = false
        }

        // Battery Optimization Card
        if (isBatteryOptimized && hasOverlayPermission) {
            binding.cardBatteryOptimization.visibility = android.view.View.VISIBLE
        } else {
            binding.cardBatteryOptimization.visibility = android.view.View.GONE
        }

        updateScheduleUI()
    }

    private fun updateScheduleUI() {
        binding.switchAutoOff.isChecked = prefsManager.isAutoOffEnabled
        binding.switchAutoOn.isChecked = prefsManager.isAutoOnEnabled

        binding.textAutoOffTime.text = formatTime(prefsManager.autoOffHour, prefsManager.autoOffMinute)
        binding.textAutoOnTime.text = formatTime(prefsManager.autoOnHour, prefsManager.autoOnMinute)

        binding.layoutAutoOffTime.alpha = if (prefsManager.isAutoOffEnabled) 1f else 0.5f
        binding.layoutAutoOnTime.alpha = if (prefsManager.isAutoOnEnabled) 1f else 0.5f
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun showTimePickerDialog(
        currentHour: Int,
        currentMinute: Int,
        onTimeSet: (hour: Int, minute: Int) -> Unit
    ) {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                onTimeSet(hourOfDay, minute)
            },
            currentHour,
            currentMinute,
            true // 24-Stunden-Format
        ).show()
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun isBatteryOptimizationEnabled(): Boolean {
        return try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.let {
                !it.isIgnoringBatteryOptimizations(packageName)
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization", e)
            false // Assume not optimized if we can't check
        }
    }

    private fun requestDisableBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
        } catch (e: Exception) {
            // Fallback: Öffne allgemeine Akku-Einstellungen
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, R.string.battery_settings_not_found, Toast.LENGTH_LONG).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            BATTERY_OPTIMIZATION_REQUEST_CODE -> {
                if (!isBatteryOptimizationEnabled()) {
                    Toast.makeText(this, R.string.battery_optimization_disabled, Toast.LENGTH_SHORT).show()
                }
            }
        }
        updateUI()
    }
}
