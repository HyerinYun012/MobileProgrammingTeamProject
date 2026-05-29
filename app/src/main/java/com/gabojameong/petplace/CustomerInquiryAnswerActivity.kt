package com.gabojameong.petplace

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerInquiryAnswerActivity : AppCompatActivity() {

    private lateinit var tvInquiryCategory: TextView
    private lateinit var tvShopName: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var tvAnswerLabel: TextView
    private lateinit var tvAnswerContent: TextView
    private lateinit var rvImages: RecyclerView

    private var inquiryId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_inquire_answer)

        inquiryId = intent.getStringExtra("INQUIRY_ID")?.toLongOrNull() ?: -1L

        tvInquiryCategory = findViewById(R.id.tv_inquiry_category)
        tvShopName        = findViewById(R.id.tv_shop_name)
        tvTitle           = findViewById(R.id.tv_title)
        tvContent         = findViewById(R.id.tv_content)
        tvAnswerLabel     = findViewById(R.id.tv_answer_label)
        tvAnswerContent   = findViewById(R.id.tv_answer_content)
        rvImages          = findViewById(R.id.rv_inquiry_images)

        findViewById<ImageView>(R.id.imageView7).setOnClickListener { finish() }

        if (inquiryId != -1L) fetchInquiryDetail(inquiryId)
        else { Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show(); finish() }
    }

    private fun fetchInquiryDetail(id: Long) {
        RetrofitClient.apiService.getInquiryDetail(id)
            .enqueue(object : Callback<ApiResponse<InquiryDetailResponse>> {
                override fun onResponse(call: Call<ApiResponse<InquiryDetailResponse>>, response: Response<ApiResponse<InquiryDetailResponse>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updateUI(it) }
                    } else {
                        Toast.makeText(this@CustomerInquiryAnswerActivity, "상세 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<InquiryDetailResponse>>, t: Throwable) {
                    Log.e("CustomerInquiry", "서버 통신 실패", t)
                    Toast.makeText(this@CustomerInquiryAnswerActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(data: InquiryDetailResponse) {
        tvInquiryCategory.text = when (data.category?.trim()?.uppercase()) {
            "GENERAL" -> "일반 문의"
            "ERROR"   -> "오류 문의"
            else      -> data.category ?: ""
        }
        tvAnswerLabel.text = if (tvInquiryCategory.text == "오류 문의") "관리자의 답변" else "사장님 답변"
        tvShopName.text = data.restaurantName ?: "미정"
        tvTitle.text   = data.title ?: ""
        tvContent.text = data.content ?: ""

        if (data.answer.isNullOrEmpty()) {
            tvAnswerContent.text = "문의 진행 중입니다. 조금만 기다려주세요."
            tvAnswerContent.setTextColor(Color.parseColor("#999999"))
        } else {
            tvAnswerContent.text = data.answer
            tvAnswerContent.setTextColor(Color.parseColor("#000000"))
        }

        if (data.imageUrls.isNullOrEmpty()) {
            rvImages.visibility = View.GONE
        } else {
            rvImages.visibility = View.VISIBLE
            rvImages.adapter = PostImageAdapter(data.imageUrls) { url ->
                startActivity(Intent(this, ImageViewerActivity::class.java).putExtra("IMAGE_URL", url))
            }
            rvImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }
    }
}
