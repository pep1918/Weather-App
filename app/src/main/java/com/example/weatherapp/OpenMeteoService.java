package com.example.weatherapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoService {

    // Geocoding: cari koordinat dari nama kota
    @GET("v1/search")
    Call<GeoResponse> searchCity(
            @Query("name") String name,
            @Query("count") int count,
            @Query("language") String language,
            @Query("format") String format
    );

    // Forecast + current (tanpa API key)
    @GET("v1/forecast")
    Call<WeatherResponseOM> getForecast7d(
            @Query("latitude") double lat,
            @Query("longitude") double lon,
            @Query("current") String current,          // e.g. temperature_2m,relative_humidity_2m,weather_code,pressure_msl,wind_speed_10m
            @Query("daily") String daily,              // e.g. weather_code,temperature_2m_max,temperature_2m_min
            @Query("timezone") String timezone,        // e.g. "auto"
            @Query("forecast_days") int forecastDays,  // 7
            @Query("wind_speed_unit") String windUnit, // "kmh"
            @Query("temperature_unit") String tempUnit // "celsius"
    );
}
