package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gabojameong.petplace.databinding.ActivityInquiryManageBinding
import com.gabojameong.petplace.databinding.ItemInquiryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InquiryManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInquiryManageBinding
    private val apiService = RetrofitClient.apiService
    private lateinit var inquiryAdapter: InquiryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInquiryManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvInquiries.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        loadInquiries()
    }

    private fun loadInquiries() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getAdminInquiries(pageable).enqueue(object : Callback<ApiResponse<PageResponse<InquiryResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<InquiryResponse>>>,
                response: Response<ApiResponse<PageResponse<InquiryResponse>>>
            ) {
                if (response.isSuccessful) {
                    val inquiryList = (response.body()?.data?.content ?: emptyList()).toMutableList()
                    inquiryAdapter = InquiryAdapter(inquiryList) { inquiry ->
                        val intent = Intent(this@InquiryManageActivity, InquiryDetailActivity::class.java).apply {
                            putExtra("INQUIRY_ID", inquiry.id)
                        }
                        startActivity(intent)
                    }
                    binding.rvInquiries.adapter = inquiryAdapter

                    // 목록 로드 후 상세 정보를 병렬로 가져와서 카테고리 업데이트 (Lazy Loading)
                    inquiryList.forEachIndexed { index, inquiry ->
                        fetchInquiryDetail(inquiry.id, index)
                    }
                } else {
                    Toast.makeText(this@InquiryManageActivity, "문의 내역을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<InquiryResponse>>>, t: Throwable) {
                Log.e("InquiryManage", "Error loading inquiries", t)
            }
        })
    }

    private fun fetchInquiryDetail(id: Long, position: Int) {
        apiService.getAdminInquiryDetail(id).enqueue(object : Callback<ApiResponse<InquiryResponse>> {
            override fun onResponse(call: Call<ApiResponse<InquiryResponse>>, response: Response<ApiResponse<InquiryResponse>>) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { detail ->
                        // 카테고리 정보가 포함된 상세 데이터로 아이템 갱신
                        inquiryAdapter.updateItem(position, detail)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<InquiryResponse>>, t: Throwable) {
                Log.e("InquiryManage", "Detail fetch failed for ID: $id", t)
            }
        })
    }

    inner class InquiryAdapter(
        private val list: MutableList<InquiryResponse>,
        private val onItemClick: (InquiryResponse) -> Unit
    ) : RecyclerView.Adapter<InquiryAdapter.ViewHolder>() {

        fun updateItem(position: Int, updatedInquiry: InquiryResponse) {
            if (position < list.size) {
                list[position] = updatedInquiry
                notifyItemChanged(position)
            }
        }

        inner class ViewHolder(private val itemBinding: ItemInquiryBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(inquiry: InquiryResponse) {
                // 카테고리 표시 (상세 정보 로드 전후 대응)
                itemBinding.tvCategory.text = when (inquiry.category) {
                    "BUSINESS" -> "비즈니스 문의"
                    "ERROR" -> "오류 문의"
                    else -> "문의"
                }
                
                // 제목 표시
                itemBinding.tvTitle.text = if (!inquiry.title.isNullOrEmpty()) inquiry.title else "문의 #${inquiry.id}"
                
                // 상태 표시
                itemBinding.tvContentPreview.text = "상태: ${inquiry.status}"

                if (inquiry.status == "COMPLETED") {
                    itemBinding.root.setBackgroundResource(R.drawable.bg_border_gray)
                } else {
                    itemBinding.root.setBackgroundResource(R.drawable.bg_border_orange)
                }

                itemBinding.root.setOnClickListener { onItemClick(inquiry) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInquiryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size
    }
}
