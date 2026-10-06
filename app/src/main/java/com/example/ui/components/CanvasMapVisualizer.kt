package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationPointEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SecurityCyan
import kotlin.math.abs

@Composable
fun CanvasMapVisualizer(
    points: List<LocationPointEntity>,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting GPS Signal... Tap 'Locate Now'",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4f)
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f + panOffsetX
                val centerY = canvasHeight / 2f + panOffsetY

                // Draw radar concentric circles
                for (radius in listOf(50f, 110f, 180f, 260f)) {
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = radius * zoomScale,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Draw radar crosshairs
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(centerX - 300f * zoomScale, centerY),
                    end = Offset(centerX + 300f * zoomScale, centerY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(centerX, centerY - 300f * zoomScale),
                    end = Offset(centerX, centerY + 300f * zoomScale),
                    strokeWidth = 1f
                )

                // Calculate bounding box of coordinates to map them into canvas space
                val lats = points.map { it.latitude }
                val lngs = points.map { it.longitude }
                val minLat = lats.minOrNull() ?: 0.0
                val maxLat = lats.maxOrNull() ?: 0.0
                val minLng = lngs.minOrNull() ?: 0.0
                val maxLng = lngs.maxOrNull() ?: 0.0

                val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)

                val padding = 60f
                val effectiveW = (canvasWidth - padding * 2) * zoomScale
                val effectiveH = (canvasHeight - padding * 2) * zoomScale

                fun toCanvasOffset(lat: Double, lng: Double): Offset {
                    val normalizedX = ((lng - minLng) / lngSpan).toFloat()
                    val normalizedY = (1f - ((lat - minLat) / latSpan).toFloat()) // Invert Y
                    val x = centerX - (effectiveW / 2) + (normalizedX * effectiveW)
                    val y = centerY - (effectiveH / 2) + (normalizedY * effectiveH)
                    return Offset(x, y)
                }

                // Draw trajectory path between points (chronological order)
                val sortedPoints = points.sortedBy { it.timestamp }
                if (sortedPoints.size > 1) {
                    val path = Path()
                    val firstOffset = toCanvasOffset(sortedPoints.first().latitude, sortedPoints.first().longitude)
                    path.moveTo(firstOffset.x, firstOffset.y)

                    for (i in 1 until sortedPoints.size) {
                        val pt = sortedPoints[i]
                        val offset = toCanvasOffset(pt.latitude, pt.longitude)
                        path.lineTo(offset.x, offset.y)
                    }

                    drawPath(
                        path = path,
                        color = SecurityCyan.copy(alpha = 0.5f),
                        style = Stroke(width = 3f)
                    )
                }

                // Draw markers
                sortedPoints.forEachIndexed { index, pt ->
                    val offset = toCanvasOffset(pt.latitude, pt.longitude)
                    val isLatest = index == sortedPoints.lastIndex
                    val isShutdown = pt.isShutdownSnapshot

                    if (isShutdown) {
                        // Red flashing/prominent marker for shutdown snapshot
                        drawCircle(
                            color = AlertRed.copy(alpha = 0.3f),
                            radius = 16f,
                            center = offset
                        )
                        drawCircle(
                            color = AlertRed,
                            radius = 8f,
                            center = offset
                        )
                    } else if (isLatest) {
                        // Pulsing Cyan marker for current position
                        drawCircle(
                            color = SecurityCyan.copy(alpha = 0.35f),
                            radius = 20f,
                            center = offset
                        )
                        drawCircle(
                            color = SecurityCyan,
                            radius = 9f,
                            center = offset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = offset
                        )
                    } else {
                        // Historical breadcrumb dot
                        drawCircle(
                            color = Color(0xFF64748B),
                            radius = 5f,
                            center = offset
                        )
                    }
                }
            }

            // Legend overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Pinch to Zoom • Drag to Pan",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
