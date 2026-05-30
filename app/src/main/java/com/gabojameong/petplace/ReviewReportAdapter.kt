package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ItemReviewReportBinding

class ReviewReportAdapter(
    private val onDeleteClick: (Long) -> Unit,
    private val onCompleteClick: (Long) -> Unit,
    private val onItemClick: (ReviewReportItem) -> Unit
) : RecyclerView.Adapter<ReviewReportAdapter.ViewHolder>() {

    private val reportList = mutableListOf<ReviewReportItem>()

    fun setData(newData: List<ReviewReportItem>) {
        reportList.clear()
        reportList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int = reportList.size

    inner class ViewHolder(private val binding: ItemReviewReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReviewReportItem) {
            binding.tvReviewContent.text = item.reviewContent
            // 리뷰 작성자 닉네임 표시 (서버에서 writerNickname 추가됨)
            binding.tvReviewerName.text = item.writerNickname ?: item.ownerName
            binding.ratingBar.rating = item.rating.toFloat()

            binding.btnDeleteReview.setOnClickListener {
                onDeleteClick(item.reviewId)
            }
            binding.btnCompleteReview.setOnClickListener {
                onCompleteClick(item.id)
            }
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
