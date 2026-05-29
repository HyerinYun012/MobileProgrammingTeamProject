package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityNoticeManageBinding
import com.gabojameong.petplace.databinding.ItemNoticeManageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NoticeManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoticeManageBinding
    private val apiService = RetrofitClient.apiService
    private var restaurantId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 식당 ID를 받아옴 (수정 시 필수)
        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)
        if (restaurantId == -1L) {
            Toast.makeText(this, "업장 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.rvNotices.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        // 공지 작성 버튼
        binding.btnWriteNotice.setOnClickListener {
            val intent = Intent(this, NoticeWriteActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurantId)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotices()
    }

    private fun loadNotices() {
        val pageable = mapOf("page" to "0", "size" to "50")
        apiService.getNotices(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<NoticeResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<NoticeResponse>>>,
                response: Response<ApiResponse<PageResponse<NoticeResponse>>>
            ) {
                if (response.isSuccessful) {
                    val notices = response.body()?.data?.content ?: emptyList()
                    binding.rvNotices.adapter = NoticeAdapter(notices)
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, t: Throwable) {
                Log.e("NoticeManage", "Error loading notices", t)
            }
        })
    }

    private fun deleteNotice(noticeId: Long) {
        apiService.deleteNotice(noticeId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    this@NoticeManageActivity.showCustomDialog("공지가 삭제되었습니다.") {
                        loadNotices()
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(applicationContext, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class NoticeAdapter(private val notices: List<NoticeResponse>) : RecyclerView.Adapter<NoticeAdapter.ViewHolder>() {
        inner class ViewHolder(private val itemBinding: ItemNoticeManageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(notice: NoticeResponse) {
                itemBinding.tvNoticeTitle.text = notice.title

                if (!notice.thumbnailUrl.isNullOrEmpty()) {
                    itemBinding.ivNoticeBanner.visibility = View.VISIBLE
                    Glide.with(itemView.context).load(notice.thumbnailUrl).into(itemBinding.ivNoticeBanner)
                } else {
                    itemBinding.ivNoticeBanner.visibility = View.GONE
                }

                itemBinding.btnDeleteNotice.setOnClickListener {
                    deleteNotice(notice.id)
                }

                itemView.setOnClickListener {
                    val intent = Intent(this@NoticeManageActivity, NoticeWriteActivity::class.java).apply {
                        putExtra("NOTICE_ID", notice.id)
                        putExtra("RESTAURANT_ID", restaurantId)
                    }
                    startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemNoticeManageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(notices[position])
        override fun getItemCount() = notices.size
    }
}