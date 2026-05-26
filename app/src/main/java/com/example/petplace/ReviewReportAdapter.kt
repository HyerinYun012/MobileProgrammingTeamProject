package com.example.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petplace.databinding.ItemReviewReportBinding

class ReviewReportAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (ReviewReportItem) -> Unit
) : RecyclerView.Adapter<ReviewReportAdapter.ViewHolder>() {

    // 신고 내역 데이터 리스트
    private val reportList = mutableListOf<ReviewReportItem>()

    // 데이터 갱신 메서드
    fun setData(newData: List<ReviewReportItem>) {
        reportList.clear()
        reportList.addAll(newData)
        notifyDataSetChanged() // UI 업데이트 요청
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 아이템 뷰 바인딩 인플레이트
        val binding = ItemReviewReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int = reportList.size

    // ViewHolder 정의
    inner class ViewHolder(private val binding: ItemReviewReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReviewReportItem) {
            // 뷰 객체에 데이터 바인딩
            binding.tvReviewContent.text = item.reviewContent
            binding.tvReviewerName.text = item.ownerName

            // 리뷰 삭제 버튼 클릭 리스너
            binding.btnDeleteReview.setOnClickListener {
                onDeleteClick(item.id)
            }

            // 항목 전체 클릭 리스너 (상세 확인 이동용)
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}