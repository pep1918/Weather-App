package com.example.weatherapp;

public class ForecastDay {
    public final String dayLabel;
    public final int weatherCode;
    public final long tmax;
    public final long tmin;

    public ForecastDay(String dayLabel, int weatherCode, long tmax, long tmin) {
        this.dayLabel = dayLabel;
        this.weatherCode = weatherCode;
        this.tmax = tmax;
        this.tmin = tmin;
    }
}
