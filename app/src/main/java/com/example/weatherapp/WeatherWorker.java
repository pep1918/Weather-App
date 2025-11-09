package com.example.weatherapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Locale;

import retrofit2.Response;

public class WeatherWorker extends Worker {
    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        double lat   = getInputData().getDouble("lat", -6.2);
        double lon   = getInputData().getDouble("lon", 106.8);
        String apiKey= getInputData().getString("key");

        try {
            OpenWeatherService svc = RetrofitClient.getRetrofitInstance().create(OpenWeatherService.class);
            Response<OneCallResponse> resp = svc.getOneCall(
                    lat, lon, "minutely,hourly,alerts", "metric", apiKey, "id"
            ).execute();

            if (!resp.isSuccessful() || resp.body() == null) return Result.retry();

            OneCallResponse.Daily today = resp.body().daily.get(0);
            String title = "Cuaca Hari Ini";
            String text  = String.format(Locale.getDefault(),
                    "Maks %.0f°, Min %.0f° • %s",
                    today.temp.max, today.temp.min, today.weather.get(0).description);

            NotificationHelper.showWeatherNotification(getApplicationContext(), title, text);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
