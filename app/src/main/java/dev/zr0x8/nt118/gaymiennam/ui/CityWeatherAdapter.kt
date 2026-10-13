package dev.zr0x8.nt118.gaymiennam.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dev.zr0x8.nt118.gaymiennam.data.CityWeatherUi
import dev.zr0x8.nt118.gaymiennam.databinding.ItemCityBinding
import java.util.Locale

class CityWeatherAdapter(
    private val onDelete: (CityWeatherUi) -> Unit
) : ListAdapter<CityWeatherUi, CityWeatherAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemCityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CityWeatherUi) {
            binding.tvCityName.text = if (item.isCurrentLocation) {
                "📍 ${item.displayName}"
            } else {
                item.displayName
            }
            binding.tvTemp.text = String.format(Locale.US, "%.1f°C", item.tempC)
            binding.tvCondition.text =
                "${item.condition} • Cảm giác như ${String.format(Locale.US, "%.0f", item.feelsLikeC)}°C"
            binding.tvMeta.text =
                "Ẩm ${item.humidity}% • Gió ${String.format(Locale.US, "%.0f", item.windKph)} km/h\n${item.localtime}"
            // Ảnh condition từ WeatherAPI (vd: https://cdn.weatherapi.com/weather/64x64/day/116.png).
            // Dùng icon 128px cho nét hơn trên màn hình lớn.
            val hiRes = item.iconUrl.replace("64x64", "128x128")
            binding.ivCondition.load(hiRes) {
                crossfade(true)
            }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CityWeatherUi>() {
            override fun areItemsTheSame(a: CityWeatherUi, b: CityWeatherUi) =
                a.query.equals(b.query, ignoreCase = true)

            override fun areContentsTheSame(a: CityWeatherUi, b: CityWeatherUi) = a == b
        }
    }
}
