package com.example.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.petplace.databinding.ItemReviewPhotoBinding

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
        // 사진 개수가 maxCount 미만이고 마지막 인덱스일 때 추가 버튼 타입을 반환
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
        // 사진이 5개 미만이면 추가 버튼을 포함하기 위해 size + 1 반환
        return if (photoList.size >= maxCount) maxCount else photoList.size + 1
    }

    // 사진 표시 ViewHolder
    inner class PhotoViewHolder(private val binding: ItemReviewPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            binding.ivPhoto.visibility = View.VISIBLE
            binding.btnDeletePhoto.visibility = View.VISIBLE

            Glide.with(itemView.context)
                .load(uri)
                .transform(RoundedCorners(20))
                .into(binding.ivPhoto)

            binding.btnDeletePhoto.setOnClickListener { onDeleteClick(uri) }
        }
    }

    // 추가 버튼 ViewHolder (주원님의 오리지널 ic_add_photo 리소스 매핑)
    inner class AddViewHolder(private val binding: ItemReviewPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.ivPhoto.visibility = View.VISIBLE
            binding.btnDeletePhoto.visibility = View.GONE

            // ic_add_photo 이미지를 추가 버튼으로 설정
            binding.ivPhoto.setImageResource(R.drawable.ic_add_photo)
            binding.ivPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.ivPhoto.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.ivPhoto.setOnClickListener { onAddClick() }
        }
    }
}