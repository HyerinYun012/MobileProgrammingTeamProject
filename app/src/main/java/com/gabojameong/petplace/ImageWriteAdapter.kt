package com.gabojameong.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageWriteAdapter(
    private val imageUris: List<Uri>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageWriteAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivSelectedImage: ImageView = itemView.findViewById(R.id.iv_selected_image)
        val btnDeleteImage: ImageView  = itemView.findViewById(R.id.btn_delete_image)

        fun bind(uri: Uri) {
            Glide.with(itemView.context).load(uri).into(ivSelectedImage)
            btnDeleteImage.setOnClickListener { onDeleteClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.activity_item_image_write, parent, false))

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) = holder.bind(imageUris[position])
    override fun getItemCount(): Int = imageUris.size
}
