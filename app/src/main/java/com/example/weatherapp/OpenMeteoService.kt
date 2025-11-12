package com.example.weatherapp

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {

    // --- GEOCODING (base: https://geocoding-api.open-meteo.com/) ---
    // Contoh: /v1/search?name=Jakarta&count=5&language=en&format=json
    @GET("v1/search")
    suspend fun geocode(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse

    // --- FORECAST (base: https://api.open-meteo.com/) ---
    // Contoh: /v1/forecast?latitude=-6.2&longitude=106.8&current_weather=true&...
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "relativehumidity_2m,surface_pressure",
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse
}
