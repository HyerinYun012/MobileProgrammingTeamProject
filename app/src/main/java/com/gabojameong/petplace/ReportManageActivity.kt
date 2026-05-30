package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityReportManageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportManageBinding
    private lateinit var reviewAdapter: ReviewReportAdapter
    private lateinit var communityAdapter: CommunityReportAdapter
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupReviewRecyclerView()
        setupCommunityRecyclerView()
        loadReportedReviews()
        loadCommunityReports()
    }

    // ─── 리뷰 신고 ────────────────────────────────────────────────────────────────

    private fun setupReviewRecyclerView() {
        reviewAdapter = ReviewReportAdapter(
            onDeleteClick = { reviewId -> deleteReview(reviewId) },
            onCompleteClick = { reportId -> completeReview(reportId) },
            onItemClick = { item ->
                val intent = Intent(this, ReviewReadActivity::class.java)
                intent.putExtra("RESTAURANT_ID", item.restaurantId)
                startActivity(intent)
            }
        )
        binding.rvReviewReports.adapter = reviewAdapter
    }

    private fun loadReportedReviews() {
        apiService.getReviewReports(0, 100).enqueue(object : Callback<ApiResponse<PageResponse<ReviewReportItem>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<ReviewReportItem>>>,
                response: Response<ApiResponse<PageResponse<ReviewReportItem>>>
            ) {
                if (response.isSuccessful) {
                    val reports = response.body()?.data?.content ?: emptyList()
                    reviewAdapter.setData(reports.filter { it.status != "COMPLETED" })
                } else {
                    Toast.makeText(applicationContext, "리뷰 신고 목록 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewReportItem>>>, t: Throwable) {
                Log.e("ReportManage", "Error loading review reports", t)
            }
        })
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
                Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun completeReview(reportId: Long) {
        apiService.completeReviewReport(reportId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "신고를 완료처리 하였습니다.", Toast.LENGTH_SHORT).show()
                    loadReportedReviews()
                } else {
                    Toast.makeText(applicationContext, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ─── 커뮤니티 신고 (게시글 / 댓글) ───────────────────────────────────────────

    private fun setupCommunityRecyclerView() {
        communityAdapter = CommunityReportAdapter(
            onDeleteClick = { item -> deleteCommunityContent(item) },
            onCompleteClick = { reportId -> completeCommunityReport(reportId) },
            onItemClick = { postId ->
                val intent = Intent(this, CommunityDetailActivity::class.java)
                intent.putExtra("POST_ID", postId)
                startActivity(intent)
            }
        )
        binding.rvCommunityReports.adapter = communityAdapter
    }

    private fun loadCommunityReports() {
        apiService.getCommunityReports("PENDING").enqueue(object : Callback<ApiResponse<PageResponse<CommunityReportResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<CommunityReportResponse>>>,
                response: Response<ApiResponse<PageResponse<CommunityReportResponse>>>
            ) {
                if (response.isSuccessful) {
                    val reports = response.body()?.data?.content ?: emptyList()
                    val pending = reports.filter { it.status != "COMPLETED" }
                    communityAdapter.setData(pending)

                    if (pending.isEmpty()) {
                        binding.tvCommunityEmpty.visibility = View.VISIBLE
                        binding.rvCommunityReports.visibility = View.GONE
                    } else {
                        binding.tvCommunityEmpty.visibility = View.GONE
                        binding.rvCommunityReports.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvCommunityEmpty.visibility = View.VISIBLE
                    Toast.makeText(applicationContext, "커뮤니티 신고 목록 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<CommunityReportResponse>>>, t: Throwable) {
                Log.e("ReportManage", "Error loading community reports", t)
                binding.tvCommunityEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun deleteCommunityContent(item: CommunityReportResponse) {
        if (item.commentId != null) {
            // 댓글 삭제
            apiService.adminDeleteCommunityComment(item.commentId).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadCommunityReports()
                    } else {
                        Toast.makeText(applicationContext, "댓글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // 게시글 삭제
            apiService.adminDeleteCommunityPost(item.postId).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadCommunityReports()
                    } else {
                        Toast.makeText(applicationContext, "게시글 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun completeCommunityReport(reportId: Long) {
        apiService.completeCommunityReport(reportId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "신고를 완료처리 하였습니다.", Toast.LENGTH_SHORT).show()
                    loadCommunityReports()
                } else {
                    Toast.makeText(applicationContext, "처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(applicationContext, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
