package com.gabojameong.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // 이미지를 띄우기 위해 Glide 라이브러리 권장

class ImageWriteAdapter(
    private val imageUris: List<Uri>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageWriteAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivSelectedImage: ImageView = itemView.findViewById(R.id.iv_selected_image)
        val btnDeleteImage: ImageView = itemView.findViewById(R.id.btn_delete_image)

        fun bind(uri: Uri, position: Int) {
            Glide.with(itemView.context)
                .load(uri)
                .into(ivSelectedImage)

            btnDeleteImage.setOnClickListener {
                onDeleteClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_image_write, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUris[position], position)
    }

    override fun getItemCount(): Int = imageUris.size
}