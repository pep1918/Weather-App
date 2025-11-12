package com.example.weatherapp

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current_weather: CurrentWeather?,
    val daily: Daily?,
    val hourly: Hourly? // untuk humidity & pressure
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String,
    val is_day: Int // 1=siang, 0=malam
)

data class Daily(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val sunrise: List<String>?,
    val sunset: List<String>?
)

data class Hourly(
    val time: List<String>?,
    val relativehumidity_2m: List<Double>?,
    val surface_pressure: List<Double>?
)

data class GeocodingResponse(val results: List<GeoResult>?)
data class GeoResult(
    val id: Int?,
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)
