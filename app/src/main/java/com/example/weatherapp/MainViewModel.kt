package com.example.weatherapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repo: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val suggestions: StateFlow<List<GeoResult>> =
        _query.debounce(300)
            .mapLatest { q -> repo.suggest(q) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _forecast = MutableStateFlow<ForecastResponse?>(null)
    val forecast: StateFlow<ForecastResponse?> = _forecast.asStateFlow()

    fun onQueryChanged(new: String) { _query.value = new }

    fun selectLocation(geo: GeoResult) {
        viewModelScope.launch {
            val fc = repo.forecast(geo.latitude, geo.longitude)
            _forecast.value = fc
            repo.persistSelectionAndCacheForecast(geo, fc)
        }
    }
}
