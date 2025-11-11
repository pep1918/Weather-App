package com.example.weatherapp;

import java.util.List;

public class WeatherResponse {
    public Coord coord;
    public Main main;
    public Wind wind;
    public List<Weather> weather;
    public String name; // nama kota

    public static class Coord { public double lon; public double lat; }
    public static class Main { public float temp; public float pressure; public int humidity; public float temp_min; public float temp_max; }
    public static class Wind { public float speed; }
    public static class Weather { public String main; public String description; public String icon; }
}
