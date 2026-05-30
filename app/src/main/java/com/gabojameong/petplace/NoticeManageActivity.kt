package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private val myRestaurants = mutableListOf<OwnerRestaurantSummary>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvNotices.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnWriteNotice.setOnClickListener {
            if (restaurantId == -1L) {
                Toast.makeText(this, "먼저 업장을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, NoticeWriteActivity::class.java)
                .putExtra("RESTAURANT_ID", restaurantId))
        }

        // OwnerMypageActivity에서 전달된 ID가 있으면 바로 사용
        val intentId = intent.getLongExtra("RESTAURANT_ID", -1L)
        if (intentId != -1L) {
            restaurantId = intentId
            loadNotices()
        }

        fetchMyRestaurants()
    }

    override fun onResume() {
        super.onResume()
        if (restaurantId != -1L) loadNotices()
    }

    // ─── 업장 Spinner ──────────────────────────────────────────────

    private fun fetchMyRestaurants() {
        RetrofitClient.apiService.getMyRestaurants()
            .enqueue(object : Callback<ApiResponse<List<OwnerRestaurantSummary>>> {
                override fun onResponse(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, response: Response<ApiResponse<List<OwnerRestaurantSummary>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        myRestaurants.clear()
                        myRestaurants.addAll(response.body()?.data ?: emptyList())
                    }
                    setupSpinner()
                }
                override fun onFailure(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, t: Throwable) {
                    Log.e("NoticeManage", "업장 목록 로드 실패", t)
                    setupSpinner()
                }
            })
    }

    private fun setupSpinner() {
        val names = mutableListOf("업장을 선택해주세요")
        names.addAll(myRestaurants.map { it.name })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRestaurant.adapter = adapter

        // intent로 받은 ID와 일치하는 업장 자동 선택
        if (restaurantId != -1L) {
            val idx = myRestaurants.indexOfFirst { it.id == restaurantId }
            if (idx >= 0) binding.spinnerRestaurant.setSelection(idx + 1)
        }

        binding.spinnerRestaurant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    restaurantId = -1L
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvNotices.visibility = View.GONE
                } else {
                    restaurantId = myRestaurants[position - 1].id
                    loadNotices()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ─── 공지 목록 로드 ────────────────────────────────────────────

    private fun loadNotices() {
        val pageable = mapOf("page" to "0", "size" to "50")
        apiService.getNotices(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<NoticeResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, response: Response<ApiResponse<PageResponse<NoticeResponse>>>) {
                if (response.isSuccessful) {
                    val notices = response.body()?.data?.content ?: emptyList()
                    if (notices.isEmpty()) {
                        binding.tvEmpty.text = "등록된 공지가 없습니다."
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvNotices.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvNotices.visibility = View.VISIBLE
                        binding.rvNotices.adapter = NoticeAdapter(notices)
                    }
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
                    this@NoticeManageActivity.showCustomDialog("공지가 삭제되었습니다.") { loadNotices() }
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
                itemBinding.btnDeleteNotice.setOnClickListener { deleteNotice(notice.id) }
                itemView.setOnClickListener {
                    startActivity(Intent(this@NoticeManageActivity, NoticeWriteActivity::class.java)
                        .putExtra("NOTICE_ID", notice.id)
                        .putExtra("RESTAURANT_ID", restaurantId))
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemNoticeManageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(notices[position])
        override fun getItemCount() = notices.size
    }
}
