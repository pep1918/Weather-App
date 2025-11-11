package com.example.weatherapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WeatherMapper {

    public static List<ForecastDay> toForecastDays(WeatherResponseOM.Daily d) {
        List<ForecastDay> out = new ArrayList<>();
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outFmt = new SimpleDateFormat("EEE", new Locale("id", "ID"));
        int n = d.time.size();
        for (int i = 0; i < n; i++) {
            String day = d.time.get(i);
            Integer code = safeIdx(d.weather_code, i);
            Double tmax = safeIdx(d.temperature_2m_max, i);
            Double tmin = safeIdx(d.temperature_2m_min, i);
            String label = day;
            try { label = outFmt.format(in.parse(day)); } catch (Exception ignored) {}
            out.add(new ForecastDay(label,
                    code == null ? 0 : code,
                    tmax == null ? 0 : Math.round(tmax),
                    tmin == null ? 0 : Math.round(tmin)));
        }
        return out;
    }

    private static <T> T safeIdx(List<T> l, int i) {
        if (l == null || i < 0 || i >= l.size()) return null;
        return l.get(i);
    }

    private WeatherMapper() {}
}
