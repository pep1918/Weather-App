package com.example.weatherapp;

import java.util.List;

public class WeatherResponseOM {

    public double latitude;
    public double longitude;
    public String timezone;

    public Current current;
    public Daily daily;

    public static class Current {
        public String time;                // ISO
        public double temperature_2m;
        public Integer weather_code;       // bisa null
        public Double relative_humidity_2m;
        public Double wind_speed_10m;
        public Double pressure_msl;
    }

    public static class Daily {
        public List<String> time;                 // yyyy-MM-dd
        public List<Integer> weather_code;
        public List<Double> temperature_2m_max;
        public List<Double> temperature_2m_min;
    }
}
