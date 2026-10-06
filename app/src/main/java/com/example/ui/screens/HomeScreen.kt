package com.example.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.TrackGuardAdminReceiver
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SecurityCyan
import com.example.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.securityConfig.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastKnown by viewModel.lastKnownLocation.collectAsState()
    val locationCount by viewModel.locationCount.collectAsState()

    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    val adminComponent = ComponentName(context, TrackGuardAdminReceiver::class.java)
    val isAdminActive = dpm?.isAdminActive(adminComponent) == true

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = SecurityCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TrackGuard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    text = "Anti-Theft & Stealth Recovery Shield",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (config.sirenAlarmActive) AlertRed else SafeGreen.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (config.sirenAlarmActive) AlertRed else SafeGreen,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (config.sirenAlarmActive) Color.White else SafeGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (config.sirenAlarmActive) "SIREN ON" else "PROTECTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.sirenAlarmActive) Color.White else SafeGreen
                    )
                }
            }
        }

        // Active Siren Warning Banner if Siren is currently sounding
        AnimatedVisibility(visible = config.sirenAlarmActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siren_alert_banner"),
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Siren active",
                            tint = AlertRed,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Emergency Siren Playing!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "100% volume overriding silent mode",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.stopAlarm(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        modifier = Modifier.testTag("stop_siren_button")
                    ) {
                        Text("Stop Siren", color = Color.White)
                    }
                }
            }
        }

        // Hero Security Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("security_status_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle glowing background gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SecurityCyan.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing animated shield badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(2.dp, SecurityCyan.copy(alpha = pulseAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Active",
                            tint = SecurityCyan,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Real-Time Tracking & Guard Armed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Device is immune to silent-mode suppression and stealthily logs last known GPS coordinates even if shut down.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Telemetry stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Saved Points", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = "$locationCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(DarkCardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Power Guard", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = if (config.powerOffProtectionEnabled) "ARMED" else "OFF",
                                color = if (config.powerOffProtectionEnabled) SafeGreen else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(DarkCardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Last Battery", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = "${lastKnown?.batteryLevel ?: 100}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Primary Action Grid (4 core capabilities requested by user)
        Text(
            text = "Emergency Anti-Theft Actions",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 1: Remote Siren
            QuickActionTile(
                title = if (config.sirenAlarmActive) "Stop Siren" else "Trigger Siren",
                subtitle = "Bypasses silent mode at 100% volume",
                icon = if (config.sirenAlarmActive) Icons.Default.NotificationsOff else Icons.Default.VolumeUp,
                iconTint = if (config.sirenAlarmActive) AlertRed else WarningAmber,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_siren"),
                onClick = {
                    if (config.sirenAlarmActive) {
                        viewModel.stopAlarm(context)
                    } else {
                        viewModel.triggerAlarm(context)
                    }
                }
            )

            // Action 2: Remote Lockdown
            QuickActionTile(
                title = "Remote Lock",
                subtitle = "Blocks device & shows owner note",
                icon = Icons.Default.Lock,
                iconTint = AlertRed,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_lockdown"),
                onClick = {
                    viewModel.triggerRemoteLock(context)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 3: Test Power-Off Protection
            QuickActionTile(
                title = "Power-Off Guard",
                subtitle = "PIN challenge & fake shutdown",
                icon = Icons.Default.PowerSettingsNew,
                iconTint = SecurityCyan,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_power_guard"),
                onClick = {
                    viewModel.testPowerOffProtection(context)
                }
            )

            // Action 4: Real-Time Locate Now
            QuickActionTile(
                title = if (uiState.isLocating) "Locating..." else "Locate Now",
                subtitle = "Sync fresh GPS coordinates",
                icon = Icons.Default.LocationOn,
                iconTint = SafeGreen,
                isLoading = uiState.isLocating,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_locate_now"),
                onClick = {
                    viewModel.refreshLocationNow()
                }
            )
        }

        // Security Hardening Matrix
        Text(
            text = "Protection Checklist",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item 1: Power Off Protection
                ChecklistRow(
                    title = "Power Off Password Protection",
                    description = "Prevents thief from powering down without entering PIN",
                    isActive = config.powerOffProtectionEnabled,
                    onToggle = { viewModel.setPowerOffProtection(it) }
                )

                // Item 2: Fake Shutdown Mode
                ChecklistRow(
                    title = "Stealth Fake Shutdown Trap",
                    description = "Mimics black dead screen while secretly tracking location",
                    isActive = config.fakeShutdownEnabled,
                    onToggle = { viewModel.setFakeShutdown(it) }
                )

                // Item 3: Background Tracking
                ChecklistRow(
                    title = "Real-Time Telemetry & Offline Cache",
                    description = "Logs coordinates into local Room DB every 30s",
                    isActive = config.stealthTrackingActive,
                    onToggle = { viewModel.setStealthTracking(it) }
                )

                // Item 4: Device Admin Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Device Administrator",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isAdminActive) "Granted - Remote Lock Enabled" else "Not Granted - Tap to Enable",
                            color = if (isAdminActive) SafeGreen else WarningAmber,
                            fontSize = 12.sp
                        )
                    }

                    if (!isAdminActive) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "TrackGuard requires device admin rights to lock device on theft."
                                    )
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("enable_device_admin_btn")
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = SafeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ChecklistRow(
    title: String,
    description: String,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = isActive,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SecurityCyan,
                checkedTrackColor = SecurityCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
