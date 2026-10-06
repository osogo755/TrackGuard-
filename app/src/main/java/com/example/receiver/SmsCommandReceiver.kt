package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.example.data.pref.SecurityPreferences
import com.example.data.repository.LocationRepository
import com.example.service.SirenService
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.shutdown.FakeShutdownActivity
import com.example.util.LocationTrackerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        for (sms in messages) {
            val senderNumber = sms.displayOriginatingAddress ?: continue
            val body = sms.displayMessageBody?.trim() ?: continue

            if (body.startsWith("#")) {
                handleRemoteSmsCommand(context, senderNumber, body)
            }
        }
    }

    private fun handleRemoteSmsCommand(context: Context, senderNumber: String, commandText: String) {
        val prefs = SecurityPreferences(context)
        val repo = LocationRepository(context)
        val helper = LocationTrackerHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = prefs.securityConfigFlow.first()
                val masterPin = config.masterPin
                val upperCmd = commandText.uppercase()

                // Verify that message contains the correct Master PIN
                if (!commandText.contains(masterPin)) {
                    Log.w("SmsCommandReceiver", "Unauthorized SMS command received without correct PIN: $commandText")
                    return@launch
                }

                when {
                    upperCmd.contains("LOCATE") || upperCmd.contains("WHERE") -> {
                        // 1. Fetch current or last known location
                        var loc = helper.getCurrentLocation()
                        val lastPoint = repo.getLastKnownLocationSync()

                        val lat = loc?.latitude ?: lastPoint?.latitude ?: 0.0
                        val lng = loc?.longitude ?: lastPoint?.longitude ?: 0.0
                        val acc = loc?.accuracy?.toInt() ?: lastPoint?.accuracy?.toInt() ?: 0
                        val battery = lastPoint?.batteryLevel ?: 100
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                        // 2. Record this query in Room DB
                        repo.recordLocationPoint(
                            latitude = lat,
                            longitude = lng,
                            accuracy = acc.toFloat(),
                            source = "Remote-SMS-Finder",
                            isShutdownSnapshot = false,
                            note = "Queried by remote phone: $senderNumber"
                        )

                        // 3. Send automated reply SMS with Google Maps link
                        val replyMessage = "🚨 TrackGuard: Lost phone located!\n" +
                                "Map: https://maps.google.com/?q=$lat,$lng\n" +
                                "Coords: $lat, $lng\n" +
                                "Accuracy: ±${acc}m | Battery: $battery%\n" +
                                "Time: $timeStr"

                        sendSmsReply(context, senderNumber, replyMessage)
                    }

                    upperCmd.contains("ALARM") || upperCmd.contains("SIREN") -> {
                        prefs.setSirenActive(true)
                        SirenService.start(context)
                        sendSmsReply(context, senderNumber, "🚨 TrackGuard: Emergency police siren triggered at 100% volume.")
                    }

                    upperCmd.contains("LOCK") -> {
                        prefs.setLockdownActive(true)
                        val lockIntent = Intent(context, LockdownActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(lockIntent)
                        sendSmsReply(context, senderNumber, "🔒 TrackGuard: Device lock screen activated with owner contact information.")
                    }

                    upperCmd.contains("FAKEOFF") || upperCmd.contains("SHUTDOWN") -> {
                        val shutdownIntent = Intent(context, FakeShutdownActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(shutdownIntent)
                        sendSmsReply(context, senderNumber, "⚠️ TrackGuard: Stealth fake shutdown trap engaged. GPS tracking active.")
                    }

                    upperCmd.contains("DISARM") || upperCmd.contains("STOP") -> {
                        prefs.setSirenActive(false)
                        prefs.setLockdownActive(false)
                        SirenService.stop(context)
                        sendSmsReply(context, senderNumber, "✅ TrackGuard: Siren and lockdown disarmed.")
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsCommandReceiver", "Error processing SMS command", e)
            }
        }
    }

    private fun sendSmsReply(context: Context, destinationNumber: String, text: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(destinationNumber, null, parts, null, null)
        } catch (e: Exception) {
            Log.e("SmsCommandReceiver", "Failed to send SMS reply", e)
        }
    }
}
