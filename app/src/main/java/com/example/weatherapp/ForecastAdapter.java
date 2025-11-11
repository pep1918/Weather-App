package com.example.weatherapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.VH> {

    private List<ForecastDay> data;

    public ForecastAdapter(List<ForecastDay> data) {
        this.data = data;
    }

    public void submit(List<ForecastDay> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast_day, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ForecastDay d = data.get(pos);
        h.textDay.setText(d.dayLabel);
        h.imageIcon.setImageResource(WeatherIconMapper.iconFor(d.weatherCode));
        h.textTempMax.setText(d.tmax + "°");
        h.textTempMin.setText(d.tmin + "°");
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView textDay, textTempMax, textTempMin;
        final ImageView imageIcon;
        VH(@NonNull View v) {
            super(v);
            textDay = v.findViewById(R.id.textDay);
            imageIcon = v.findViewById(R.id.imageIcon);
            textTempMax = v.findViewById(R.id.textTempMax);
            textTempMin = v.findViewById(R.id.textTempMin);
        }
    }
}
