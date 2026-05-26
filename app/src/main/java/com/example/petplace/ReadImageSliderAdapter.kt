package com.example.petplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ItemSliderImageBinding

class ReadImageSliderAdapter(private val imageList: List<String>) :
    RecyclerView.Adapter<ReadImageSliderAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSliderImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUrl: String) {
            // 인터넷 주소(또는 로컬 주소)를 받아서 이미지뷰에 꽉 차게 꽂아줌
            Glide.with(itemView.context)
                .load(Uri.parse(imageUrl))
                .into(binding.ivSliderImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 아까 새로 만든 item_slider_image.xml을 뷰바인딩으로 연결!
        val binding = ItemSliderImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size
}