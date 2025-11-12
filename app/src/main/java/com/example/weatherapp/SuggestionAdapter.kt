package com.example.weatherapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.ItemSuggestionBinding

class SuggestionAdapter(
    private val onClick: (GeoResult) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.VH>() {

    private val items = mutableListOf<GeoResult>()

    fun submit(newItems: List<GeoResult>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(val b: ItemSuggestionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position] // ← jangan pakai nama 'it' biar tak ketimpa
        holder.b.tvName.text = listOfNotNull(item.name, item.country).joinToString(", ")
        holder.b.root.setOnClickListener { _ ->
            onClick(item) // ← kirim GeoResult, bukan View
        }
    }
}
