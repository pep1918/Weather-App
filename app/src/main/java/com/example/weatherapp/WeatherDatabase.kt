package com.example.weatherapp

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, ForecastDayEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun forecastDao(): ForecastDao

    companion object {
        val instance: WeatherDatabase by lazy {
            Room.databaseBuilder(
                WeatherApp.instance,
                WeatherDatabase::class.java,
                "weather.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
