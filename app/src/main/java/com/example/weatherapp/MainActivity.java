package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherapp.Entity.ForecastItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tempTextView, cityTextView, descTextView, humidityTextView, windTextView;
    private RecyclerView forecastRecyclerView;
    private ProgressBar loadingBar;
    private ImageView weatherIcon;

    private ForecastAdapter forecastAdapter;
    private List<ForecastItem> forecastList = new ArrayList<>();

    private static final String API_KEY = ""; // 🔑 Ganti dengan API key kamu
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;

    private double latitude = -7.2575; // Default Surabaya
    private double longitude = 112.7521;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🌑 Aktifkan dark/light mode otomatis
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 🔔 Notifikasi cuaca harian
        NotificationHelper.scheduleDailyNotification(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        tempTextView = findViewById(R.id.textTemperature);
        cityTextView = findViewById(R.id.textCity);
        descTextView = findViewById(R.id.textDescription);
        humidityTextView = findViewById(R.id.textHumidity);
        windTextView = findViewById(R.id.textWind);
        weatherIcon = findViewById(R.id.imageWeatherIcon);
        forecastRecyclerView = findViewById(R.id.recyclerForecast);
        loadingBar = findViewById(R.id.progressBar);

        forecastRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        forecastAdapter = new ForecastAdapter(forecastList);
        forecastRecyclerView.setAdapter(forecastAdapter);
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                getCurrentWeather();
                getForecast();
            } else {
                requestNewLocationData();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    latitude = lastLocation.getLatitude();
                    longitude = lastLocation.getLongitude();
                    getCurrentWeather();
                    getForecast();
                }
            }
        }, Looper.getMainLooper());
    }

    private void getCurrentWeather() {
        loadingBar.setVisibility(View.VISIBLE);

        WeatherService weatherService = RetrofitClient.getRetrofitInstance().create(WeatherService.class);
        Call<WeatherResponse> call = weatherService.getCurrentWeatherByCoord(latitude, longitude, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                loadingBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();

                    cityTextView.setText(data.getName());
                    tempTextView.setText(String.format("%.0f°C", data.getMain().getTemp()));
                    descTextView.setText(data.getWeather().get(0).getDescription());
                    humidityTextView.setText("Humidity: " + data.getMain().getHumidity() + "%");
                    windTextView.setText("Wind: " + data.getWind().getSpeed() + " m/s");

                    String icon = data.getWeather().get(0).getIcon();
                    int resId = getResources().getIdentifier("ic_" + icon, "drawable", getPackageName());
                    if (resId != 0) weatherIcon.setImageResource(resId);

                    // 🔔 Kirim notifikasi kondisi cuaca
                    NotificationHelper.showWeatherNotification(MainActivity.this,
                            data.getName(),
                            "Suhu: " + data.getMain().getTemp() + "°C, " + data.getWeather().get(0).getDescription());
                } else {
                    Toast.makeText(MainActivity.this, "Gagal memuat data cuaca", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                loadingBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Kesalahan koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getForecast() {
        WeatherService weatherService = RetrofitClient.getRetrofitInstance().create(WeatherService.class);
        Call<WeatherResponse> call = weatherService.getForecastByCoord(latitude, longitude, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    forecastList.clear();
                    forecastList.addAll(response.body().getForecastItems());
                    forecastAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal memuat prakiraan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔐 Izin lokasi
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk cuaca otomatis", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
