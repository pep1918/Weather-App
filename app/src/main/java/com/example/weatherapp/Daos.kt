package com.example.weatherapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationEntity): Long

    @Query("SELECT * FROM locations WHERE lat = :lat AND lon = :lon LIMIT 1")
    suspend fun findByLatLon(lat: Double, lon: Double): LocationEntity?

    @Query("UPDATE locations SET lastUsedAt = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long)

    @Query("SELECT * FROM locations ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 10): List<LocationEntity>
}

@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ForecastDayEntity>)

    @Query("DELETE FROM forecast_days WHERE locationId = :locationId")
    suspend fun deleteByLocation(locationId: Long)

    @Query("""
        SELECT * FROM forecast_days 
        WHERE locationId = :locationId 
        ORDER BY dateIso ASC 
        LIMIT :limit
    """)
    suspend fun getByLocation(locationId: Long, limit: Int = 7): List<ForecastDayEntity>
}
