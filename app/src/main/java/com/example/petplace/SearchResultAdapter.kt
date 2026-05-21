package com.example.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.petplace.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private var items: List<RestaurantResponse>,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RestaurantResponse) {
            binding.tvPlaceName.text = item.name
            binding.tvLocation.text = item.region

            // 7-7: RestaurantResponse 명세에 맞춰 imageUrl 필드 사용
            val imageUrl = item.imageUrl
            
            Glide.with(binding.ivPlace.context)
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(24)) // 이미지 중앙 자르기 및 모서리 둥글게 (24px)
                .placeholder(R.mipmap.icon) // 로딩 중 이미지
                .error(R.mipmap.icon)       // 에러 시 이미지
                .into(binding.ivPlace)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<RestaurantResponse>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
