package com.gabojameong.petplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CommunityPostAdapter(
    private val posts: MutableList<PostDetailResponse>,
    private val onItemClick: (PostDetailResponse) -> Unit
) : RecyclerView.Adapter<CommunityPostAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView    = itemView.findViewById(R.id.tv_item_title)
        val tvPreview: TextView  = itemView.findViewById(R.id.tv_item_preview)
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_author_profile)
        val tvAuthor: TextView   = itemView.findViewById(R.id.tv_item_author)
        val tvDate: TextView     = itemView.findViewById(R.id.tv_item_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_community_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.tvTitle.text   = post.title
        holder.tvPreview.text = post.content
        holder.tvAuthor.text  = post.writerNickname
        holder.tvDate.text    = formatDate(post.createdAt)

        Glide.with(holder.itemView.context)
            .load(post.writerProfileUrl)
            .circleCrop()
            .placeholder(R.drawable.union)
            .error(R.drawable.union)
            .into(holder.ivProfile)

        holder.itemView.setOnClickListener { onItemClick(post) }
    }

    override fun getItemCount(): Int = posts.size

    private fun formatDate(dateStr: String) = try {
        dateStr.substring(0, 10).replace("-", ".")
    } catch (e: Exception) { dateStr }
}
