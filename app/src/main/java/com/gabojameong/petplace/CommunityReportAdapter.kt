package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ItemCommunityReportBinding

class CommunityReportAdapter(
    private val onDeleteClick: (item: CommunityReportRequest) -> Unit,
    private val onCompleteClick: (reportId: Long) -> Unit
) : RecyclerView.Adapter<CommunityReportAdapter.ViewHolder>() {

    private val reportList = mutableListOf<CommunityReportRequest>()

    fun setData(newData: List<CommunityReportRequest>) {
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

        fun bind(item: CommunityReportRequest) {
            // commentId가 null이면 게시글, 있으면 댓글
            val isComment = item.commentId != null
            binding.tvTypeBadge.text = if (isComment) "댓글" else "게시글"

            binding.tvReporter.text = "신고자: ${item.reporterNickname}"
            binding.tvReason.text = "사유: ${item.reason}"
            binding.tvStatus.text = "상태: ${item.status}  |  ${item.createdAt.take(10)}"

            binding.btnDeleteContent.setOnClickListener {
                onDeleteClick(item)
            }
            binding.btnCompleteReport.setOnClickListener {
                onCompleteClick(item.id)
            }
        }
    }
}
