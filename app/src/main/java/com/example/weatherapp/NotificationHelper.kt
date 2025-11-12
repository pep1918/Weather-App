package com.example.weatherapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "weather.detail"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Weather Detail",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifikasi detail cuaca saat pencarian lokasi" }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    fun showDetail(
        ctx: Context,
        locationName: String,
        desc: String,
        tempRange: String,
        iconRes: Int
    ) {
        ensureChannel(ctx)
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("Cuaca $locationName")
            .setContentText("$desc • $tempRange")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$desc • $tempRange"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val nm = NotificationManagerCompat.from(ctx)
        val granted = if (Build.VERSION.SDK_INT >= 33)
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

        if (granted) nm.notify(1001, builder.build())
    }
}
