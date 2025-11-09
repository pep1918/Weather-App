package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
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
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tempTextView, cityTextView, descTextView, humidityTextView, windTextView;
    private RecyclerView forecastRecyclerView;
    private ProgressBar loadingBar;
    private ImageView weatherIcon;

    private ForecastAdapter forecastAdapter;
    private final List<ForecastItem> forecastList = new ArrayList<>();

    // === KONFIGURASI ===
    private static final String API_KEY = "YOUR_API_KEY"; // <— Ganti dengan key kamu
    private static final int LOCATION_REQUEST_CODE = 100;
    private static final int NOTIF_REQUEST_CODE = 1012;

    // Lokasi default (Surabaya) bila izin ditolak
    private double latitude = -7.2575;
    private double longitude = 112.7521;

    private FusedLocationProviderClient fusedLocationClient;
    private TtsSpeaker tts = new TtsSpeaker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🌑 Mode gelap/terang otomatis
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 🔧 Init view
        tempTextView = findViewById(R.id.textTemperature);
        cityTextView = findViewById(R.id.textCity);
        descTextView = findViewById(R.id.textDescription);
        humidityTextView = findViewById(R.id.textHumidity);
        windTextView = findViewById(R.id.textWind);
        weatherIcon = findViewById(R.id.imageWeatherIcon);
        forecastRecyclerView = findViewById(R.id.recyclerForecast);
        loadingBar = findViewById(R.id.progressBar);

        forecastRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );
        forecastAdapter = new ForecastAdapter(forecastList);
        forecastRecyclerView.setAdapter(forecastAdapter);

        // 🔔 channel notifikasi + izin (Android 13+)
        NotificationHelper.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_REQUEST_CODE);
        }

        // 🔊 TTS
        tts.init(this, null);

        // 📍 Lokasi
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocationOrRequest();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }

    // =======================
    // Lokasi & Reverse Geocode
    // =======================
    @SuppressLint("MissingPermission")
    private void getLastLocationOrRequest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE
            );
            // Lanjutkan dengan default (tanpa lokasi) untuk pengalaman pertama
            loadDataWithCurrentCoords();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                setCoordsAndLoad(location);
            } else {
                requestSingleFreshLocation();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Gagal mendapatkan lokasi terakhir", Toast.LENGTH_SHORT).show();
            loadDataWithCurrentCoords();
        });
    }

    @SuppressLint("MissingPermission")
    private void requestSingleFreshLocation() {
        var request = new com.google.android.gms.location.LocationRequest.Builder(10_000L)
                .setMinUpdateIntervalMillis(5_000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdates(1) // sekali ambil, lalu berhenti
                .build();

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location last = locationResult.getLastLocation();
                if (last != null) setCoordsAndLoad(last);
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }

    private void setCoordsAndLoad(Location loc) {
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        loadDataWithCurrentCoords();
        scheduleWeatherWorker(latitude, longitude); // jadwalkan notifikasi berkala
    }

    private void loadDataWithCurrentCoords() {
        getCurrentWeather();
        getForecast7Days();
    }

    private String reverseCity(double lat, double lon) {
        try {
            Geocoder g = new Geocoder(this, Locale.getDefault());
            List<Address> list = g.getFromLocation(lat, lon, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                if (a.getLocality() != null) return a.getLocality();
                if (a.getSubAdminArea() != null) return a.getSubAdminArea();
                if (a.getAdminArea() != null) return a.getAdminArea();
            }
        } catch (Exception ignored) {}
        return "Lokasi Anda";
    }

    // =======================
    // Current Weather + TTS + Notif
    // =======================
    private void getCurrentWeather() {
        loadingBar.setVisibility(View.VISIBLE);

        OpenWeatherService service = RetrofitClient.service();
        Call<WeatherResponse> call = service.currentByCoord(latitude, longitude, API_KEY, "metric", "id");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                loadingBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(MainActivity.this, "Gagal memuat data cuaca", Toast.LENGTH_SHORT).show();
                    return;
                }

                WeatherResponse data = response.body();

                String cityName = data.getName();
                if (cityName == null || cityName.trim().isEmpty()) {
                    cityName = reverseCity(latitude, longitude); // pencocokan lokasi
                }
                cityTextView.setText(cityName);
                tempTextView.setText(String.format(Locale.getDefault(),"%.0f°C", data.getMain().getTemp()));
                descTextView.setText(data.getWeather().get(0).getDescription());
                humidityTextView.setText("Kelembapan: " + data.getMain().getHumidity() + "%");
                windTextView.setText("Angin: " + data.getWind().getSpeed() + " m/s");

                String icon = data.getWeather().get(0).getIcon();
                int resId = getResources().getIdentifier("ic_" + icon, "drawable", getPackageName());
                if (resId != 0) weatherIcon.setImageResource(resId);

                // 🔔 Notif ringkasan + 🔊 TTS
                String notifText = "Suhu: " + String.format(Locale.getDefault(),"%.0f", data.getMain().getTemp())
                        + "°C, " + data.getWeather().get(0).getDescription();
                NotificationHelper.showWeatherNotification(MainActivity.this, cityName, notifText);

                tts.speak("Cuaca di " + cityName + ". "
                        + data.getWeather().get(0).getDescription()
                        + ". Suhu " + String.format(Locale.getDefault(),"%.0f", data.getMain().getTemp())
                        + " derajat.");
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                loadingBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Kesalahan koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =======================
    // 7-Day Forecast (One Call)
    // =======================
    private void getForecast7Days() {
        OpenWeatherService service = RetrofitClient.service();
        Call<OneCallResponse> call = service.oneCall(
                latitude, longitude,
                "minutely,hourly,alerts",
                "metric",
                API_KEY,
                "id"
        );

        call.enqueue(new Callback<OneCallResponse>() {
            @Override
            public void onResponse(Call<OneCallResponse> call, Response<OneCallResponse> response) {
                if (!response.isSuccessful() || response.body()==null) {
                    Toast.makeText(MainActivity.this, "Gagal memuat prakiraan", Toast.LENGTH_SHORT).show();
                    return;
                }

                forecastList.clear();
                List<OneCallResponse.Daily> daily = response.body().daily;
                int max = Math.min(7, daily.size());
                SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM", new Locale("id"));

                for (int i = 0; i < max; i++) {
                    OneCallResponse.Daily d = daily.get(i);
                    String day = df.format(d.dt * 1000L);
                    // Map ke ForecastItem milikmu (silakan sesuaikan ctor/field jika beda)
                    ForecastItem item = new ForecastItem(
                            d.dt,
                            day,
                            d.weather.get(0).description,
                            d.temp.max,
                            d.temp.min,
                            d.weather.get(0).icon
                    );
                    forecastList.add(item);
                }
                forecastAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<OneCallResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal memuat prakiraan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =======================
    // Jadwal notifikasi berkala (3 jam)
    // =======================
    private void scheduleWeatherWorker(double lat, double lon) {
        Data input = new Data.Builder()
                .putDouble("lat", lat)
                .putDouble("lon", lon)
                .putString("key", API_KEY)
                .build();

        Constraints cons = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(WeatherWorker.class, java.time.Duration.ofHours(3))
                        .setConstraints(cons)
                        .setInputData(input)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weather_worker",
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    // ===== Izin =====
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            boolean granted = false;
            for (int r : grantResults) if (r == PackageManager.PERMISSION_GRANTED) { granted = true; break; }
            if (granted) {
                getLastLocationOrRequest();
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk cuaca otomatis", Toast.LENGTH_SHORT).show();
                loadDataWithCurrentCoords(); // tetap jalan dengan default
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
