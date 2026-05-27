package com.gabojameong.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gabojameong.petplace.databinding.ItemReviewPhotoBinding

class ReviewPhotoAdapter(
    private val onAddClick: () -> Unit,
    private val onDeleteClick: (Uri) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val photoList = mutableListOf<Uri>()
    private val maxCount = 5

    companion object {
        private const val VIEW_TYPE_PHOTO = 1
        private const val VIEW_TYPE_ADD = 2
    }

    fun setPhotos(uris: List<Uri>) {
        photoList.clear()
        photoList.addAll(uris)
        notifyDataSetChanged()
    }

    fun getPhotos(): List<Uri> = photoList

    override fun getItemViewType(position: Int): Int {
        return if (photoList.size < maxCount && position == photoList.size) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemReviewPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == VIEW_TYPE_ADD) {
            AddViewHolder(binding)
        } else {
            PhotoViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PhotoViewHolder) {
            holder.bind(photoList[position])
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return if (photoList.size >= maxCount) maxCount else photoList.size + 1
    }

    inner class PhotoViewHolder(private val binding: ItemReviewPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.ivPhoto.visibility = android.view.View.VISIBLE
            binding.btnDeletePhoto.visibility = android.view.View.VISIBLE

            Glide.with(itemView.context)
                .load(uri)
                .transform(RoundedCorners(20))
                .into(binding.ivPhoto)

            binding.btnDeletePhoto.setOnClickListener { onDeleteClick(uri) }
        }
    }

    inner class AddViewHolder(private val binding: ItemReviewPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.ivPhoto.visibility = android.view.View.VISIBLE
            binding.btnDeletePhoto.visibility = android.view.View.GONE

            binding.ivPhoto.setImageResource(R.drawable.ic_add_photo)
            binding.ivPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivPhoto.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.ivPhoto.setOnClickListener { onAddClick() }
        }
    }
}
