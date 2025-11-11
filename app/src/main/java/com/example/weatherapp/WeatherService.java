package com.example.weatherapp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface WeatherService {

    /** Pakai @Url full agar bisa panggil geocoding & forecast tanpa 2 Retrofit */
    @GET
    Call<GeoResponse> geocode(@Url String fullUrl);

    @GET
    Call<WeatherResponseOM> forecast(@Url String fullUrl);
}
