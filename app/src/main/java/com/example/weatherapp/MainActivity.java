package com.example.weatherapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 101;

    // View
    private TextView textLocation, txtTemp, txtDesc, txtHumidity, txtWind, txtPressure;
    private EditText editCity;
    private Button btnSearch;
    private ImageView imgIcon;
    private RecyclerView recyclerForecast;

    // Adapter 7 hari (asumsikan sudah ada ForecastAdapter + ForecastDay)
    private ForecastAdapter forecastAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecycler();
        setupSearch();

        // Coba load kota terakhir; jika tidak ada -> auto lokasi
        loadLastOrDetectLocation();
    }

    private void bindViews() {
        textLocation   = findViewById(R.id.textLocation);
        txtTemp        = findViewById(R.id.txtTemp);
        txtDesc        = findViewById(R.id.txtDesc);
        txtHumidity    = findViewById(R.id.txtHumidity);
        txtWind        = findViewById(R.id.txtWind);
        txtPressure    = findViewById(R.id.txtPressure);
        imgIcon        = findViewById(R.id.imgIcon);

        editCity       = findViewById(R.id.editCity);
        btnSearch      = findViewById(R.id.btnSearch);
        recyclerForecast = findViewById(R.id.recyclerForecast);
    }

    private void setupRecycler() {
        // Grid 7 kolom (7-day)
        recyclerForecast.setLayoutManager(new GridLayoutManager(this, 7));
        forecastAdapter = new ForecastAdapter(new java.util.ArrayList<>());
        recyclerForecast.setAdapter(forecastAdapter);
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> trySearchCity());
        editCity.setOnEditorActionListener((v, actionId, event) -> {
            boolean ime = actionId == EditorInfo.IME_ACTION_SEARCH;
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (ime || enter) {
                trySearchCity();
                return true;
            }
            return false;
        });
    }

    private void trySearchCity() {
        final String q = editCity.getText() != null ? editCity.getText().toString().trim() : "";
        if (TextUtils.isEmpty(q)) {
            Toast.makeText(this, "Masukkan nama kota", Toast.LENGTH_SHORT).show();
            return;
        }
        // Cari lat/lon via Open-Meteo Geocoding
        WeatherServiceOM.searchCityLatLon(q, new WeatherServiceOM.Result<GeoResponse.Result>() {
            @Override public void ok(GeoResponse.Result res) {
                // Simpan ke Room (opsional — kamu sudah punya City/CityDao/WeatherDatabase)
                new Thread(() -> {
                    try {
                        WeatherDatabase.getInstance(MainActivity.this)
                                .cityDao().save(new City(res.name, res.latitude, res.longitude));
                    } catch (Throwable ignored) {}
                }).start();

                // Load cuaca 7 hari
                loadFromLatLon(res.latitude, res.longitude, res.name);
            }
            @Override public void err(Throwable t) {
                Toast.makeText(MainActivity.this, "Kota tidak ditemukan", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLastOrDetectLocation() {
        // Ambil kota terakhir dari Room
        new Thread(() -> {
            City last = null;
            try {
                last = WeatherDatabase.getInstance(MainActivity.this).cityDao().getLastCity();
            } catch (Throwable ignored) {}

            final City lastCity = last;
            runOnUiThread(() -> {
                if (lastCity != null) {
                    loadFromLatLon(lastCity.lat, lastCity.lon, lastCity.name);
                } else {
                    requestOrFetchLocation();
                }
            });
        }).start();
    }

    private void requestOrFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }
        // Izin sudah OK -> ambil lokasi terakhir
        new LocationHelper(this).lastKnown(new LocationHelper.Callback() {
            @Override public void onLocation(double lat, double lon) {
                loadFromLatLon(lat, lon, "Lokasi Saya");
            }
            @Override public void onError(Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal ambil lokasi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Ambil current + 7 hari dari Open-Meteo dan render ke UI
    private void loadFromLatLon(double lat, double lon, String displayName) {
        WeatherServiceOM.fetch7Days(lat, lon, new WeatherServiceOM.Result<WeatherResponseOM>() {
            @Override public void ok(WeatherResponseOM data) {
                renderCurrent(displayName, data);
                render7Days(data);
            }
            @Override public void err(Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal memuat cuaca", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderCurrent(String name, WeatherResponseOM data) {
        textLocation.setText(name);

        if (data != null && data.current != null) {
            long t = Math.round(data.current.temperature_2m);
            txtTemp.setText(t + "°C");
            txtDesc.setText(WeatherIconMapper.describe(data.current.weather_code));

            int hum = data.current.relative_humidity_2m != null
                    ? data.current.relative_humidity_2m.intValue()
                    : 0;

            int wind = data.current.wind_speed_10m != null
                    ? (int) Math.round(data.current.wind_speed_10m)
                    : 0;

            int pres = data.current.pressure_msl != null
                    ? (int) Math.round(data.current.pressure_msl)
                    : 0;

            txtHumidity.setText(hum + "%");
            txtWind.setText(wind + " km/h");
            txtPressure.setText(pres + " hPa");

            // (opsional) ganti ikon berdasarkan weather_code jika kamu punya drawable mapping sendiri
            // imgIcon.setImageResource( ... );
        }
    }

    private void render7Days(WeatherResponseOM data) {
        if (data == null || data.daily == null || data.daily.time == null) {
            forecastAdapter.submit(java.util.Collections.emptyList());
            return;
        }
        List<ForecastDay> items = WeatherMapper.toForecastDays(data.daily);
        forecastAdapter.submit(items);
    }

    // Callback permission lokasi
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(requestCode, perms, res);
        if (requestCode == REQ_LOCATION) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                requestOrFetchLocation();
            } else {
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
