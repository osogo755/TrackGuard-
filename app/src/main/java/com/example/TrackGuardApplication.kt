package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.pref.SecurityPreferences
import com.example.data.repository.LocationRepository

class TrackGuardApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val locationRepository by lazy { LocationRepository(this) }
    val securityPreferences by lazy { SecurityPreferences(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
