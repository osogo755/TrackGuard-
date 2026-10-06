package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SecurityCyan
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FinderModeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val finderState by viewModel.finderState.collectAsState()
    val securityConfig by viewModel.securityConfig.collectAsState()

    var targetPhoneInput by remember { mutableStateOf(finderState.targetPhone.ifEmpty { securityConfig.emergencyPhone }) }
    var targetPinInput by remember { mutableStateOf(finderState.targetPin.ifEmpty { securityConfig.masterPin }) }
    var pasteCoordsInput by remember { mutableStateOf("") }
    var showExplanationGuide by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Finder Radar",
                        tint = SecurityCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Find Lost Phone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Text(
                    text = "Track & recover a lost phone from this second device",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = { showExplanationGuide = !showExplanationGuide },
                modifier = Modifier.testTag("help_guide_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "How it works",
                    tint = SecurityCyan
                )
            }
        }

        // Expandable Explanation Architecture Card
        AnimatedVisibility(visible = showExplanationGuide) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("architecture_explanation_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCyan),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📱 How Finding Another Phone Works:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecurityCyan
                    )

                    HowItWorksStep(
                        step = "1",
                        title = "Send Encrypted SMS Command",
                        description = "From this phone (or ANY phone), send an SMS with '#LOCATE [PIN]' to the lost phone's number."
                    )
                    HowItWorksStep(
                        step = "2",
                        title = "Lost Phone Intercepts Command",
                        description = "Even if silent or locked, TrackGuard on the lost device silently grabs GPS coordinates and battery stats."
                    )
                    HowItWorksStep(
                        step = "3",
                        title = "Automated Coordinates Reply",
                        description = "The lost phone immediately sends an automated SMS reply containing its live Google Maps coordinate link."
                    )
                    HowItWorksStep(
                        step = "4",
                        title = "Proximity Radar & Turn-by-Turn Navigation",
                        description = "Paste or lock onto the coordinates here to view real-time distance, bearing, and launch Google Maps navigation directly to its location."
                    )
                }
            }
        }

        // Target Lost Phone Setup Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("target_setup_card"),
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
                Text(
                    text = "Target Device Credentials",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetPhoneInput,
                        onValueChange = {
                            targetPhoneInput = it
                            viewModel.updateFinderTarget(it, targetPinInput)
                        },
                        label = { Text("Lost Phone Number") },
                        placeholder = { Text("+1 555-0199") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = SecurityCyan)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("target_phone_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecurityCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = targetPinInput,
                        onValueChange = {
                            targetPinInput = it
                            viewModel.updateFinderTarget(targetPhoneInput, it)
                        },
                        label = { Text("Target PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SecurityCyan)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecurityCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Demo preset button for quick testing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            // Preset demo coordinates (e.g. 350 meters away)
                            viewModel.setFinderTargetCoordinates(37.7790, -122.4180, 4f, 76)
                            Toast.makeText(context, "Demo Target coordinates locked", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Simulate Nearby Target", color = SecurityCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        // Remote Commands Action Bar from Finder Phone
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dispatch Command to Lost Phone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Command 1: Locate SMS
                    Button(
                        onClick = {
                            sendSmsIntent(context, targetPhoneInput, "#LOCATE $targetPinInput")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_locate_sms_btn")
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Locate SMS", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Command 2: Blast Siren
                    Button(
                        onClick = {
                            sendSmsIntent(context, targetPhoneInput, "#ALARM $targetPinInput")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_siren_sms_btn")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sound Siren", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Command 3: Remote Lock
                    Button(
                        onClick = {
                            sendSmsIntent(context, targetPhoneInput, "#LOCK $targetPinInput")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_lock_sms_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock Device", color = Color.White, fontSize = 12.sp)
                    }

                    // Command 4: Disarm
                    Button(
                        onClick = {
                            sendSmsIntent(context, targetPhoneInput, "#DISARM $targetPinInput")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dispatch_disarm_sms_btn")
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disarm Siren", color = SafeGreen, fontSize = 12.sp)
                    }
                }
            }
        }

        // Live Proximity Radar & Target Compass
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("proximity_radar_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (finderState.targetLat != null) SecurityCyan else DarkCardBorder
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Proximity Radar & Direction",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (finderState.targetLat != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SafeGreen.copy(alpha = 0.2f))
                                .border(1.dp, SafeGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("TARGET LOCKED", color = SafeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Directional Radar Visualizer
                DirectionalCompassRadar(
                    distanceMeters = finderState.distanceMeters,
                    bearingDegrees = finderState.bearingDegrees,
                    isLocked = finderState.targetLat != null,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics: Distance and Coordinates
                if (finderState.targetLat != null && finderState.targetLng != null) {
                    val distText = if (finderState.distanceMeters != null) {
                        if (finderState.distanceMeters!! < 1000) {
                            "${finderState.distanceMeters!!.toInt()} meters away"
                        } else {
                            String.format(Locale.US, "%.2f km away", finderState.distanceMeters!! / 1000f)
                        }
                    } else {
                        "Target Coordinates Locked"
                    }

                    Text(
                        text = distText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = SecurityCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Lost Device Coords: ${String.format(Locale.US, "%.5f", finderState.targetLat!!)}, ${String.format(Locale.US, "%.5f", finderState.targetLng!!)}",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Turn-by-Turn Navigation Action
                    Button(
                        onClick = {
                            val navUri = Uri.parse("google.navigation:q=${finderState.targetLat},${finderState.targetLng}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, navUri)
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://maps.google.com/?q=${finderState.targetLat},${finderState.targetLng}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_turn_by_turn_nav")
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Turn-by-Turn Navigation", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Awaiting location response from lost device...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // SMS Response Importer & Parser Card
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Import SMS Response or Coordinates",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Paste the SMS text received from the lost phone to decode its exact coordinates:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = pasteCoordsInput,
                    onValueChange = { pasteCoordsInput = it },
                    placeholder = { Text("e.g. https://maps.google.com/?q=37.7749,-122.4194") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_coords_input"),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    pasteCoordsInput = clip
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = SecurityCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecurityCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (pasteCoordsInput.isNotBlank()) {
                                viewModel.parseAndSetTargetFromText(pasteCoordsInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                        modifier = Modifier.testTag("decode_coords_btn")
                    ) {
                        Text("Decode & Track Target", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                finderState.lastPingStatus?.let { status ->
                    Text(
                        text = status,
                        fontSize = 12.sp,
                        color = if (status.startsWith("✅")) SafeGreen else WarningAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Web Tracking Link Generator (For Computer/Laptop/Friend's Browser)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Universal Web Tracking Link",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "If you don't have an Android device nearby, open this link on any computer, tablet, or browser:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                val webPortalUrl = "https://trackguard.live/track?id=${securityConfig.remoteToken}&pin=${securityConfig.masterPin}"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .padding(12.dp)
                ) {
                    Text(
                        text = webPortalUrl,
                        color = SecurityCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Web Tracker", webPortalUrl))
                            Toast.makeText(context, "Web link copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SecurityCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link", color = SecurityCyan)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Track lost device at: $webPortalUrl")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Web Portal Link"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = SecurityCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Link", color = SecurityCyan)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DirectionalCompassRadar(
    distanceMeters: Float?,
    bearingDegrees: Float?,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 12f

        // Radar background circles
        for (r in listOf(radius * 0.35f, radius * 0.7f, radius)) {
            drawCircle(
                color = Color(0xFF1E293B),
                radius = r,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        // Radar Crosshairs
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1f
        )

        // Center dot: "Finder (You)"
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = center
        )

        if (isLocked) {
            val angleRad = Math.toRadians((bearingDegrees ?: 45f).toDouble() - 90.0)
            val targetOffset = Offset(
                (center.x + (radius * 0.75f) * cos(angleRad)).toFloat(),
                (center.y + (radius * 0.75f) * sin(angleRad)).toFloat()
            )

            // Line towards target
            drawLine(
                color = SecurityCyan.copy(alpha = 0.6f),
                start = center,
                end = targetOffset,
                strokeWidth = 2.5f
            )

            // Glowing target dot: "Lost Phone"
            drawCircle(
                color = AlertRed.copy(alpha = 0.35f),
                radius = 16f,
                center = targetOffset
            )
            drawCircle(
                color = AlertRed,
                radius = 8f,
                center = targetOffset
            )
        } else {
            // Sweeping radar beam when scanning
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val endX = center.x + radius * cos(sweepRad).toFloat()
            val endY = center.y + radius * sin(sweepRad).toFloat()

            drawLine(
                color = SecurityCyan.copy(alpha = 0.4f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun HowItWorksStep(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SecurityCyan),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = DarkBackground, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

private fun sendSmsIntent(context: Context, destinationNumber: String, messageText: String) {
    try {
        val smsUri = Uri.parse("smsto:$destinationNumber")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", messageText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open SMS app. Send text: '$messageText' to $destinationNumber", Toast.LENGTH_LONG).show()
    }
}
