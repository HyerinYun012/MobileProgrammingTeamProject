package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityReportManageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportManageBinding
    private lateinit var reviewAdapter: ReviewReportAdapter
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadReportedReviews()
    }

    private fun loadReportedReviews() {
        apiService.getReviewReports(0, 100).enqueue(object : Callback<ApiResponse<PageResponse<ReviewReportItem>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<ReviewReportItem>>>,
                response: Response<ApiResponse<PageResponse<ReviewReportItem>>>
            ) {
                if (response.isSuccessful) {
                    val reports = response.body()?.data?.content ?: emptyList()
                    // status가 COMPLETED인 항목 제외
                    val filteredReports = reports.filter { it.status != "COMPLETED" }
                    reviewAdapter.setData(filteredReports)
                } else {
                    Toast.makeText(applicationContext, "신고 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewReportItem>>>, t: Throwable) {
                Log.e("ReportManage", "Error loading reports", t)
            }
        })
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewReportAdapter(
            onDeleteClick = { reviewId ->
                deleteReview(reviewId)
            },
            onCompleteClick = { reportId ->
                completeReview(reportId)
            },
            onItemClick = { item ->
                val intent = Intent(this, ReviewReadActivity::class.java)
                intent.putExtra("RESTAURANT_ID", item.restaurantId)
                startActivity(intent)
            }
        )
        binding.rvReviewReports.adapter = reviewAdapter
    }

    private fun deleteReview(reviewId: Long) {
        apiService.adminDeleteReview(reviewId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "리뷰가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    loadReportedReviews()
                } else {
                    Toast.makeText(applicationContext, "리뷰 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Log.e("ReportManage", "Error deleting review", t)
                Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun completeReview(reportId: Long) {
        apiService.completeReviewReport(reportId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "신고를 완료처리 하였습니다.", Toast.LENGTH_SHORT).show()
                    loadReportedReviews() // 목록 새로고침
                } else {
                    Toast.makeText(applicationContext, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Log.e("ReportManage", "Error completing report", t)
                Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
