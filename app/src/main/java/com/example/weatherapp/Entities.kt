package com.example.weatherapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index(value = ["lat","lon"], unique = true)]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val lastUsedAt: Long
)

@Entity(
    tableName = "forecast_days",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId"), Index("dateIso")]
)
data class ForecastDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val locationId: Long,
    val dateIso: String,
    val weathercode: Int,
    val tMax: Double,
    val tMin: Double
)
