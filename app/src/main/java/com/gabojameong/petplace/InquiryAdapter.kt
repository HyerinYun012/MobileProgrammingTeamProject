package com.gabojameong.petplace

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ActivityItemInquiryBinding

class InquiryAdapter(
    private val inquiryList: List<Inquiry>,
    private val onItemClick: (Inquiry) -> Unit
) : RecyclerView.Adapter<InquiryAdapter.InquiryViewHolder>() {

    inner class InquiryViewHolder(private val binding: ActivityItemInquiryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Inquiry) {
            binding.tvInquiryTitle.text = data.title

            if (data.isReplied) {
                binding.tvReplyStatus.text = "답변완료"
                binding.tvReplyStatus.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#FF8243")) // 주황 - 완료
            } else {
                binding.tvReplyStatus.text = "답변대기"
                binding.tvReplyStatus.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#BCBCBC")) // 회색 - 대기
            }

            // 슬롯 클릭 이벤트
            itemView.setOnClickListener {
                onItemClick(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InquiryViewHolder {
        val binding = ActivityItemInquiryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InquiryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InquiryViewHolder, position: Int) {
        holder.bind(inquiryList[position])
    }

    override fun getItemCount(): Int = inquiryList.size
}
