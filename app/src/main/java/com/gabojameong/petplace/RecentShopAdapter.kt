package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class RecentShopAdapter(
    private val shopList: List<RecentViewResponse>,
    private val onItemClick: (RecentViewResponse) -> Unit
) : RecyclerView.Adapter<RecentShopAdapter.RecentShopViewHolder>() {

    inner class RecentShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPlace: ImageView = itemView.findViewById(R.id.ivPlace)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        val btnBookmark: ImageButton = itemView.findViewById(R.id.btnBookmark)

        fun bind(shop: RecentViewResponse) {
            tvLocation.text = "[${shop.category}]"
            tvPlaceName.text = shop.restaurantName

            Glide.with(itemView.context)
                .load(shop.imageUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .placeholder(R.drawable.bg_round_gray)
                .error(R.drawable.bg_round_gray)
                .into(ivPlace)

            itemView.setOnClickListener {
                onItemClick(shop)
            }

            btnBookmark.setOnClickListener {
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return RecentShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentShopViewHolder, position: Int) {
        holder.bind(shopList[position])
    }

    override fun getItemCount(): Int = shopList.size
}