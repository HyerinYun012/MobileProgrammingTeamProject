package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class FavoriteUiModel(
    val restaurantId: Long,
    val restaurantName: String,
    val address: String,
    val imageUrl: String?,
    var isBookmarked: Boolean = true
)

class FavoriteShopAdapter(
    private val shopList: MutableList<FavoriteUiModel>,
    private val onBookmarkClick: (Long, Boolean) -> Unit
) : RecyclerView.Adapter<FavoriteShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPlace: ImageView = itemView.findViewById(R.id.ivPlace)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        val btnBookmark: ImageButton = itemView.findViewById(R.id.btnBookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = shopList[position]

        holder.tvLocation.text = item.address
        holder.tvPlaceName.text = item.restaurantName

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.bg_round_gray)
            .into(holder.ivPlace)

        if (item.isBookmarked) {
            holder.btnBookmark.setBackgroundResource(R.drawable.icon_heart)
        } else {
            holder.btnBookmark.setBackgroundResource(R.drawable.icon_heart_empty)
        }

        holder.btnBookmark.setOnClickListener {
            item.isBookmarked = !item.isBookmarked
            notifyItemChanged(position)

            onBookmarkClick(item.restaurantId, item.isBookmarked)
        }
    }

    override fun getItemCount(): Int {
        return shopList.size
    }
}