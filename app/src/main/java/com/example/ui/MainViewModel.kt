package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackGuardApplication
import com.example.data.model.LocationPointEntity
import com.example.data.pref.SecurityConfig
import com.example.service.SirenService
import com.example.service.TrackingService
import com.example.ui.lockdown.LockdownActivity
import com.example.ui.shutdown.FakeShutdownActivity
import com.example.util.LocationTrackerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isLocating: Boolean = false,
    val lastCommandStatus: String? = null,
    val isDeviceAdminActive: Boolean = false
)

data class FinderTargetState(
    val targetPhone: String = "",
    val targetPin: String = "1234",
    val targetLat: Double? = null,
    val targetLng: Double? = null,
    val targetAccuracy: Float = 0f,
    val targetBattery: Int? = null,
    val targetTimestamp: Long? = null,
    val distanceMeters: Float? = null,
    val bearingDegrees: Float? = null,
    val lastPingStatus: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TrackGuardApplication
    private val locationRepo = app.locationRepository
    private val securityPrefs = app.securityPreferences
    private val trackerHelper = LocationTrackerHelper(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _finderState = MutableStateFlow(FinderTargetState())
    val finderState: StateFlow<FinderTargetState> = _finderState.asStateFlow()

    val securityConfig: StateFlow<SecurityConfig> = securityPrefs.securityConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityConfig())

    val recentLocations: StateFlow<List<LocationPointEntity>> = locationRepo.recentLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastKnownLocation: StateFlow<LocationPointEntity?> = locationRepo.lastKnownLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shutdownSnapshots: StateFlow<List<LocationPointEntity>> = locationRepo.shutdownSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationCount: StateFlow<Int> = locationRepo.locationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Start background tracking by default if configured
        viewModelScope.launch {
            securityPrefs.securityConfigFlow.collect { config ->
                if (config.stealthTrackingActive) {
                    TrackingService.start(app)
                }
            }
        }
    }

    fun refreshLocationNow() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLocating = true)
            val loc = trackerHelper.getCurrentLocation()
            if (loc != null) {
                locationRepo.recordLocationPoint(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    speed = loc.speed,
                    source = "Manual-Ping",
                    isShutdownSnapshot = false,
                    note = "Manually triggered from dashboard"
                )
            }
            _uiState.value = _uiState.value.copy(isLocating = false)
        }
    }

    fun triggerAlarm(context: Context) {
        viewModelScope.launch {
            securityPrefs.setSirenActive(true)
            SirenService.start(context)
        }
    }

    fun stopAlarm(context: Context) {
        viewModelScope.launch {
            securityPrefs.setSirenActive(false)
            SirenService.stop(context)
        }
    }

    fun triggerRemoteLock(context: Context) {
        viewModelScope.launch {
            securityPrefs.setLockdownActive(true)
            val intent = Intent(context, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    fun testPowerOffProtection(context: Context) {
        val intent = Intent(context, FakeShutdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun simulateRemoteCommand(context: Context, rawCommand: String) {
        val cmd = rawCommand.trim().uppercase()
        viewModelScope.launch {
            when {
                cmd.contains("ALARM") || cmd.contains("SIREN") -> {
                    triggerAlarm(context)
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "✅ Executed: Emergency Siren Started (Silent Mode Bypassed)"
                    )
                }
                cmd.contains("LOCK") -> {
                    triggerRemoteLock(context)
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "✅ Executed: Remote Lock & Warning Screen Activated"
                    )
                }
                cmd.contains("LOCATE") -> {
                    refreshLocationNow()
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "✅ Executed: Live GPS Ping Captured & Synced"
                    )
                }
                cmd.contains("SHUTDOWN") || cmd.contains("FAKEOFF") -> {
                    testPowerOffProtection(context)
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "✅ Executed: Stealth Power-Off Trap Triggered"
                    )
                }
                cmd.contains("DISARM") || cmd.contains("STOP") -> {
                    stopAlarm(context)
                    securityPrefs.setLockdownActive(false)
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "✅ Executed: Siren and Lockdown Disarmed"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        lastCommandStatus = "⚠️ Unknown Command. Supported: #ALARM, #LOCK, #LOCATE, #FAKEOFF, #DISARM"
                    )
                }
            }
        }
    }

    fun updateMasterPin(newPin: String) {
        viewModelScope.launch {
            securityPrefs.updateMasterPin(newPin)
        }
    }

    fun updateEmergencyContacts(phone: String, email: String) {
        viewModelScope.launch {
            securityPrefs.updateEmergencyContacts(phone, email)
        }
    }

    fun updateLockMessage(msg: String) {
        viewModelScope.launch {
            securityPrefs.updateLockMessage(msg)
        }
    }

    fun setPowerOffProtection(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setPowerOffProtection(enabled)
        }
    }

    fun setFakeShutdown(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setFakeShutdown(enabled)
        }
    }

    fun setStealthTracking(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setStealthTracking(enabled)
            if (enabled) {
                TrackingService.start(app)
            } else {
                TrackingService.stop(app)
            }
        }
    }

    fun updateFinderTarget(phone: String, pin: String) {
        _finderState.value = _finderState.value.copy(
            targetPhone = phone,
            targetPin = pin
        )
    }

    fun setFinderTargetCoordinates(lat: Double, lng: Double, accuracy: Float = 5f, battery: Int? = null) {
        _finderState.value = _finderState.value.copy(
            targetLat = lat,
            targetLng = lng,
            targetAccuracy = accuracy,
            targetBattery = battery,
            targetTimestamp = System.currentTimeMillis()
        )
        calculateProximityToTarget()
    }

    fun calculateProximityToTarget() {
        viewModelScope.launch(Dispatchers.IO) {
            val myLoc = trackerHelper.getCurrentLocation()
            val targetLat = _finderState.value.targetLat
            val targetLng = _finderState.value.targetLng

            if (myLoc != null && targetLat != null && targetLng != null) {
                val results = FloatArray(2)
                android.location.Location.distanceBetween(
                    myLoc.latitude, myLoc.longitude,
                    targetLat, targetLng,
                    results
                )
                val distance = results[0]
                val bearing = results[1]
                _finderState.value = _finderState.value.copy(
                    distanceMeters = distance,
                    bearingDegrees = bearing
                )
            }
        }
    }

    fun parseAndSetTargetFromText(rawText: String) {
        try {
            val urlRegex = Regex("q=([-+]?[0-9]*\\.?[0-9]+),([-+]?[0-9]*\\.?[0-9]+)")
            val matchUrl = urlRegex.find(rawText)
            if (matchUrl != null) {
                val lat = matchUrl.groupValues[1].toDouble()
                val lng = matchUrl.groupValues[2].toDouble()
                setFinderTargetCoordinates(lat, lng)
                _finderState.value = _finderState.value.copy(
                    lastPingStatus = "✅ Target coordinates locked: $lat, $lng"
                )
                return
            }

            val coordRegex = Regex("([-+]?[0-9]{1,2}\\.[0-9]+)[,\\s]+([-+]?[0-9]{1,3}\\.[0-9]+)")
            val matchCoord = coordRegex.find(rawText)
            if (matchCoord != null) {
                val lat = matchCoord.groupValues[1].toDouble()
                val lng = matchCoord.groupValues[2].toDouble()
                setFinderTargetCoordinates(lat, lng)
                _finderState.value = _finderState.value.copy(
                    lastPingStatus = "✅ Target coordinates locked: $lat, $lng"
                )
                return
            }

            _finderState.value = _finderState.value.copy(
                lastPingStatus = "⚠️ Could not parse coordinates from input text"
            )
        } catch (e: Exception) {
            _finderState.value = _finderState.value.copy(
                lastPingStatus = "⚠️ Error parsing coordinates"
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepo.clearHistory()
        }
    }
}
