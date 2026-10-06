package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.repository.LocationRepository
import com.example.util.LocationTrackerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var trackingJob: Job? = null
    private lateinit var locationHelper: LocationTrackerHelper
    private lateinit var repository: LocationRepository

    private val shutdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_SHUTDOWN ||
                action == "android.intent.action.QUICKBOOT_POWEROFF" ||
                action == Intent.ACTION_BATTERY_LOW
            ) {
                // Device is shutting down or battery critically low!
                // Capture critical last-known offline snapshot immediately!
                serviceScope.launch {
                    val loc = locationHelper.getCurrentLocation()
                    if (loc != null) {
                        repository.recordLocationPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            speed = loc.speed,
                            source = "Last-Known-Shutdown-Snapshot",
                            isShutdownSnapshot = true,
                            note = "Captured during device shutdown / power off sequence"
                        )
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationTrackerHelper(this)
        repository = LocationRepository(this)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SHUTDOWN)
            addAction("android.intent.action.QUICKBOOT_POWEROFF")
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        ContextCompat.registerReceiver(
            this,
            shutdownReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_TRACKING) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildTrackingNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, buildTrackingNotification())
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        startLocationTracking()

        return START_STICKY
    }

    private fun startLocationTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            // First take an immediate snapshot
            val immediate = locationHelper.getCurrentLocation()
            if (immediate != null) {
                repository.recordLocationPoint(
                    latitude = immediate.latitude,
                    longitude = immediate.longitude,
                    accuracy = immediate.accuracy,
                    altitude = immediate.altitude,
                    speed = immediate.speed,
                    source = "GPS",
                    isShutdownSnapshot = false,
                    note = "Tracking initialized"
                )
            }

            // Then listen for continuous location updates
            locationHelper.getLocationUpdates(30000L)
                .catch { /* continue on failure */ }
                .collect { loc ->
                    repository.recordLocationPoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        altitude = loc.altitude,
                        speed = loc.speed,
                        source = "GPS",
                        isShutdownSnapshot = false,
                        note = "Periodic background telemetry"
                    )
                }
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(shutdownReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        stopTracking()
        super.onDestroy()
    }

    private fun buildTrackingNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("TrackGuard Shield Active")
            .setContentText("Real-time theft telemetry & power-off guardian running.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Theft Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device location and last-known offline coordinates"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "trackguard_tracking_channel"
        const val NOTIFICATION_ID = 9992
        const val ACTION_START_TRACKING = "com.example.trackguard.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.trackguard.ACTION_STOP_TRACKING"

        fun start(context: Context) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasLocationPermission) {
                return
            }

            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Prevent crash if app not in eligible foreground state
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
