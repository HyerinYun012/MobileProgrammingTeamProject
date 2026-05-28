package com.example.petplace // 🚨 본인 패키지명 확인!

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityOwnerReviewManageBinding
import com.example.petplace.databinding.ItemOwnerReviewBinding
import com.example.petplace.network.RetrofitClient
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.PageResponse
import com.example.petplace.network.ReviewResponse
import com.example.petplace.network.ReportRequest // 아까 만든 DTO!
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerReviewManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerReviewManageBinding

    // 🚨 내 가게 식당 아이디 (임시로 1번!)
    private val myRestaurantId = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerReviewManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        fetchOwnerReviewsFromServer()
    }

    // =========================================================================
    // 🌐 내 가게 리뷰 조회 (GET)
    // =========================================================================
    private fun fetchOwnerReviewsFromServer() {
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "20",
            "sort" to "createdAt,DESC"
        )

        RetrofitClient.apiService.getReviews(myRestaurantId, pageableMap)
            .enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<ReviewResponse>>>,
                    response: Response<ApiResponse<PageResponse<ReviewResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pageData = response.body()?.data
                        val reviewList = pageData?.content ?: emptyList()

                        binding.tvReviewCount.text = "${pageData?.totalElements ?: 0}개"

                        binding.rvOwnerReviews.adapter = OwnerReviewAdapter(reviewList) { reviewId ->
                            // 🔥 신고 버튼 눌리면 이 함수 실행!
                            reportReviewToServer(reviewId)
                        }
                    } else {
                        Toast.makeText(this@OwnerReviewManageActivity, "리뷰를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                    Toast.makeText(this@OwnerReviewManageActivity, "통신 에러", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🚨 리뷰 신고하기 (POST)
    // =========================================================================
    private fun reportReviewToServer(reviewId: Long) {
        val requestBody = ReportRequest("부적절한 욕설 및 비방이 포함되어 있습니다.")

        // 🔥 주원님이 가지고 있는 사장님 토큰을 여기에 쏙! (Bearer 띄어쓰기 필수!)
        val ownerToken = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxNCIsInJvbGUiOiJPV05FUiIsImlhdCI6MTc3OTk4MDM2MSwiZXhwIjoxNzgwMDY2NzYxfQ.GmFF5JO82De6MZ4R0GS7OSpfdAx5iRN1VLIzVZ2FYBYhOy-ZoEHLnYsbwqFS6cjgRdxpZt2cl9Ml9iYw5HERXA" // 👈 찐 토큰 값으로 변경!

        RetrofitClient.apiService.reportReview(ownerToken, reviewId, requestBody)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        showReportCompleteDialog()
                        fetchOwnerReviewsFromServer()
                    } else {
                        // 🔥 403 찐 원인 까보기 로그!!
                        val errorBody = response.errorBody()?.string()
                        Log.e("ReportDebug", "🚨 서버 에러 코드: ${response.code()}")
                        Log.e("ReportDebug", "🚨 서버가 뱉은 진짜 거절 사유: $errorBody")
                        Log.e("ReportDebug", "🚨 내가 보낸 토큰 앞뒤 확인: [$ownerToken]")
                        Log.e("ReportDebug", "🚨 신고 요청한 리뷰 ID: $reviewId")

                        Toast.makeText(this@OwnerReviewManageActivity, "신고 접수 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Log.e("ReportDebug", "🚨 통신 자체 실패: ${t.message}")
                    Toast.makeText(this@OwnerReviewManageActivity, "신고 통신 에러", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🎨 사장님 전용 리사이클러뷰 어댑터 (단일 이미지 Glide 적용)
    // =========================================================================
    inner class OwnerReviewAdapter(
        private val reviews: List<ReviewResponse>,
        private val onReportClick: (Long) -> Unit
    ) : RecyclerView.Adapter<OwnerReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemOwnerReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewResponse) {
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content
                itemBinding.tvName.text = review.writerName ?: "고객님"

                // 📸 단일 이미지 처리 (Glide 장착!)
                if (!review.imageUrl.isNullOrEmpty()) {
                    itemBinding.ivReviewPhoto.visibility = View.VISIBLE
                    Glide.with(itemBinding.root.context)
                        .load(review.imageUrl)
                        .centerCrop()
                        .into(itemBinding.ivReviewPhoto)
                } else {
                    itemBinding.ivReviewPhoto.visibility = View.GONE
                }

                // 🚨 신고 버튼 이벤트 연결
                itemBinding.btnReport.setOnClickListener {
                    // 서버 응답 모델에 있는 진짜 ID(reviewId 또는 id)를 넘겨줌!
                    val id = review.id?.toLong() ?: 0L // 🚨 ReviewResponse 모델의 변수명에 맞게 수정!
                    onReportClick(id)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemOwnerReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
    private fun showReportCompleteDialog() {
        // 1. 팝업창 객체 만들기
        val dialog = android.app.Dialog(this)

        // 2. 주원님이 만든 예쁜 커스텀 XML 화면 연결! (🚨 파일명 dialog_custom 맞는지 확인!)
        dialog.setContentView(R.layout.dialog_custom)

        // 3. 🔥 핵심 마법: 팝업창 기본 배경을 투명하게 날려버리기!
        // (이걸 안 하면 주원님이 만든 둥근 모서리 뒤에 네모난 흰색 배경이 보기 싫게 튀어나옴!)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // 4. 바깥 화면 눌러서 팝업창 꺼지는 거 방지
        dialog.setCancelable(false)

        // 5. XML 안에 있는 텍스트뷰 찾아서 문구 세팅
        val tvMessage = dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_message)
        tvMessage.text = "관리자에게 신고가 접수되었습니다." // 원하는 문구로 변경 가능!

        // 6. 확인 버튼 클릭 이벤트 연결
        val btnConfirm = dialog.findViewById<android.widget.TextView>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            dialog.dismiss() // 팝업창 닫기
            fetchOwnerReviewsFromServer() // 목록 새로고침해서 갱신!
        }

        // 7. 짠! 화면에 띄우기
        dialog.show()
    }
}