package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(point: LocationPointEntity): Long

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLocations(limit: Int = 100): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    fun getLastKnownLocationFlow(): Flow<LocationPointEntity?>

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastKnownLocationSync(): LocationPointEntity?

    @Query("SELECT * FROM location_points WHERE isShutdownSnapshot = 1 ORDER BY timestamp DESC")
    fun getShutdownSnapshots(): Flow<List<LocationPointEntity>>

    @Query("SELECT COUNT(*) FROM location_points")
    fun getLocationCount(): Flow<Int>

    @Query("DELETE FROM location_points")
    suspend fun clearAll()
}
