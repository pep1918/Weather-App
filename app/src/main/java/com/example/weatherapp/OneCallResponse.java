package com.example.weatherapp;

import java.util.List;

public class OneCallResponse {
    public Current current;
    public java.util.List<Daily> daily;

    public static class Current {
        public double temp;
        public java.util.List<Weather> weather;
    }
    public static class Daily {
        public long dt;
        public Temp temp;
        public java.util.List<Weather> weather;
        public double pop;
    }
    public static class Temp { public double min, max; }
    public static class Weather { public String main, description, icon; }
}
