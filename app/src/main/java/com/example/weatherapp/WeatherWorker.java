package com.example.weatherapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import retrofit2.Response;

public class WeatherWorker extends Worker {
    public static final String API_KEY = "856110a69c5f750b2b04f8d1b524b056"; // TODO: ganti

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override
    public Result doWork() {
        try {
            City last = WeatherDatabase.getInstance(getApplicationContext()).cityDao().getLastCity();
            if (last == null) return Result.success();

            Response<OneCallResponse> resp = RetrofitClient.api()
                    .getWeeklyForecast(last.lat, last.lon, "minutely,hourly,alerts", "metric", API_KEY)
                    .execute();

            if (resp.isSuccessful() && resp.body() != null && resp.body().daily != null && !resp.body().daily.isEmpty()) {
                OneCallResponse.Daily today = resp.body().daily.get(0);
                String cond = (today.weather != null && !today.weather.isEmpty()) ? today.weather.get(0).description : "–";
                String text = last.name + " • " + Math.round(today.temp.max) + "° / " + Math.round(today.temp.min) + "° • " + cond;
                NotificationHelper.notifyWeather(getApplicationContext(), "Cuaca Hari Ini", text);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
