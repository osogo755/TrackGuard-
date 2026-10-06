package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_points")
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val networkStatus: String = "Unknown", // "Wi-Fi", "Cellular", "Offline"
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "GPS", // "GPS", "Network", "Last-Known-Shutdown-Snapshot", "Emergency-Ping", "Remote-Command"
    val isShutdownSnapshot: Boolean = false,
    val note: String = ""
)
