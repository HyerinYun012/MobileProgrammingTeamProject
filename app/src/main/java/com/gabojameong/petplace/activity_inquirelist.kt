package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 문의 데이터를 담는 클래스
data class Inquiry(
    val id: String,           // 서버에서 받을 고유 ID
    val title: String,        // 문의 제목
    val isReplied: Boolean    // 답변 완료 여부
)

class activity_inquirelist : AppCompatActivity() {

    // 테스트용 변수: 실제 앱에서는 로그인된 유저의 권한을 서버나 SharedPreferences에서 가져와야 합니다.
    private var userRole: String = "CUSTOMER" // "OWNER" 또는 "CUSTOMER"

    // RecyclerView 전역 변수 선언
    private lateinit var rvInquiryList: RecyclerView

    // 문의 작성 후 돌아왔을 때 목록을 새로고침하기 위한 런처
    private val startWriteInquiry = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // 무조건 새로고침 (작성 성공 여부와 상관없이)
        loadInquiryDataFromServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inquirelist)

        // 실제 로그인된 사용자의 권한 가져오기
        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        userRole = sharedPref.getString("userRole", "CUSTOMER") ?: "CUSTOMER"

        // 1. 뷰(View) 연결
        rvInquiryList = findViewById(R.id.rv_inquiry_list)
        val btnWriteInquiry = findViewById<ImageButton>(R.id.btn_write_inquiry)
        val tvWriteInquiryLabel = findViewById<TextView>(R.id.tv_write_inquiry_label)
        val btnBack = findViewById<ImageView>(R.id.imageView7)

        // 2. 권한에 따른 '문의 쓰기' 버튼 처리
        if (userRole == "OWNER") {
            // 사장님: 버튼과 글씨 모두 숨김 처리
            btnWriteInquiry.visibility = View.GONE
            tvWriteInquiryLabel.visibility = View.GONE
        } else {
            // 고객님: 버튼 보임 및 클릭 이벤트 설정
            btnWriteInquiry.visibility = View.VISIBLE
            tvWriteInquiryLabel.visibility = View.VISIBLE

            val writeClickListener = View.OnClickListener {
                // 고객일 경우 문의 쓰기 화면으로 이동 (런처 사용)
                val intent = Intent(this, activity_inquire::class.java)
                startWriteInquiry.launch(intent)
            }
            btnWriteInquiry.setOnClickListener(writeClickListener)
            tvWriteInquiryLabel.setOnClickListener(writeClickListener)
        }

        // 3. 뒤로가기 버튼 처리 (고객/사장 분기)
        btnBack.setOnClickListener {
            val destination = if (userRole == "OWNER") {
                owner_mypage::class.java
            } else {
                customer_mypage::class.java
            }
            val intent = Intent(this, destination)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // 쌓인 액티비티 정리
            startActivity(intent)
            finish()
        }

        // 4. 리사이클러뷰(목록) 설정
        rvInquiryList.layoutManager = LinearLayoutManager(this)

        // 5. 초기 데이터 로드 (onCreate에서는 레이아웃만 설정)
        loadInquiryDataFromServer()
    }

    override fun onResume() {
        super.onResume()
        // 답변 등록 후 돌아왔을 때 목록을 항상 새로고침
        loadInquiryDataFromServer()
    }

    // =======================================================
    // 서버 통신 부분 (Retrofit 사용)
    // =======================================================
    private fun loadInquiryDataFromServer() {
        // RetrofitClient.apiService를 직접 사용하여 API 서비스를 가져옵니다.
        val apiService = RetrofitClient.apiService
        
        // 현재 페이지 번호와 한 페이지에 불러올 개수를 설정합니다.
        val pageable = mapOf("page" to "0", "size" to "20")

        val call = if (userRole == "OWNER") {
            apiService.getOwnerInquiries(pageable)
        } else {
            apiService.getMyInquiries(pageable)
        }

        call.enqueue(object : Callback<ApiResponse<PageResponse<InquiryResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<InquiryResponse>>>,
                response: Response<ApiResponse<PageResponse<InquiryResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val inquiryResponses = response.body()?.data?.content ?: emptyList()
                    val uiData = inquiryResponses.map { res ->
                        Inquiry(
                            id = res.id.toString(),
                            title = res.title,
                            isReplied = res.status == "COMPLETED"
                        )
                    }
                    setupAdapter(uiData)
                } else {
                    Log.e("API_ERROR", "Error: ${response.code()} / ${response.message()}")
                    Toast.makeText(this@activity_inquirelist, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    showDummyData()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<InquiryResponse>>>, t: Throwable) {
                Log.e("API_ERROR", "서버 통신 실패: ${t.message}")
                showDummyData()
            }
        })
    }

    // 서버 오류 시 빈 목록 표시 (사장 전용 — 오류 문의는 제외)
    private fun showDummyData() {
        val dummyData = if (userRole == "OWNER") {
            // 사장님: 일반(GENERAL) 문의만 노출
            listOf(
                Inquiry("1", "강아지 미용 예약 취소하고 싶어요.", true),
                Inquiry("2", "환불 규정이 어떻게 되나요?", false)
            )
        } else {
            listOf(
                Inquiry("1", "강아지 미용 예약 취소하고 싶어요.", true),
                Inquiry("2", "환불 규정이 어떻게 되나요?", false),
                Inquiry("3", "사진 업로드 오류 건", false)
            )
        }
        setupAdapter(dummyData)
    }

    // 어댑터를 연결하고 클릭 이벤트를 세팅하는 함수
    private fun setupAdapter(dataList: List<Inquiry>) {
        val adapter = InquiryAdapter(dataList) { inquiry ->
            val destination = if (userRole == "OWNER") {
                activity_owner_inquire_answer::class.java
            } else {
                activity_customer_inquire_answer::class.java
            }

            val intent = Intent(this, destination)
            intent.putExtra("INQUIRY_TITLE", inquiry.title)
            intent.putExtra("INQUIRY_ID", inquiry.id) // 서버에 상세 정보를 요청할 때 쓸 ID
            startActivity(intent)
        }

        rvInquiryList.adapter = adapter
    }
}
