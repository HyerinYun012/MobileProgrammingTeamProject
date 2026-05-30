package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gabojameong.petplace.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private var items: List<RestaurantResponse>,
    private val onFavoriteClick: (RestaurantResponse, (Boolean) -> Unit) -> Unit,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RestaurantResponse) {
            binding.tvPlaceName.text = item.name
            binding.tvLocation.text = item.region

            updateFavoriteUI(item.bookmarked)

            // 서버가 imageUrls(복수) 또는 imageUrl(단수) 중 하나로 응답 → 첫 번째 이미지 사용
            val imageUrl = item.imageUrls?.firstOrNull() ?: item.imageUrl

            Glide.with(binding.ivPlace.context)
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(24))
                .placeholder(R.mipmap.icon)
                .error(R.mipmap.icon)
                .into(binding.ivPlace)

            binding.btnBookmark.setOnClickListener {
                onFavoriteClick(item) { isNowBookmarked ->
                    // 리스트의 데이터 상태도 업데이트
                    updateBookmarkStatus(item.id, isNowBookmarked)
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun updateFavoriteUI(isBookmarked: Boolean) {
            binding.btnBookmark.setBackgroundResource(
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

    fun updateBookmarkStatus(restaurantId: Long, isBookmarked: Boolean) {
        val index = items.indexOfFirst { it.id == restaurantId }
        if (index != -1) {
            if (items[index].bookmarked != isBookmarked) {
                val updatedItem = items[index].copy(bookmarked = isBookmarked)
                val newList = items.toMutableList()
                newList[index] = updatedItem
                items = newList
                notifyItemChanged(index)
            }
        }
    }
}
