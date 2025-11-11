package com.example.weatherapp;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClientOM {

    private static Retrofit meteo;
    private static Retrofit geocoding;

    // Open-Meteo Forecast
    public static Retrofit meteo() {
        if (meteo == null) {
            meteo = new Retrofit.Builder()
                    .baseUrl("https://api.open-meteo.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return meteo;
    }

    // Open-Meteo Geocoding
    public static Retrofit geocoding() {
        if (geocoding == null) {
            geocoding = new Retrofit.Builder()
                    .baseUrl("https://geocoding-api.open-meteo.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geocoding;
    }

    public static OpenMeteoService apiForecast() {
        return meteo().create(OpenMeteoService.class);
    }

    public static OpenMeteoService apiGeocoding() {
        return geocoding().create(OpenMeteoService.class);
    }

    private RetrofitClientOM() {}
}
