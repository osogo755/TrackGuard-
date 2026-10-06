package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.lockdown.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SirenService : Service() {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var sirenJob: Job? = null
    private var vibratorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SIREN) {
            stopSiren()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildSirenNotification())
        startSiren()

        return START_STICKY
    }

    private fun startSiren() {
        if (isPlaying) return
        isPlaying = true

        // 1. Force alarm stream to maximum volume (bypasses silent/vibrate mode)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let { am ->
            try {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, AudioManager.FLAG_SHOW_UI)
            } catch (e: Exception) {
                // Ignore if security restriction
            }
        }

        // 2. Start synthesizing dynamic dual-frequency siren tone
        sirenJob = serviceScope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val buffer = ShortArray(bufferSize / 2)
                var phase = 0.0
                var currentFreq = 700.0
                var goingUp = true

                while (isActive && isPlaying) {
                    for (i in buffer.indices) {
                        // Modulate siren pitch between 700Hz and 1500Hz
                        if (goingUp) {
                            currentFreq += 0.05
                            if (currentFreq >= 1500.0) goingUp = false
                        } else {
                            currentFreq -= 0.05
                            if (currentFreq <= 700.0) goingUp = true
                        }

                        val sample = (sin(phase) * Short.MAX_VALUE * 0.95).toInt().toShort()
                        buffer[i] = sample
                        phase += 2.0 * PI * currentFreq / sampleRate
                        if (phase > 2.0 * PI) {
                            phase -= 2.0 * PI
                        }
                    }
                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                // AudioTrack failure fallback
            }
        }

        // 3. Start pulsing vibration
        vibratorJob = serviceScope.launch {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 400, 200, 400, 200, 800)
            while (isActive && isPlaying) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createWaveform(pattern, -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(pattern, -1)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                kotlinx.coroutines.delay(2000L)
            }
        }
    }

    private fun stopSiren() {
        isPlaying = false
        sirenJob?.cancel()
        vibratorJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    override fun onDestroy() {
        stopSiren()
        super.onDestroy()
    }

    private fun buildSirenNotification(): Notification {
        val unlockIntent = Intent(this, LockdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val unlockPendingIntent = PendingIntent.getActivity(
            this, 0, unlockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🚨 EMERGENCY SIREN ACTIVE!")
            .setContentText("Bypassing silent mode. Enter PIN to deactivate.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(unlockPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Alarm Siren",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm triggered remotely or by anti-theft protection"
                setSound(null, null)
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "trackguard_alarm_channel"
        const val NOTIFICATION_ID = 9991
        const val ACTION_START_SIREN = "com.example.trackguard.ACTION_START_SIREN"
        const val ACTION_STOP_SIREN = "com.example.trackguard.ACTION_STOP_SIREN"

        fun start(context: Context) {
            val intent = Intent(context, SirenService::class.java).apply {
                action = ACTION_START_SIREN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SirenService::class.java).apply {
                action = ACTION_STOP_SIREN
            }
            context.startService(intent)
        }
    }
}
