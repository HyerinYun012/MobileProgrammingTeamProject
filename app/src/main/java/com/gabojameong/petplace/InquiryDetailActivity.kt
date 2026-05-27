package com.gabojameong.petplace

import android.os.Bundle
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
        val category = intent.getStringExtra("CATEGORY") ?: "분류 없음"
       // val email = intent.getStringExtra("EMAIL") ?: "이메일 없음"
        val content = intent.getStringExtra("CONTENT") ?: "내용 없음"

        binding.tvInquiryCategory.text = category
       // binding.tvInquiryEmail.text = email
        binding.tvInquiryContent.text = content

        binding.btnCompleteInquiry.setOnClickListener {
            if (inquiryId != -1L) {
                completeInquiry(inquiryId)
            }
        }
    }

    private fun completeInquiry(id: Long) {
        apiService.completeInquiry(id).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "처리 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {}
        })
    }
}
