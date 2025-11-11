package com.example.weatherapp;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class WeatherServiceOM {

    public interface Result<T> { void ok(T data); void err(Throwable t); }

    /** Cari kota → ambil lat/lon kota pertama (best match) */
    public static void searchCityLatLon(String city, Result<GeoResponse.Result> cb) {
        RetrofitClientOM.apiGeocoding()
                .searchCity(city, 1, "id", "json")
                .enqueue(new Callback<GeoResponse>() {
                    @Override public void onResponse(@NonNull Call<GeoResponse> call, @NonNull Response<GeoResponse> r) {
                        if (!r.isSuccessful() || r.body() == null || r.body().results == null || r.body().results.isEmpty()) {
                            cb.err(new IllegalStateException("Kota tidak ditemukan"));
                            return;
                        }
                        cb.ok(r.body().results.get(0));
                    }
                    @Override public void onFailure(@NonNull Call<GeoResponse> call, @NonNull Throwable t) { cb.err(t); }
                });
    }

    /** Ambil current + 7 hari */
    public static void fetch7Days(double lat, double lon, Result<WeatherResponseOM> cb) {
        String current = "temperature_2m,relative_humidity_2m,weather_code,pressure_msl,wind_speed_10m";
        String daily   = "weather_code,temperature_2m_max,temperature_2m_min";
        RetrofitClientOM.apiForecast()
                .getForecast7d(lat, lon, current, daily, "auto", 7, "kmh", "celsius")
                .enqueue(new Callback<WeatherResponseOM>() {
                    @Override public void onResponse(@NonNull Call<WeatherResponseOM> call, @NonNull Response<WeatherResponseOM> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            cb.err(new IllegalStateException("Gagal memuat cuaca"));
                            return;
                        }
                        cb.ok(r.body());
                    }
                    @Override public void onFailure(@NonNull Call<WeatherResponseOM> call, @NonNull Throwable t) { cb.err(t); }
                });
    }

    private WeatherServiceOM() {}
}
