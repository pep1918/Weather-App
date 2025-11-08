package com.example.weatherapp;

public class ForecastItem {
    private String day;
    private String icon;
    private String tempHighLow;

    public ForecastItem(String day, String icon, String tempHighLow) {
        this.day = day;
        this.icon = icon;
        this.tempHighLow = tempHighLow;
    }

    public String getDay() { return day; }
    public String getIcon() { return icon; }
    public String getTempHighLow() { return tempHighLow; }
}
