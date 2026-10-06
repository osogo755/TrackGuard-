package com.example.ui.shutdown

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TrackGuardApplication
import com.example.service.TrackingService
import com.example.ui.lockdown.KeypadButton
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SecurityCyan
import com.example.ui.theme.TrackGuardTheme
import com.example.util.LocationTrackerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FakeShutdownActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            TrackGuardTheme(darkTheme = true) {
                FakeShutdownContent(
                    onExitVerified = {
                        finish()
                    }
                )
            }
        }
    }
}

enum class ShutdownStage {
    POWER_MENU,
    ENTER_PIN,
    SHUTTING_DOWN_ANIMATION,
    BLACK_SCREEN_STEALTH_MODE
}

@Composable
fun FakeShutdownContent(onExitVerified: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TrackGuardApplication
    val config by app.securityPreferences.securityConfigFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(ShutdownStage.POWER_MENU) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var blackScreenTapCount by remember { mutableIntStateOf(0) }
    var showOwnerRecoveryDialog by remember { mutableStateOf(false) }

    BackHandler {
        // Prevent backing out while in security flow
        if (stage == ShutdownStage.POWER_MENU) {
            onExitVerified()
        }
    }

    when (stage) {
        ShutdownStage.POWER_MENU -> {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("power_menu_guard"),
                color = Color.Black.copy(alpha = 0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Power Guard",
                            tint = SecurityCyan,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Power Off Protection",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TrackGuard is protecting this device. Authentication required before shutdown.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Simulated Power Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Power Off Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    stage = ShutdownStage.ENTER_PIN
                                }
                                .padding(16.dp)
                                .testTag("power_off_action")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Power off",
                                    tint = AlertRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Power Off", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        // Restart Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    stage = ShutdownStage.ENTER_PIN
                                }
                                .padding(16.dp)
                                .testTag("restart_action")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SecurityCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    tint = SecurityCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Restart", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { onExitVerified() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("cancel_power_menu")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.LightGray)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Cancel & Return to Device", color = Color.LightGray)
                    }
                }
            }
        }

        ShutdownStage.ENTER_PIN -> {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("shutdown_pin_challenge"),
                color = DarkBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security PIN",
                                tint = SecurityCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Security Verification",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter Master PIN to authorize device power off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // PIN indicator dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(4) { index ->
                                val isFilled = index < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) SecurityCyan else Color.DarkGray)
                                )
                            }
                        }

                        AnimatedVisibility(visible = errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = AlertRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Keypad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("CANCEL", "0", "DEL")
                        )

                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    KeypadButton(
                                        text = key,
                                        onClick = {
                                            errorMessage = null
                                            when (key) {
                                                "CANCEL" -> {
                                                    stage = ShutdownStage.POWER_MENU
                                                }
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        if (enteredPin.length == 4) {
                                                            val masterPin = config?.masterPin ?: "1234"
                                                            if (enteredPin == masterPin) {
                                                                // Correct PIN! Power off authorized
                                                                onExitVerified()
                                                            } else {
                                                                // Unauthorized thief attempt!
                                                                // Log last-known snapshot immediately and trigger stealth trap!
                                                                coroutineScope.launch(Dispatchers.IO) {
                                                                    val helper = LocationTrackerHelper(context)
                                                                    val loc = helper.getCurrentLocation()
                                                                    if (loc != null) {
                                                                        app.locationRepository.recordLocationPoint(
                                                                            latitude = loc.latitude,
                                                                            longitude = loc.longitude,
                                                                            accuracy = loc.accuracy,
                                                                            source = "Last-Known-Shutdown-Snapshot",
                                                                            isShutdownSnapshot = true,
                                                                            note = "Thief entered wrong power-off PIN ($enteredPin)"
                                                                        )
                                                                    }
                                                                }

                                                                // Trigger fake shutdown trap!
                                                                stage = ShutdownStage.SHUTTING_DOWN_ANIMATION
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        ShutdownStage.SHUTTING_DOWN_ANIMATION -> {
            // Fake Android Shutdown Screen
            LaunchedEffect(Unit) {
                // Keep background tracking active!
                TrackingService.start(context)
                delay(3000L)
                stage = ShutdownStage.BLACK_SCREEN_STEALTH_MODE
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fake_shutdown_anim"),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Powering off...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        ShutdownStage.BLACK_SCREEN_STEALTH_MODE -> {
            // Complete black screen simulation: Phone looks totally powered off!
            // Secret owner rescue: Tap screen 5 times to reveal Owner Unlock dialog!
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        blackScreenTapCount++
                        if (blackScreenTapCount >= 4) {
                            showOwnerRecoveryDialog = true
                            blackScreenTapCount = 0
                        }
                    }
                    .testTag("fake_shutdown_black_screen"),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showOwnerRecoveryDialog) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            color = DarkSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Owner Rescue Mode",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SecurityCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Device is in stealth tracking mode. Enter Master PIN to awaken screen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showOwnerRecoveryDialog = false
                                            stage = ShutdownStage.ENTER_PIN
                                            enteredPin = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Enter PIN", color = DarkBackground)
                                    }

                                    Button(
                                        onClick = { showOwnerRecoveryDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Stay Stealth", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
