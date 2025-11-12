package com.example.weatherapp

object WeatherIconMapper {

    private fun isNight(localHour: Int) = localHour < 6 || localHour >= 18

    fun iconRes(code: Int, localHour: Int): Int {
        val night = isNight(localHour)
        return when {
            code == 0 -> if (night) R.drawable.icon02n else R.drawable.icon02d
            code in 1..3 || code in 45..48 -> if (night) R.drawable.icon03n else R.drawable.icon03d
            code in 51..67 || code in 80..82 || code in 95..99 -> R.drawable.icon09n
            else -> if (night) R.drawable.icon03n else R.drawable.icon03d
        }
    }

    fun description(code: Int): String = when (code) {
        0 -> "Cerah"
        1 -> "Cerah sebagian"
        2 -> "Berawan sebagian"
        3 -> "Berawan"
        in 45..48 -> "Berkabut"
        in 51..57 -> "Gerimis"
        in 61..67 -> "Hujan"
        in 71..77 -> "Salju"
        in 80..82 -> "Hujan lokal"
        in 95..99 -> "Badai petir"
        else -> "Cuaca tidak diketahui"
    }
}
