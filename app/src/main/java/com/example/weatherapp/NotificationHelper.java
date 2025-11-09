package com.example.weatherapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String CHANNEL_ID = "weather_updates";

    public static void ensureChannel(Context ctx){
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Weather Updates", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Peringatan & ringkasan cuaca");
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static void showWeatherNotification(Context ctx, String title, String text){
        ensureChannel(ctx);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(2001, nb.build());
    }
}
