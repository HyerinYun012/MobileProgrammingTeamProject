package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<PostImageAdapter.PostImageViewHolder>() {

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
        
        // 열람 모드에서는 삭제 버튼을 숨김 (레이아웃 기본값이 gone이므로 확실히 하기 위해 한 번 더 설정)
        holder.btnRemove.visibility = View.GONE
        
        Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(R.color.search_background)
            .into(holder.ivImage)
    }

    override fun getItemCount(): Int = imageUrls.size
}
