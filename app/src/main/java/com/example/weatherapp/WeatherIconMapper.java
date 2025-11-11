package com.example.weatherapp;

public final class WeatherIconMapper {

    public static String describe(Integer code) {
        if (code == null) return "—";
        int c = code;
        if (c == 0) return "Cerah";
        if (c == 1 || c == 2 || c == 3) return "Cerah berawan";
        if (c == 45 || c == 48) return "Berkabut";
        if (c == 51 || c == 53 || c == 55) return "Gerimis";
        if (c == 56 || c == 57) return "Gerimis beku";
        if (c == 61 || c == 63 || c == 65) return "Hujan";
        if (c == 66 || c == 67) return "Hujan beku";
        if (c == 71 || c == 73 || c == 75) return "Salju";
        if (c == 80 || c == 81 || c == 82) return "Hujan deras";
        if (c == 95) return "Badai petir";
        if (c == 96 || c == 99) return "Badai petir (es)";
        return "Cuaca";
    }

    private WeatherIconMapper() {}
}
