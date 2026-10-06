package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.pref.SecurityPreferences
import com.example.service.TrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurityPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                val config = prefs.securityConfigFlow.first()
                if (config.stealthTrackingActive) {
                    TrackingService.start(context)
                }
            }
        }
    }
}
