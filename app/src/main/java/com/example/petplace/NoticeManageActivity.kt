package com.example.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityNoticeManageBinding
import com.example.petplace.databinding.ItemNoticeManageBinding

class NoticeManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoticeManageBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }

        // 공지 쓰기 버튼
        binding.btnWriteNotice.setOnClickListener {
            val intent = Intent(this, NoticeWriteActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val notices = dbHelper.getNoticesByStoreId(1) // 임시 storeId 1
        binding.rvNotices.adapter = NoticeAdapter(notices)
    }

    inner class NoticeAdapter(private val notices: List<NoticeData>) : RecyclerView.Adapter<NoticeAdapter.ViewHolder>() {
        inner class ViewHolder(private val itemBinding: ItemNoticeManageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(notice: NoticeData) {
                itemBinding.tvNoticeTitle.text = notice.title

                if (notice.bannerUri.isNotEmpty()) {
                    itemBinding.ivNoticeBanner.visibility = View.VISIBLE
                    Glide.with(itemView).load(Uri.parse(notice.bannerUri)).into(itemBinding.ivNoticeBanner)
                } else {
                    itemBinding.ivNoticeBanner.visibility = View.GONE
                }

                // 공지 삭제 버튼 클릭 리스너
                itemBinding.btnDeleteNotice.setOnClickListener {
                    // 1. 먼저 데이터베이스에서 해당 공지 삭제 처리!
                    dbHelper.deleteNotice(notice.id)

                    // 2. 삭제 완료 커스텀 다이얼로그 띄우기
                    showCustomDialog("공지가 삭제되었습니다.") {
                        // 3. 다이얼로그의 '확인' 버튼을 누르면 목록화면(onResume)을 다시 호출해서 화면 새로고침
                        onResume()
                    }
                }

                // 아이템 클릭 시 수정 화면으로 이동 (ID 넘김)
                itemView.setOnClickListener {
                    val intent = Intent(this@NoticeManageActivity, NoticeWriteActivity::class.java)
                    intent.putExtra("NOTICE_ID", notice.id)
                    startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(ItemNoticeManageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(notices[position])
        override fun getItemCount() = notices.size
    }
}