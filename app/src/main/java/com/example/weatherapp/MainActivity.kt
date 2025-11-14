package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    // ViewModel, repo & adapter cuaca
    private val vm: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val repo = WeatherRepository()
    private val gridAdapter = DailyAdapter()

    // Scope untuk collect Flow
    private val scope = MainScope()

    // State suggestion & notifikasi
    private var lastSuggestions: List<GeoResult> = emptyList()
    private var lastNotifiedQuery: String = ""
    private var pendingAutoNotify: Boolean = false

    // ------------------------------
    //        RIWAYAT PENCARIAN
    // ------------------------------
    private lateinit var prefs: SharedPreferences
    private val searchHistory: MutableList<String> = mutableListOf()

    private val PREF_NAME = "weather_prefs"
    private val KEY_HISTORY = "search_history"
    private val MAX_HISTORY_SIZE = 10
    // ------------------------------

    // Launcher izin notifikasi
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

        // Inisialisasi SharedPreferences & load riwayat
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        loadSearchHistoryFromPrefs()
        renderSearchHistoryChips()

        // RecyclerView 7-day forecast
        binding.recyclerForecast.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = gridAdapter
        }

        // Dropdown autocomplete
        val dropdownAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.editCity.setAdapter(dropdownAdapter)

        // Hint "Search Region" hilang saat user mengetik
        binding.editCity.addTextChangedListener { text ->
            binding.tilCity.hint = if (text.isNullOrEmpty()) "Search Region" else ""
            vm.onQueryChanged(text?.toString().orEmpty())
        }

        // User memilih suggestion dari dropdown
        binding.editCity.setOnItemClickListener { _, _, pos, _ ->
            val geo = lastSuggestions.getOrNull(pos) ?: return@setOnItemClickListener
            vm.selectLocation(geo)

            val selectedText = "${geo.name}, ${geo.country}"
            binding.editCity.setText(selectedText)
            binding.editCity.setSelection(selectedText.length)

            addQueryToHistory(selectedText)

            scope.launch {
                vm.forecast.collectLatest { f ->
                    if (f != null) ensureNotificationPermissionThenNotify()
                }
            }
        }

        // Tombol "Cari" ditekan
        binding.btnSearch.setOnClickListener {
            val q = binding.editCity.text?.toString().orEmpty()
            vm.onQueryChanged(q)
            addQueryToHistory(q)
        }

        // Observasi suggestions (kota)
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

                    // opsional: simpan suggestion pertama sebagai riwayat
                    val firstText = "${list.first().name}, ${list.first().country}"
                    addQueryToHistory(firstText)
                }
            }
        }

        // Observasi forecast cuaca
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

                // Animasi icon
                binding.imgIcon.animate()
                    .rotationBy(360f)
                    .setDuration(600L)
                    .start()

                binding.txtTemp.text =
                    cur?.temperature?.roundToInt()?.let { "$it°C" } ?: "--°C"
                binding.textLocation.text =
                    binding.editCity.text?.toString().orEmpty().ifEmpty { "--" }

                binding.txtDesc.text = when (cur?.weathercode) {
                    0 -> if (night) "Cerah (malam)" else "Cerah"
                    in 1..3 -> if (night) "Cerah berawan (malam)" else "Cerah berawan"
                    in 61..65, in 51..67, in 80..82 -> "Hujan"
                    else -> "Berawan"
                }

                // Background dinamis
                if (cur != null) {
                    updateBackgroundWithAnimation(
                        isNight = night,
                        weatherCode = cur.weathercode
                    )
                }

                val hourly = f?.hourly
                val hum = cur?.time?.let {
                    repo.matchHourlyValue(
                        it,
                        hourly?.time,
                        hourly?.relativehumidity_2m
                    )
                }
                val pres = cur?.time?.let {
                    repo.matchHourlyValue(
                        it,
                        hourly?.time,
                        hourly?.surface_pressure
                    )
                }
                binding.txtHumidity.text = hum?.roundToInt()?.let { "$it%" } ?: "--%"
                binding.txtPressure.text = pres?.roundToInt()?.let { "$it hPa" } ?: "-- hPa"
                binding.txtWind.text =
                    cur?.windspeed?.roundToInt()?.let { "$it km/h" } ?: "-- km/h"

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

    // --------------------------------------------------------------------
    //                    RIWAYAT PENCARIAN
    // --------------------------------------------------------------------

    /** Load riwayat dari SharedPreferences ke list */
    private fun loadSearchHistoryFromPrefs() {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isNotEmpty()) {
            val items = raw.split("|").filter { it.isNotBlank() }
            searchHistory.clear()
            searchHistory.addAll(items)
        }
    }

    /** Simpan riwayat ke SharedPreferences */
    private fun saveSearchHistoryToPrefs() {
        val joined = searchHistory.joinToString("|")
        prefs.edit().putString(KEY_HISTORY, joined).apply()
    }

    /** Tambahkan query ke riwayat + batasi ukuran list */
    private fun addQueryToHistory(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return

        // hapus jika sudah ada (biar tidak dobel)
        searchHistory.remove(q)
        // tambahkan di paling depan (terbaru)
        searchHistory.add(0, q)

        // batasi jumlah maksimum riwayat (pakai removeAt, aman untuk API 24+)
        while (searchHistory.size > MAX_HISTORY_SIZE) {
            searchHistory.removeAt(searchHistory.size - 1)
        }

        saveSearchHistoryToPrefs()
        renderSearchHistoryChips()
    }

    /** Render searchHistory jadi chip di chipGroupHistory */
    private fun renderSearchHistoryChips() {
        val group = binding.chipGroupHistory
        group.removeAllViews()

        searchHistory.forEach { text ->
            val chip = Chip(this).apply {
                this.text = text
                isCheckable = false
                isClickable = true

                setOnClickListener {
                    binding.editCity.setText(text)
                    binding.editCity.setSelection(text.length)
                    vm.onQueryChanged(text)
                }
            }
            group.addView(chip)
        }
    }

    // --------------------------------------------------------------------
    //           BACKGROUND + IZIN & NOTIFIKASI CUACA
    // --------------------------------------------------------------------

    /** Background berubah (siang / malam / hujan) + animasi */
    private fun updateBackgroundWithAnimation(isNight: Boolean, weatherCode: Int) {
        val isRain = when (weatherCode) {
            in 51..67,
            in 80..82,
            in 61..65 -> true
            else -> false
        }

        val bgRes = when {
            isRain -> R.drawable.bg_weather_rain
            isNight -> R.drawable.bg_weather_gradient_night
            else -> R.drawable.bg_weather_gradient
        }

        val root = binding.rootContainer
        root.animate().cancel()
        root.alpha = 0f
        root.setBackgroundResource(bgRes)
        root.animate()
            .alpha(1f)
            .setDuration(600L)
            .start()
    }

    private fun needsPostNotifPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
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

    // Notifikasi cuaca
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
