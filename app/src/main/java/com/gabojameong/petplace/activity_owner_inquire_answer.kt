package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class activity_owner_inquire_answer : AppCompatActivity() {

    private lateinit var tvInquiryCategory: TextView
    private lateinit var tvShopName: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var rvImages: RecyclerView

    private lateinit var etAnswerInput: EditText
    private lateinit var btnSaveAnswer: Button
    private lateinit var tvSavedAnswer: TextView

    private var inquiryId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_inquire_answer)

        inquiryId = intent.getStringExtra("INQUIRY_ID")?.toLongOrNull() ?: -1L

        initViews()

        findViewById<ImageView>(R.id.imageView7).setOnClickListener {
            finish()
        }

        if (inquiryId != -1L) {
            fetchInquiryDetailFromServer(inquiryId)
        }

        findViewById<TextView>(R.id.btn_report).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("문의 신고")
                .setMessage("이 문의를 신고하시겠습니까?")
                .setPositiveButton("신고") { _, _ -> reportInquiry() }
                .setNegativeButton("취소", null)
                .show()
        }

        btnSaveAnswer.setOnClickListener {
            saveAnswerToServer()
        }
    }

    private fun initViews() {
        tvInquiryCategory = findViewById(R.id.tv_inquiry_category)
        tvShopName = findViewById(R.id.tv_shop_name)
        tvTitle = findViewById(R.id.tv_title)
        tvContent = findViewById(R.id.tv_content)
        rvImages = findViewById(R.id.rv_inquiry_images)

        etAnswerInput = findViewById(R.id.et_answer_input)
        btnSaveAnswer = findViewById(R.id.btn_save_answer)
        tvSavedAnswer = findViewById(R.id.tv_saved_answer)
    }

    private fun fetchInquiryDetailFromServer(id: Long) {
        RetrofitClient.apiService.getOwnerInquiryDetail(id).enqueue(object : Callback<ApiResponse<InquiryDetailResponse>> {
            override fun onResponse(call: Call<ApiResponse<InquiryDetailResponse>>, response: Response<ApiResponse<InquiryDetailResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { data ->
                        updateUI(data)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<InquiryDetailResponse>>, t: Throwable) {
                Log.e("API_ERROR", "서버 통신 실패", t)
            }
        })
    }

    private fun updateUI(data: InquiryDetailResponse) {
        tvInquiryCategory.text = when (data.category) {
            "GENERAL" -> "일반 문의"
            "ERROR" -> "오류 문의"
            else -> data.category
        }
        tvShopName.text = data.shopName ?: "미정"
        tvTitle.text = data.title
        tvContent.text = data.content

        if (data.imageUrls.isNullOrEmpty()) {
            rvImages.visibility = View.GONE
        } else {
            rvImages.visibility = View.VISIBLE
            rvImages.apply {
                adapter = PostImageAdapter(data.imageUrls) { url ->
                    val intent = Intent(this@activity_owner_inquire_answer, ImageViewerActivity::class.java)
                    intent.putExtra("IMAGE_URL", url)
                    startActivity(intent)
                }
                layoutManager = LinearLayoutManager(this@activity_owner_inquire_answer, LinearLayoutManager.HORIZONTAL, false)
            }
        }

        if (data.answer.isNullOrEmpty()) {
            setEditMode()
        } else {
            setViewMode(data.answer)
        }
    }

    private fun saveAnswerToServer() {
        val answerText = etAnswerInput.text.toString().trim()
        if (answerText.isEmpty()) {
            Toast.makeText(this, "답변 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AnswerRequest(answerText)

        RetrofitClient.apiService.completeOwnerInquiry(inquiryId, request).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@activity_owner_inquire_answer, "답변이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    setViewMode(answerText) // 성공 시 즉시 화면을 읽기 전용으로 변환
                } else {
                    Toast.makeText(this@activity_owner_inquire_answer, "등록 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(this@activity_owner_inquire_answer, "서버 연결 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun reportInquiry() {
        if (inquiryId == -1L) return
        RetrofitClient.apiService.reportOwnerInquiry(inquiryId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@activity_owner_inquire_answer, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@activity_owner_inquire_answer, "신고 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@activity_owner_inquire_answer, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setEditMode() {
        etAnswerInput.visibility = View.VISIBLE
        btnSaveAnswer.visibility = View.VISIBLE
        tvSavedAnswer.visibility = View.GONE
    }

    private fun setViewMode(answer: String) {
        tvSavedAnswer.text = answer
        tvSavedAnswer.visibility = View.VISIBLE

        etAnswerInput.visibility = View.GONE
        btnSaveAnswer.visibility = View.GONE
    }
}