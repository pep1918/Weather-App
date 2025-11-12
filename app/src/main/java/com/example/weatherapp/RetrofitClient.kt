package com.example.weatherapp

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    private val http by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    val geo: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(http)
            .build()
            .create(OpenMeteoService::class.java)
    }

    val weather: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(http)
            .build()
            .create(OpenMeteoService::class.java)
    }
}
