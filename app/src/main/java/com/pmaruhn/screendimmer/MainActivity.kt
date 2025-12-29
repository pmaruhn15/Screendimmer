package com.pmaruhn.screendimmer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pmaruhn.screendimmer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

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
            binding.seekBarDimLevel.isEnabled = true
            binding.switchDimmer.isEnabled = true
            binding.switchStartOnBoot.isEnabled = true
        } else {
            binding.cardPermission.visibility = android.view.View.VISIBLE
            binding.cardSettings.alpha = 0.5f
            binding.seekBarDimLevel.isEnabled = false
            binding.switchDimmer.isEnabled = false
            binding.switchStartOnBoot.isEnabled = false
        }
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
