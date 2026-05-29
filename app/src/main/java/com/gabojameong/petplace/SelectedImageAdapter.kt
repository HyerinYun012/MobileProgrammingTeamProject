package com.gabojameong.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

sealed class PostImageItem {
    data class Existing(val url: String) : PostImageItem()
    data class New(val uri: Uri) : PostImageItem()
}

class SelectedImageAdapter(
    private val imageList: MutableList<PostImageItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView   = view.findViewById(R.id.iv_post_image)
        val btnRemove: ImageView = view.findViewById(R.id.btn_remove_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        when (val item = imageList[position]) {
            is PostImageItem.Existing -> Glide.with(holder.itemView.context)
                .load(item.url).placeholder(R.drawable.union).error(R.drawable.union).into(holder.ivImage)
            is PostImageItem.New -> holder.ivImage.setImageURI(item.uri)
        }
        holder.btnRemove.visibility = View.VISIBLE
        holder.btnRemove.setOnClickListener { onRemoveClick(position) }
    }

    override fun getItemCount(): Int = imageList.size
}
