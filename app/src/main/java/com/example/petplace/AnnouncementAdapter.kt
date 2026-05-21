package com.example.petplace

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ItemAnnouncementBinding
import java.io.Serializable

data class AnnouncementData(
    val title: String,
    val posterName: String = "사장",
    val posterUrl: String? = null,
    val contents: String = "",
    val postTime: String = "방금 전",
    val fullImageUrl: String? = null,
    val previewUrl: String? = null
) : Serializable

class AnnouncementAdapter(private val announcementList: List<AnnouncementData>) :
    RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    inner class AnnouncementViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: AnnouncementData) {
            binding.tvTitle.text = data.title
            
            Glide.with(binding.ivPoster.context)
                .load(data.posterUrl)
                .placeholder(R.drawable.icon_pfp1)
                .into(binding.ivPoster)

            Glide.with(binding.ivPreview.context)
                .load(data.previewUrl ?: data.fullImageUrl)
                .placeholder(R.drawable.test_announcement_preview)
                .into(binding.ivPreview)

            binding.btnInvisible.setOnClickListener {
                val intent = Intent(it.context, AnnouncementActivity::class.java).apply {
                    putExtra("announcement", data)
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        holder.bind(announcementList[position])
    }

    override fun getItemCount(): Int = announcementList.size
}
