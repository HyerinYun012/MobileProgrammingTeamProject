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
    private val onFavoriteClick: (RestaurantResponse, (Boolean) -> Unit) -> Unit,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RestaurantResponse) {
            binding.tvPlaceName.text = item.name
            binding.tvLocation.text = item.region

            updateFavoriteUI(item.isBookmarked)

            // Glide를 사용하여 이미지 로드 (라운딩 처리 포함)
            val imageUrl = item.imageUrl
            
            Glide.with(binding.ivPlace.context)
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(24))
                .placeholder(R.mipmap.icon)
                .error(R.mipmap.icon)
                .into(binding.ivPlace)

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(item) { isNowBookmarked ->
                    // 리스트의 데이터 상태도 업데이트 (필요 시)
                    item.copy(isBookmarked = isNowBookmarked) // 실제 원본 리스트의 객체 상태를 바꾸고 싶다면 아래처럼 처리
                    updateFavoriteUI(isNowBookmarked)
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun updateFavoriteUI(isBookmarked: Boolean) {
            binding.btnFavorite.setBackgroundResource(
                if (isBookmarked) R.drawable.icon_heart else R.drawable.icon_heart_empty
            )
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
