package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostImageAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PostImageAdapter.PostImageViewHolder>() {

    class PostImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.iv_post_image)
        val btnRemove: ImageView = view.findViewById(R.id.btn_remove_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_post_image, parent, false)
        return PostImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostImageViewHolder, position: Int) {
        val url = imageUrls[position]
        holder.btnRemove.visibility = View.GONE

        Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(R.color.search_background)
            .error(R.color.search_background)
            .into(holder.ivImage)

        holder.ivImage.setOnClickListener {
            onImageClick?.invoke(url)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
