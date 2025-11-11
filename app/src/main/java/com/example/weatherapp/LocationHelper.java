package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    public interface Callback {
        void onLocation(double lat, double lon);
        void onError(Throwable t);
    }

    private final Context ctx;
    private final FusedLocationProviderClient fused;

    public LocationHelper(@NonNull Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.fused = LocationServices.getFusedLocationProviderClient(this.ctx);
    }

    /** Dapatkan lokasi terakhir; jika null, minta 1x update cepat */
    public void lastKnown(@NonNull Callback cb) {
        if (!hasPermission()) {
            cb.onError(new SecurityException("Location permission not granted"));
            return;
        }
        try {
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    cb.onLocation(loc.getLatitude(), loc.getLongitude());
                } else {
                    requestSingleUpdate(cb);
                }
            }).addOnFailureListener(cb::onError);
        } catch (SecurityException se) {
            cb.onError(se);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestSingleUpdate(@NonNull Callback cb) {
        if (!hasPermission()) {
            cb.onError(new SecurityException("Location permission not granted"));
            return;
        }
        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000L /*2s*/)
                .setMaxUpdates(1)
                .build();

        LocationCallback callback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() != null) {
                    cb.onLocation(result.getLastLocation().getLatitude(),
                            result.getLastLocation().getLongitude());
                } else {
                    cb.onError(new IllegalStateException("No location fix"));
                }
                fused.removeLocationUpdates(this);
            }
        };

        try {
            fused.requestLocationUpdates(req, callback, Looper.getMainLooper());
        } catch (SecurityException se) {
            cb.onError(se);
        }
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
