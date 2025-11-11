package com.example.weatherapp;

import java.util.List;

public class GeoResponse {
    public List<Result> results;

    public static class Result {
        public String name;
        public String country;
        public double latitude;
        public double longitude;
    }
}
