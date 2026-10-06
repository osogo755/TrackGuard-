package com.example.data.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.securityDataStore by preferencesDataStore(name = "trackguard_preferences")

data class SecurityConfig(
    val masterPin: String = "1234",
    val emergencyPhone: String = "+1 555-0199",
    val emergencyEmail: String = "emergency.backup@example.com",
    val lockScreenMessage: String = "THIS DEVICE IS REPORTED STOLEN. GPS TRACKING IS ACTIVE. RETURN TO OWNER IMMEDIATELY.",
    val powerOffProtectionEnabled: Boolean = true,
    val fakeShutdownEnabled: Boolean = true,
    val stealthTrackingActive: Boolean = true,
    val deviceLockdownActive: Boolean = false,
    val sirenAlarmActive: Boolean = false,
    val remoteToken: String = ""
)

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val MASTER_PIN = stringPreferencesKey("master_pin")
        val EMERGENCY_PHONE = stringPreferencesKey("emergency_phone")
        val EMERGENCY_EMAIL = stringPreferencesKey("emergency_email")
        val LOCK_MESSAGE = stringPreferencesKey("lock_screen_message")
        val POWER_OFF_PROTECTION = booleanPreferencesKey("power_off_protection")
        val FAKE_SHUTDOWN = booleanPreferencesKey("fake_shutdown")
        val STEALTH_TRACKING = booleanPreferencesKey("stealth_tracking")
        val LOCKDOWN_ACTIVE = booleanPreferencesKey("lockdown_active")
        val SIREN_ACTIVE = booleanPreferencesKey("siren_active")
        val REMOTE_TOKEN = stringPreferencesKey("remote_token")
    }

    val securityConfigFlow: Flow<SecurityConfig> = context.securityDataStore.data.map { prefs ->
        val token = prefs[Keys.REMOTE_TOKEN] ?: generateDefaultToken()
        SecurityConfig(
            masterPin = prefs[Keys.MASTER_PIN] ?: "1234",
            emergencyPhone = prefs[Keys.EMERGENCY_PHONE] ?: "+1 555-0199",
            emergencyEmail = prefs[Keys.EMERGENCY_EMAIL] ?: "emergency.backup@example.com",
            lockScreenMessage = prefs[Keys.LOCK_MESSAGE]
                ?: "THIS DEVICE IS REPORTED STOLEN. GPS TRACKING IS ACTIVE. RETURN TO OWNER IMMEDIATELY.",
            powerOffProtectionEnabled = prefs[Keys.POWER_OFF_PROTECTION] ?: true,
            fakeShutdownEnabled = prefs[Keys.FAKE_SHUTDOWN] ?: true,
            stealthTrackingActive = prefs[Keys.STEALTH_TRACKING] ?: true,
            deviceLockdownActive = prefs[Keys.LOCKDOWN_ACTIVE] ?: false,
            sirenAlarmActive = prefs[Keys.SIREN_ACTIVE] ?: false,
            remoteToken = token
        )
    }

    private fun generateDefaultToken(): String {
        return "TG-" + UUID.randomUUID().toString().take(6).uppercase()
    }

    suspend fun updateMasterPin(newPin: String) {
        context.securityDataStore.edit { it[Keys.MASTER_PIN] = newPin }
    }

    suspend fun updateEmergencyContacts(phone: String, email: String) {
        context.securityDataStore.edit {
            it[Keys.EMERGENCY_PHONE] = phone
            it[Keys.EMERGENCY_EMAIL] = email
        }
    }

    suspend fun updateLockMessage(message: String) {
        context.securityDataStore.edit { it[Keys.LOCK_MESSAGE] = message }
    }

    suspend fun setPowerOffProtection(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.POWER_OFF_PROTECTION] = enabled }
    }

    suspend fun setFakeShutdown(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.FAKE_SHUTDOWN] = enabled }
    }

    suspend fun setStealthTracking(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.STEALTH_TRACKING] = enabled }
    }

    suspend fun setLockdownActive(active: Boolean) {
        context.securityDataStore.edit { it[Keys.LOCKDOWN_ACTIVE] = active }
    }

    suspend fun setSirenActive(active: Boolean) {
        context.securityDataStore.edit { it[Keys.SIREN_ACTIVE] = active }
    }
}
