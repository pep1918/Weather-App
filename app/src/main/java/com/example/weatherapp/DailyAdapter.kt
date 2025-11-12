package com.example.weatherapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.ItemDayBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class GridDay(
    val dateIso: String,
    val iconRes: Int,
    val tMax: Double,
    val tMin: Double
)

class DailyAdapter : RecyclerView.Adapter<DailyAdapter.VH>() {

    private val items = mutableListOf<GridDay>()

    fun submit(newItems: List<GridDay>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(val b: ItemDayBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvDay.text = formatDay(item.dateIso)
        holder.b.ivIcon.setImageResource(item.iconRes)
        holder.b.tvTMax.text = "${item.tMax.roundToInt()}°"
        holder.b.tvTMin.text = "${item.tMin.roundToInt()}°"
    }

    private fun formatDay(iso: String): String {
        return try {
            val date = LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE)
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        } catch (e: Exception) {
            "--"
        }
    }
}
