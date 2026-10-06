package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.example.data.db.AppDatabase
import com.example.data.model.LocationPointEntity
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val locationDao = database.locationDao()

    val recentLocations: Flow<List<LocationPointEntity>> = locationDao.getRecentLocations(100)
    val lastKnownLocation: Flow<LocationPointEntity?> = locationDao.getLastKnownLocationFlow()
    val shutdownSnapshots: Flow<List<LocationPointEntity>> = locationDao.getShutdownSnapshots()
    val locationCount: Flow<Int> = locationDao.getLocationCount()

    suspend fun getLastKnownLocationSync(): LocationPointEntity? {
        return locationDao.getLastKnownLocationSync()
    }

    suspend fun recordLocationPoint(
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0f,
        altitude: Double = 0.0,
        speed: Float = 0f,
        source: String = "GPS",
        isShutdownSnapshot: Boolean = false,
        note: String = ""
    ): Long {
        val (batteryLevel, isCharging) = getBatteryInfo()
        val networkStatus = getNetworkStatus()

        val entity = LocationPointEntity(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = altitude,
            speed = speed,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            networkStatus = networkStatus,
            timestamp = System.currentTimeMillis(),
            source = source,
            isShutdownSnapshot = isShutdownSnapshot,
            note = note
        )

        return locationDao.insertLocation(entity)
    }

    suspend fun clearHistory() {
        locationDao.clearAll()
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            Pair(batteryPct, isCharging)
        } catch (e: Exception) {
            Pair(100, false)
        }
    }

    private fun getNetworkStatus(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Offline"
            val network = cm.activeNetwork ?: return "Offline"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "Offline"
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }
        } catch (e: Exception) {
            "Offline"
        }
    }
}
