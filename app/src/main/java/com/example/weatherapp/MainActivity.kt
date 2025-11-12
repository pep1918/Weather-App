package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.example.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val repo = WeatherRepository()
    private val gridAdapter = DailyAdapter()

    private val scope = MainScope()

    private var lastSuggestions: List<GeoResult> = emptyList()
    private var lastNotifiedQuery: String = ""
    private var pendingAutoNotify: Boolean = false

    // launcher izin notifikasi
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingAutoNotify) {
            showWeatherNotificationSafely()
            pendingAutoNotify = false
        } else if (!granted) {
            if (shouldShowPostNotifRationale()) showPostNotifRationale()
            else showGoToSettingsDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView untuk 7-day forecast
        binding.recyclerForecast.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = gridAdapter
        }

        // Dropdown autocomplete
        val dropdownAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.editCity.setAdapter(dropdownAdapter)

        // Input listener: update query dan kirim notifikasi otomatis
        binding.editCity.addTextChangedListener {
            vm.onQueryChanged(it?.toString().orEmpty())
        }

        // Klik suggestion manual
        binding.editCity.setOnItemClickListener { _, _, pos, _ ->
            val geo = lastSuggestions.getOrNull(pos) ?: return@setOnItemClickListener
            vm.selectLocation(geo)
            scope.launch {
                vm.forecast.collectLatest { f ->
                    if (f != null) ensureNotificationPermissionThenNotify()
                }
            }
        }

        // Tombol Cari
        binding.btnSearch.setOnClickListener {
            vm.onQueryChanged(binding.editCity.text?.toString().orEmpty())
        }

        // Observasi suggestions → tampilkan dropdown + auto-notify
        scope.launch {
            vm.suggestions.collectLatest { list ->
                lastSuggestions = list
                dropdownAdapter.clear()
                dropdownAdapter.addAll(list.map { "${it.name}, ${it.country}" })
                dropdownAdapter.notifyDataSetChanged()
                if (list.isNotEmpty()) binding.editCity.showDropDown()

                val q = binding.editCity.text?.toString().orEmpty()
                if (q.length >= 3 && list.isNotEmpty() && q != lastNotifiedQuery) {
                    lastNotifiedQuery = q
                    pendingAutoNotify = true
                    vm.selectLocation(list.first())
                }
            }
        }

        // Observasi forecast → update UI dan kirim notifikasi auto
        scope.launch {
            vm.forecast.collectLatest { f ->
                val night = repo.isNight(f?.current_weather?.is_day)
                AppCompatDelegate.setDefaultNightMode(
                    if (night) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )

                val cur = f?.current_weather
                val icon = repo.iconFor(cur?.weathercode ?: 3, night)
                binding.imgIcon.setImageResource(icon)
                binding.txtTemp.text = cur?.temperature?.roundToInt()?.let { "$it°C" } ?: "--°C"
                binding.textLocation.text =
                    binding.editCity.text?.toString().orEmpty().ifEmpty { "--" }
                binding.txtDesc.text = when (cur?.weathercode) {
                    0 -> if (night) "Cerah (malam)" else "Cerah"
                    in 1..3 -> if (night) "Cerah berawan (malam)" else "Cerah berawan"
                    in 61..65, in 51..67, in 80..82 -> "Hujan"
                    else -> "Berawan"
                }

                val hourly = f?.hourly
                val hum = cur?.time?.let { repo.matchHourlyValue(it, hourly?.time, hourly?.relativehumidity_2m) }
                val pres = cur?.time?.let { repo.matchHourlyValue(it, hourly?.time, hourly?.surface_pressure) }
                binding.txtHumidity.text = hum?.roundToInt()?.let { "$it%" } ?: "--%"
                binding.txtPressure.text = pres?.roundToInt()?.let { "$it hPa" } ?: "-- hPa"
                binding.txtWind.text = cur?.windspeed?.roundToInt()?.let { "$it km/h" } ?: "-- km/h"

                val d = f?.daily
                if (d != null && d.time.isNotEmpty()) {
                    val items = d.time.indices.map { i ->
                        GridDay(
                            dateIso = d.time[i],
                            iconRes = repo.iconFor(d.weathercode[i], night),
                            tMax = d.temperature_2m_max[i],
                            tMin = d.temperature_2m_min[i]
                        )
                    }.take(7)
                    gridAdapter.submit(items)
                } else gridAdapter.submit(emptyList())

                if (pendingAutoNotify && f != null) {
                    ensureNotificationPermissionThenNotify()
                    pendingAutoNotify = false
                }
            }
        }
    }

    // --- izin notifikasi ---
    private fun needsPostNotifPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED

    private fun shouldShowPostNotifRationale(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    private fun showPostNotifRationale() {
        AlertDialog.Builder(this)
            .setTitle("Izin Notifikasi")
            .setMessage("Aplikasi butuh izin untuk menampilkan notifikasi cuaca terbaru.")
            .setPositiveButton("Izinkan") { d, _ ->
                d.dismiss()
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Nanti") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Aktifkan Notifikasi")
            .setMessage("Notifikasi dimatikan. Aktifkan melalui pengaturan aplikasi.")
            .setPositiveButton("Buka Pengaturan") { d, _ ->
                d.dismiss()
                openAppNotificationSettings()
            }
            .setNegativeButton("Batal") { d, _ -> d.dismiss() }
            .show()
    }

    private fun openAppNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }
    }

    // --- logic aman sebelum notify ---
    private fun ensureNotificationPermissionThenNotify() {
        if (Build.VERSION.SDK_INT < 33) {
            showWeatherNotificationSafely()
            return
        }

        if (needsPostNotifPermission()) {
            pendingAutoNotify = true
            if (shouldShowPostNotifRationale()) showPostNotifRationale()
            else requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            showGoToSettingsDialog()
            return
        }

        showWeatherNotificationSafely()
    }

    private fun showWeatherNotificationSafely() {
        try {
            showWeatherNotification()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Izin notifikasi ditolak.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Notifikasi cuaca utama ---
    @SuppressLint("MissingPermission")
    private fun showWeatherNotification() {
        val f = vm.forecast.value ?: return
        val cur = f.current_weather ?: return
        val night = cur.is_day == 0
        val icon = repo.iconFor(cur.weathercode, night)
        val text =
            "Suhu ${cur.temperature.roundToInt()}°C • Angin ${cur.windspeed.roundToInt()} km/j • Kode ${cur.weathercode}"

        val notif = NotificationCompat.Builder(this, "weather_detail")
            .setSmallIcon(icon)
            .setContentTitle("Cuaca saat ini")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1001, notif)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
