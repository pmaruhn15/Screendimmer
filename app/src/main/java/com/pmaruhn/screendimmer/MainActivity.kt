package com.pmaruhn.screendimmer

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        scheduleManager = ScheduleManager(this)

        setupUI()
        checkOverlayPermission()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
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
        val hasPermission = Settings.canDrawOverlays(this)
        val isEnabled = prefsManager.isDimmerEnabled

        binding.switchDimmer.isChecked = isEnabled && hasPermission
        binding.switchStartOnBoot.isChecked = prefsManager.startOnBoot
        binding.textDimLevel.text = getString(R.string.dim_level_value, prefsManager.dimmerLevel)

        if (hasPermission) {
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }
}
