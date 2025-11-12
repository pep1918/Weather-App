package com.example.weatherapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WeatherApp : Application() {

    companion object {
        lateinit var instance: WeatherApp
            private set

        const val CHANNEL_ID_WEATHER = "weather_detail"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Detail"
            val desc = "Notifikasi detail cuaca terkini"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_WEATHER, name, importance).apply {
                description = desc
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
