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
                    val inquiryList = response.body()?.data?.content ?: emptyList()
                    binding.rvInquiries.adapter = InquiryAdapter(inquiryList) { inquiry ->
                        val intent = Intent(this@InquiryManageActivity, InquiryDetailActivity::class.java).apply {
                            putExtra("INQUIRY_ID", inquiry.id)
                            putExtra("CATEGORY", inquiry.category)
                            putExtra("EMAIL", inquiry.email) // User email if available in DTO
                            putExtra("CONTENT", inquiry.content)
                        }
                        startActivity(intent)
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

    inner class InquiryAdapter(
        private val list: List<InquiryResponse>,
        private val onItemClick: (InquiryResponse) -> Unit
    ) : RecyclerView.Adapter<InquiryAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemInquiryBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(inquiry: InquiryResponse) {
                itemBinding.tvCategory.text = inquiry.category
                itemBinding.tvContentPreview.text = inquiry.content

                // 배경 리소스 처리 (상태에 따라)
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
