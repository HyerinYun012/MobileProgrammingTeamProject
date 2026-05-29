package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityInquiryDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InquiryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInquiryDetailBinding
    private val apiService = RetrofitClient.apiService
    private var inquiryId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInquiryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        inquiryId = intent.getLongExtra("INQUIRY_ID", -1L)
        
        if (inquiryId != -1L) {
            loadInquiryDetail()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCompleteInquiry.setOnClickListener {
            val answer = binding.editTextAnswer.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(this, "답변 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            completeInquiry(inquiryId, answer)
        }
    }

    private fun loadInquiryDetail() {
        apiService.getAdminInquiryDetail(inquiryId).enqueue(object : Callback<ApiResponse<InquiryDetailResponse>> {
            override fun onResponse(call: Call<ApiResponse<InquiryDetailResponse>>, response: Response<ApiResponse<InquiryDetailResponse>>) {
                if (response.isSuccessful) {
                    val inquiry = response.body()?.data
                    inquiry?.let {
                        binding.tvInquiryCategory.text = if (!it.title.isNullOrEmpty()) it.title else (it.category ?: "문의")
                        binding.tvInquiryContent.text = it.content ?: "내용 없음"

                        if (!it.answer.isNullOrEmpty()) {
                            binding.editTextAnswer.setText(it.answer)
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, "내역을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<InquiryDetailResponse>>, t: Throwable) {
                Log.e("InquiryDetail", "Error loading detail", t)
            }
        })
    }

    private fun completeInquiry(id: Long, replyText: String) {
        val body = mapOf("reply" to replyText)
        apiService.completeInquiry(id, body).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "답변이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    //status를 COMPLETED로 변경한 후 목록 새로고침 유도
                    finish()
                } else {
                    Toast.makeText(applicationContext, "답변 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Log.e("InquiryDetail", "Error completing inquiry", t)
            }
        })
    }
}
