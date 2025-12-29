package com.pmaruhn.screendimmer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class DimmerService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private lateinit var prefsManager: PreferencesManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "dimmer_service_channel"

        const val ACTION_START = "com.pmaruhn.screendimmer.ACTION_START"
        const val ACTION_STOP = "com.pmaruhn.screendimmer.ACTION_STOP"
        const val ACTION_UPDATE = "com.pmaruhn.screendimmer.ACTION_UPDATE"

        // Broadcast action for state changes
        const val ACTION_STATE_CHANGED = "com.pmaruhn.screendimmer.ACTION_STATE_CHANGED"
        const val EXTRA_IS_ENABLED = "is_enabled"

        fun start(context: Context) {
            val intent = Intent(context, DimmerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DimmerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, DimmerService::class.java).apply {
                action = ACTION_UPDATE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        prefsManager.registerOnChangeListener(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlay()
                prefsManager.isDimmerEnabled = true
                broadcastStateChange(true)
            }
            ACTION_STOP -> {
                hideOverlay()
                prefsManager.isDimmerEnabled = false
                broadcastStateChange(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> {
                updateOverlay()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        prefsManager.unregisterOnChangeListener(this)
        hideOverlay()
        // Ensure state is synced on destroy
        prefsManager.isDimmerEnabled = false
        broadcastStateChange(false)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "dimmer_level") {
            updateOverlay()
        }
    }

    private fun broadcastStateChange(isEnabled: Boolean) {
        // Send local broadcast for in-app components
        val localIntent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_ENABLED, isEnabled)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

        // Send global broadcast for system components (like TileService)
        val globalIntent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_ENABLED, isEnabled)
            setPackage(packageName)
        }
        sendBroadcast(globalIntent)
    }

    private fun showOverlay() {
        if (overlayView != null) {
            updateOverlay()
            return
        }

        overlayView = View(this).apply {
            setBackgroundColor(calculateOverlayColor())
        }

        val params = createLayoutParams()

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun updateOverlay() {
        overlayView?.setBackgroundColor(calculateOverlayColor())
    }

    private fun calculateOverlayColor(): Int {
        val level = prefsManager.dimmerLevel
        val alpha = (level * 255 / 100).coerceIn(0, 230)
        return Color.argb(alpha, 0, 0, 0)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        // Get the real screen size including navigation bar
        val screenSize = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager?.currentWindowMetrics?.bounds
            screenSize.x = bounds?.width() ?: 0
            screenSize.y = bounds?.height() ?: 0
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealSize(screenSize)
        }

        // Add extra padding to ensure full coverage
        val extraPadding = 200

        return WindowManager.LayoutParams(
            screenSize.x + extraPadding,
            screenSize.y + extraPadding,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = -extraPadding / 2
            y = -extraPadding / 2
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            // Cover display cutouts (notch) on Android P+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DimmerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, prefsManager.dimmerLevel))
            .setSmallIcon(R.drawable.ic_brightness)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_brightness, getString(R.string.notification_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
