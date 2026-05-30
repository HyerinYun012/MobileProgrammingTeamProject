package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ItemCommunityReportBinding

class CommunityReportAdapter(
    private val onDeleteClick: (item: CommunityReportResponse) -> Unit,
    private val onCompleteClick: (reportId: Long) -> Unit,
    private val onItemClick: (postId: Long) -> Unit = {}
) : RecyclerView.Adapter<CommunityReportAdapter.ViewHolder>() {

    private val reportList = mutableListOf<CommunityReportResponse>()

    fun setData(newData: List<CommunityReportResponse>) {
        reportList.clear()
        reportList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommunityReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int = reportList.size

    inner class ViewHolder(private val binding: ItemCommunityReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommunityReportResponse) {
            // commentId가 null이면 게시글, 있으면 댓글
            val isComment = item.commentId != null
            binding.tvTypeBadge.text = if (isComment) "댓글" else "게시글"

            binding.tvReporter.text = "신고자: ${item.reporterNickname}"
            binding.tvReason.text = "사유: ${item.reason}"
            binding.tvStatus.text = "상태: ${item.status}  |  ${item.createdAt.take(10)}"

            binding.btnDeleteContent.setOnClickListener { onDeleteClick(item) }
            binding.btnCompleteReport.setOnClickListener { onCompleteClick(item.id) }

            // 아이템 전체 클릭 → 해당 게시글 상세로 이동 (댓글 신고도 postId 사용)
            binding.root.setOnClickListener {
                item.postId?.let { onItemClick(it) }
            }
        }
    }
}
