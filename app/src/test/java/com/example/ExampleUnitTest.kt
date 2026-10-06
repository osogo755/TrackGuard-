package com.example

import com.example.data.model.LocationPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLocationPointEntityCreation() {
        val point = LocationPointEntity(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 4.5f,
            altitude = 15.0,
            speed = 1.2f,
            batteryLevel = 85,
            isCharging = false,
            networkStatus = "Cellular",
            source = "Last-Known-Shutdown-Snapshot",
            isShutdownSnapshot = true,
            note = "Device shutdown point"
        )

        assertEquals(37.7749, point.latitude, 0.0001)
        assertEquals(-122.4194, point.longitude, 0.0001)
        assertTrue(point.isShutdownSnapshot)
        assertEquals("Last-Known-Shutdown-Snapshot", point.source)
    }

    @Test
    fun testRemoteCommandParsing() {
        val command = "#ALARM 1234"
        assertTrue(command.uppercase().contains("ALARM"))
    }

    @Test
    fun testCoordinateExtractionRegex() {
        val rawSms = "🚨 TrackGuard: Lost phone located! https://maps.google.com/?q=37.7749,-122.4194 (Accuracy: ±5m)"
        val urlRegex = Regex("q=([-+]?[0-9]*\\.?[0-9]+),([-+]?[0-9]*\\.?[0-9]+)")
        val match = urlRegex.find(rawSms)
        assertTrue(match != null)
        assertEquals("37.7749", match?.groupValues?.get(1))
        assertEquals("-122.4194", match?.groupValues?.get(2))
    }
}
