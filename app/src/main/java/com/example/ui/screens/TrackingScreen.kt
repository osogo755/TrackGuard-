package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationPointEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CanvasMapVisualizer
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SecurityCyan
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LocationFilter {
    ALL,
    SHUTDOWN_ONLY,
    EMERGENCY_ONLY
}

@Composable
fun TrackingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentLocations by viewModel.recentLocations.collectAsState()
    val lastKnown by viewModel.lastKnownLocation.collectAsState()
    val shutdownSnapshots by viewModel.shutdownSnapshots.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var activeFilter by remember { mutableStateOf(LocationFilter.ALL) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val displayedLocations = when (activeFilter) {
        LocationFilter.ALL -> recentLocations
        LocationFilter.SHUTDOWN_ONLY -> recentLocations.filter { it.isShutdownSnapshot }
        LocationFilter.EMERGENCY_ONLY -> recentLocations.filter {
            it.source.contains("Emergency", ignoreCase = true) ||
                    it.source.contains("Lockdown", ignoreCase = true) ||
                    it.source.contains("Unauthorized", ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Location Telemetry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time tracking & offline last-known records",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { viewModel.refreshLocationNow() },
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.testTag("refresh_location_btn")
                ) {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = DarkBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ping GPS", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Hero Card: Last Known Location Before Offline / Powered Off
        item {
            LastKnownLocationCard(
                point = lastKnown,
                context = context
            )
        }

        // Canvas Map & Breadcrumb Visualizer
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visual Radar & Breadcrumb Trail",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${recentLocations.size} points recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecurityCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CanvasMapVisualizer(
                    points = recentLocations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .testTag("canvas_map_radar")
                )
            }
        }

        // Filter chips & Log header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Breadcrumbs Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (recentLocations.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == LocationFilter.ALL,
                    onClick = { activeFilter = LocationFilter.ALL },
                    label = { Text("All (${recentLocations.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecurityCyan,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    )
                )

                FilterChip(
                    selected = activeFilter == LocationFilter.SHUTDOWN_ONLY,
                    onClick = { activeFilter = LocationFilter.SHUTDOWN_ONLY },
                    label = { Text("Shutdown Snapshots (${shutdownSnapshots.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertRed,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    )
                )

                FilterChip(
                    selected = activeFilter == LocationFilter.EMERGENCY_ONLY,
                    onClick = { activeFilter = LocationFilter.EMERGENCY_ONLY },
                    label = { Text("Alerts") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarningAmber,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurface,
                        labelColor = Color.White
                    )
                )
            }
        }

        // List of points
        if (displayedLocations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No location telemetry points recorded yet.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(displayedLocations, key = { it.id }) { point ->
                LocationPointRow(point = point, context = context)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation dialog for clearing database
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Telemetry History?") },
            text = { Text("This will permanently remove all cached GPS points and last-known shutdown records from local storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LastKnownLocationCard(
    point: LocationPointEntity?,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_known_location_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (point?.isShutdownSnapshot == true) AlertRed else SecurityCyan
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (point?.isShutdownSnapshot == true) Icons.Default.PowerOff else Icons.Default.MyLocation,
                        contentDescription = "Last Known Pin",
                        tint = if (point?.isShutdownSnapshot == true) AlertRed else SecurityCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (point?.isShutdownSnapshot == true) "LAST POINT BEFORE SHUTDOWN" else "LAST KNOWN DATA POINT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (point?.isShutdownSnapshot == true) AlertRed else SecurityCyan,
                        letterSpacing = 0.5.sp
                    )
                }

                if (point != null) {
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    Text(
                        text = timeFormat.format(Date(point.timestamp)),
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (point != null) {
                // Coordinates display
                Text(
                    text = "${String.format(Locale.US, "%.5f", point.latitude)}°, ${String.format(Locale.US, "%.5f", point.longitude)}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata row: Battery, Accuracy, Network, Source
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetadataChip(
                        label = "Accuracy",
                        value = "±${point.accuracy.toInt()}m",
                        color = SecurityCyan
                    )
                    MetadataChip(
                        label = "Battery",
                        value = "${point.batteryLevel}%",
                        color = if (point.batteryLevel < 20) AlertRed else SafeGreen
                    )
                    MetadataChip(
                        label = "Status",
                        value = point.networkStatus,
                        color = if (point.networkStatus == "Offline") WarningAmber else SafeGreen
                    )
                }

                if (point.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📝 ${point.note}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Open in Google Maps & Share Coordinates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val mapUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(Stolen Phone)")
                            val intent = Intent(Intent.ACTION_VIEW, mapUri)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://maps.google.com/?q=${point.latitude},${point.longitude}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_in_maps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Maps",
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Maps", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "🚨 TRACKGUARD RECOVERY DATA:\n" +
                                    "Last Known Location: https://maps.google.com/?q=${point.latitude},${point.longitude}\n" +
                                    "Coordinates: ${point.latitude}, ${point.longitude}\n" +
                                    "Accuracy: ±${point.accuracy}m\n" +
                                    "Battery Level: ${point.batteryLevel}%\n" +
                                    "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(point.timestamp))}\n" +
                                    "Source: ${point.source}"

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Recovery Coordinates"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_coords_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = SecurityCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = SecurityCyan)
                    }
                }
            } else {
                Text(
                    text = "No location points recorded yet. Tap 'Ping GPS' above to register current position.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MetadataChip(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LocationPointRow(
    point: LocationPointEntity,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location_point_${point.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (point.isShutdownSnapshot) AlertRed.copy(alpha = 0.5f) else DarkCardBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    point.isShutdownSnapshot -> AlertRed
                                    point.source.contains("Emergency") -> WarningAmber
                                    else -> SecurityCyan
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(point.timestamp)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = point.source,
                        fontSize = 10.sp,
                        color = if (point.isShutdownSnapshot) AlertRed else Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${String.format(Locale.US, "%.5f", point.latitude)}, ${String.format(Locale.US, "%.5f", point.longitude)} (±${point.accuracy.toInt()}m)",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                if (point.note.isNotBlank()) {
                    Text(
                        text = point.note,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Quick Map icon button
            IconButton(
                onClick = {
                    val mapUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://maps.google.com/?q=${point.latitude},${point.longitude}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "View on Map",
                    tint = SecurityCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
