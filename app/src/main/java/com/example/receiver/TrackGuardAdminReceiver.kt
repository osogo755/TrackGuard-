package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.repository.LocationRepository
import com.example.util.LocationTrackerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackGuardAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "TrackGuard Device Admin Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "TrackGuard Device Admin Deactivated", Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        // Unauthorized unlock attempt! Log snapshot silently
        CoroutineScope(Dispatchers.IO).launch {
            val helper = LocationTrackerHelper(context)
            val repo = LocationRepository(context)
            val loc = helper.getCurrentLocation()
            if (loc != null) {
                repo.recordLocationPoint(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    speed = loc.speed,
                    source = "Unauthorized-Unlock-Attempt",
                    isShutdownSnapshot = false,
                    note = "Failed lock screen password attempt detected"
                )
            }
        }
    }
}
