package com.example.weatherapp

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Menyediakan Retrofit untuk:
 *  - Forecast: https://api.open-meteo.com/
 *  - Geocoding: https://geocoding-api.open-meteo.com/
 *
 * Gunakan Retrofit yang terpisah agar endpoint relatif di interface tetap rapi.
 */
object RetrofitModule {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Base URL untuk forecast
    private val forecastRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Base URL untuk geocoding
    private val geocodeRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /** Service untuk forecast */
    val forecastApi: OpenMeteoService by lazy {
        forecastRetrofit.create(OpenMeteoService::class.java)
    }

    /** Service untuk geocoding */
    val geocodeApi: OpenMeteoService by lazy {
        geocodeRetrofit.create(OpenMeteoService::class.java)
    }
}
