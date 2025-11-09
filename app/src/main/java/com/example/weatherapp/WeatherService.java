package com.example.weatherapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherService {
    @GET("data/2.5/weather")
    Call<WeatherResponse> currentByCoord(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );

    @GET("data/3.0/onecall")
    Call<OneCallResponse> getOneCall(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("exclude") String exclude,  // "minutely,hourly,alerts"
            @Query("units") String units,      // "metric"
            @Query("appid") String apiKey,
            @Query("lang") String lang         // "id"
    );
}
