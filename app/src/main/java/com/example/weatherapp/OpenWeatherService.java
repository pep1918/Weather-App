package com.example.weatherapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Nama file/kelas dipertahankan: OpenWeatherService
public interface OpenWeatherService {

    // Geocoding: cari koordinat dari nama kota
    // Contoh: /v1/search?name=Surabaya&count=1&language=id&format=json
    @GET("v1/search")
    Call<GeoResponse> searchCity(
            @Query("name") String name,
            @Query("count") int count,
            @Query("language") String language,
            @Query("format") String format
    );

    // Forecast + Current (Open-Meteo)
    // Contoh: /v1/forecast?latitude=-7.25&longitude=112.75&current=temperature_2m,weather_code,relative_humidity_2m,pressure_msl,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=7&timezone=auto
    @GET("v1/forecast")
    Call<OneCallResponse> getWeeklyForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,            // "temperature_2m,weather_code,relative_humidity_2m,pressure_msl,wind_speed_10m"
            @Query("daily") String daily,                // "weather_code,temperature_2m_max,temperature_2m_min"
            @Query("forecast_days") int days,            // 7
            @Query("timezone") String timezone,          // "auto"
            @Query("temperature_unit") String tUnit,     // "celsius"
            @Query("windspeed_unit") String wUnit,       // "kmh"
            @Query("precipitation_unit") String pUnit    // "mm"
    );
}
