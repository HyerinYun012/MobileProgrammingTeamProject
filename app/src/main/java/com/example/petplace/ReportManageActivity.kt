package com.example.petplace

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityReportManageBinding

class ReportManageActivity : AppCompatActivity() {

    // ViewBinding 및 헬퍼 객체 선언
    private lateinit var binding: ActivityReportManageBinding
    private lateinit var reviewAdapter: ReviewReportAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityReportManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // 뒤로가기 버튼 이벤트 처리
        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // 화면 재활성화 시 신고 목록 데이터 갱신
        loadReportedReviews()
    }

    // 신고 접수된 리뷰 데이터 로드 및 어댑터 갱신
    private fun loadReportedReviews() {
        // DB에서 신고 처리된 리뷰 목록 조회
        val reportedList = dbHelper.getReportedReviews()

        // 조회된 데이터를 ReviewReportItem 객체로 매핑
        val mappedList = reportedList.map {
            ReviewReportItem(
                id = it.id,
                reviewId = it.id,
                reviewContent = it.content,
                ownerName = "펫플유저",
                reason = "사장님 신고",
                status = "대기",
                createdAt = ""
            )
        }
        reviewAdapter.setData(mappedList)
    }

    // RecyclerView 및 어댑터 초기 설정
    private fun setupRecyclerView() {
        reviewAdapter = ReviewReportAdapter(
            onDeleteClick = { reportId ->
                // 대상 리뷰 DB 내 영구 삭제 처리
                dbHelper.deleteReview(reportId)

                // 삭제 완료 다이얼로그 출력 및 목록 갱신
                showCustomDialog("해당 리뷰를 영구 삭제했습니다.") {
                    loadReportedReviews()
                }
            },
            onItemClick = { item ->
                // 리뷰 상세 확인 페이지로 화면 전환
                val intent = Intent(this@ReportManageActivity, ReviewReadActivity::class.java)
                startActivity(intent)
            }
        )
        binding.rvReviewReports.adapter = reviewAdapter
    }
}