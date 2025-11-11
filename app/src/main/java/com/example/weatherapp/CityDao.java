package com.example.weatherapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(City city);

    @Query("SELECT * FROM city ORDER BY id DESC LIMIT 1")
    City getLastCity();
}
