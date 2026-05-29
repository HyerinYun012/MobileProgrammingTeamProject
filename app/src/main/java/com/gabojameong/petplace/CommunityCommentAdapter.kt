package com.gabojameong.petplace

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityCommentAdapter(
    private val context: Context,
    private val comments: MutableList<CommentResponse>,
    private val postAuthorId: Long,
    private val onCommentChanged: () -> Unit
) : RecyclerView.Adapter<CommunityCommentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView  = itemView.findViewById(R.id.iv_author_profile)
        val tvAuthor: TextView    = itemView.findViewById(R.id.tv_comment_author)
        val tvOwnerBadge: TextView    = itemView.findViewById(R.id.tv_owner_badge)
        val tvBadgeCustomer: TextView = itemView.findViewById(R.id.tv_badge_customer)
        val tvPostAuthorBadge: TextView = itemView.findViewById(R.id.tv_post_author_badge)
        val tvDate: TextView      = itemView.findViewById(R.id.tv_comment_date)
        val tvContent: TextView   = itemView.findViewById(R.id.tv_comment_content)
        val btnEdit: TextView     = itemView.findViewById(R.id.btn_edit_comment)
        val btnDelete: TextView   = itemView.findViewById(R.id.btn_delete_comment)
        val layoutEditMode: View  = itemView.findViewById(R.id.layout_edit_comment_mode)
        val etEditComment: EditText   = itemView.findViewById(R.id.et_edit_comment)
        val btnCancelEdit: TextView   = itemView.findViewById(R.id.btn_cancel_edit)
        val btnSaveEdit: TextView     = itemView.findViewById(R.id.btn_save_edit)
        val btnReport: TextView   = itemView.findViewById(R.id.btn_report_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_community_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]

        holder.tvAuthor.text  = comment.nickname
        holder.tvDate.text    = formatDate(comment.createdAt)
        holder.tvContent.text = comment.content

        Glide.with(context)
            .load(comment.profileUrl)
            .circleCrop()
            .placeholder(R.drawable.union)
            .error(R.drawable.union)
            .into(holder.ivProfile)

        holder.tvContent.visibility    = View.VISIBLE
        holder.layoutEditMode.visibility = View.GONE

        when (comment.role) {
            "OWNER" -> { holder.tvOwnerBadge.visibility = View.VISIBLE; holder.tvBadgeCustomer.visibility = View.GONE }
            "CUSTOMER" -> { holder.tvOwnerBadge.visibility = View.GONE; holder.tvBadgeCustomer.visibility = View.VISIBLE }
            else -> { holder.tvOwnerBadge.visibility = View.GONE; holder.tvBadgeCustomer.visibility = View.GONE }
        }

        holder.tvPostAuthorBadge.visibility = if (comment.userId == postAuthorId) View.VISIBLE else View.GONE
        holder.btnEdit.visibility   = if (comment.mine) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (comment.mine) View.VISIBLE else View.GONE
        holder.btnReport.visibility = if (!comment.mine) View.VISIBLE else View.GONE

        holder.btnReport.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("댓글 신고").setMessage("이 댓글을 신고하시겠습니까?")
                .setPositiveButton("신고") { _, _ ->
                    RetrofitClient.apiService.reportComment(comment.id, CommunityReportBody("부적절한 내용"))
                        .enqueue(object : Callback<ApiResponse<Any>> {
                            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                                Toast.makeText(context, if (response.isSuccessful) "신고가 접수되었습니다." else "신고 실패", Toast.LENGTH_SHORT).show()
                            }
                            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                                Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
                .setNegativeButton("취소", null).show()
        }

        holder.btnEdit.setOnClickListener {
            holder.tvContent.visibility = View.GONE
            holder.layoutEditMode.visibility = View.VISIBLE
            holder.etEditComment.setText(comment.content)
            holder.etEditComment.requestFocus()
        }

        holder.btnCancelEdit.setOnClickListener {
            holder.layoutEditMode.visibility = View.GONE
            holder.tvContent.visibility = View.VISIBLE
        }

        holder.btnSaveEdit.setOnClickListener {
            val newContent = holder.etEditComment.text.toString().trim()
            if (newContent.isEmpty()) { Toast.makeText(context, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            RetrofitClient.apiService.updateComment(comment.id, CommentRequest(null, newContent))
                .enqueue(object : Callback<ApiResponse<Any>> {
                    override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                        val pos = holder.adapterPosition
                        if (response.isSuccessful && pos != RecyclerView.NO_POSITION) {
                            comments[pos] = comment.copy(content = newContent)
                            notifyItemChanged(pos)
                        } else {
                            Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                        Toast.makeText(context, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        holder.btnDelete.setOnClickListener {
            RetrofitClient.apiService.deleteComment(comment.id)
                .enqueue(object : Callback<ApiResponse<Any>> {
                    override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                        if (response.isSuccessful) {
                            val pos = holder.adapterPosition
                            if (pos != RecyclerView.NO_POSITION && pos < comments.size) {
                                comments.removeAt(pos)
                                notifyItemRemoved(pos)
                                onCommentChanged()
                            }
                        } else {
                            Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                        Toast.makeText(context, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun getItemCount(): Int = comments.size

    private fun formatDate(dateStr: String) = try {
        if (dateStr.length >= 16) dateStr.substring(0, 16).replace("T", " ") else dateStr
    } catch (e: Exception) { dateStr }
}
