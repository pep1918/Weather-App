package com.example.weatherapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Repository utama untuk data cuaca:
 * - Geocoding (saran lokasi)
 * - Forecast
 * - Cache Room
 * - Util ikon & pencocokan hourly
 */
class WeatherRepository(
    // Pisahkan api untuk forecast & geo agar sesuai baseUrl
    private val forecastApi: OpenMeteoService = RetrofitModule.forecastApi,
    private val geocodeApi: OpenMeteoService = RetrofitModule.geocodeApi,
    private val db: WeatherDatabase = WeatherDatabase.instance
) {
    private val locationDao = db.locationDao()
    private val forecastDao = db.forecastDao()

    /** Saran lokasi: <2 huruf ambil recent dari DB, >=2 huruf ambil dari API geocoding */
    suspend fun suggest(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        if (query.length < 2) {
            locationDao.recent(10).map {
                GeoResult(
                    id = it.id.toInt(),
                    name = it.name,
                    country = it.country,
                    latitude = it.lat,
                    longitude = it.lon
                )
            }
        } else {
            geocodeApi.geocode(query).results.orEmpty()
        }
    }

    /** Ambil forecast dari API */
    suspend fun forecast(lat: Double, lon: Double): ForecastResponse =
        forecastApi.getForecast(lat, lon)

    /** is_day == 0 berarti malam */
    fun isNight(isDayFlag: Int?): Boolean = (isDayFlag == 0)

    /** Mapping icon sederhana by weather code + siang/malam */
    fun iconFor(weathercode: Int, isNight: Boolean): Int {
        fun d() = when (weathercode) {
            0 -> R.drawable.icon02d
            in 1..3 -> R.drawable.icon03d
            in 51..67, in 80..82, in 61..65 -> R.drawable.icon09n
            else -> R.drawable.icon03d
        }
        fun n() = when (weathercode) {
            0 -> R.drawable.icon02n
            in 1..3 -> R.drawable.icon03n
            in 51..67, in 80..82, in 61..65 -> R.drawable.icon09n
            else -> R.drawable.icon03n
        }
        return if (isNight) n() else d()
    }

    /** Cocokkan nilai hourly (mis. humidity/pressure) dengan ISO time current_weather */
    fun matchHourlyValue(isoTime: String, hourlyTime: List<String>?, values: List<Double>?): Double? {
        if (hourlyTime.isNullOrEmpty() || values.isNullOrEmpty()) return null
        val idx = hourlyTime.indexOf(isoTime)
        if (idx >= 0 && idx < values.size) return values[idx]
        // fallback: nearest secara lexicographic compare (cukup aman bila format sama)
        val nearest = hourlyTime.minByOrNull { t -> abs(t.compareTo(isoTime)) } ?: return null
        val i = hourlyTime.indexOf(nearest)
        return values.getOrNull(i)
    }

    /** Simpan lokasi terpilih + cache 7 hari ke DB lokal */
    suspend fun persistSelectionAndCacheForecast(
        chosen: GeoResult,
        forecast: ForecastResponse
    ): LocationEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = locationDao.findByLatLon(chosen.latitude, chosen.longitude)
        val locId = if (existing == null) {
            locationDao.insert(
                LocationEntity(
                    name = chosen.name,
                    country = chosen.country,
                    lat = chosen.latitude,
                    lon = chosen.longitude,
                    lastUsedAt = now
                )
            )
        } else {
            locationDao.touch(existing.id, now)
            existing.id
        }

        val daily = forecast.daily
        if (daily != null && daily.time.isNotEmpty()) {
            forecastDao.deleteByLocation(locId)
            val items = daily.time.indices.map { i ->
                ForecastDayEntity(
                    locationId = locId,
                    dateIso = daily.time[i],
                    weathercode = daily.weathercode[i],
                    tMax = daily.temperature_2m_max[i],
                    tMin = daily.temperature_2m_min[i]
                )
            }.take(7)
            forecastDao.insertAll(items)
        }

        existing ?: LocationEntity(
            id = locId,
            name = chosen.name,
            country = chosen.country,
            lat = chosen.latitude,
            lon = chosen.longitude,
            lastUsedAt = now
        )
    }

    /** Ambil cache forecast lokal 7 hari untuk lat/lon tertentu */
    suspend fun getCachedForecastDaysFor(lat: Double, lon: Double): List<ForecastDayEntity> =
        withContext(Dispatchers.IO) {
            val loc = locationDao.findByLatLon(lat, lon) ?: return@withContext emptyList()
            forecastDao.getByLocation(loc.id, 7)
        }
}
