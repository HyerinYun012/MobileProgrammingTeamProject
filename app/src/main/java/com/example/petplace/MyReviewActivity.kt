package com.example.petplace

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityMyReviewBinding
import com.example.petplace.databinding.ItemMyReviewBinding

class MyReviewActivity : AppCompatActivity() {

    // ViewBinding 객체 선언
    private lateinit var binding: ActivityMyReviewBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityMyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 데이터베이스 헬퍼 초기화
        dbHelper = DatabaseHelper(this)

        // 뒤로가기 버튼 이벤트 처리
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // DB 리뷰 데이터 로드 및 UI 갱신
        val reviewList = dbHelper.getAllReviews()
        binding.tvReviewCount.text = "${reviewList.size}개"

        // 어댑터 초기화 및 리스트 연결
        binding.rvMyReviews.adapter = MyReviewAdapter(reviewList) { reviewId ->
            // 데이터베이스 내 해당 리뷰 삭제 처리
            dbHelper.deleteReview(reviewId)

            // 커스텀 다이얼로그 호출 및 화면 새로고침
            showCustomDialog("리뷰가 정상적으로 삭제되었습니다.") {
                onResume()
            }
        }
    }

    // 내 리뷰 목록 RecyclerView 어댑터 (ViewBinding 적용)
    inner class MyReviewAdapter(
        private val reviews: List<ReviewData>,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MyReviewAdapter.ViewHolder>() {

        // ViewHolder 정의 (ItemMyReviewBinding 사용)
        inner class ViewHolder(private val itemBinding: ItemMyReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(review: ReviewData) {
                // 데이터 바인딩
                itemBinding.tvStoreName.text = "방문한 매장"
                itemBinding.itemRatingBar.rating = review.rating
                itemBinding.tvReviewContent.text = review.content

                // 이미지 존재 여부에 따른 뷰 표시 상태 제어
                if (!review.imageUri.isNullOrEmpty()) {
                    itemBinding.cvPhotoContainer.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(Uri.parse(review.imageUri))
                        .into(itemBinding.ivReviewPhoto)
                } else {
                    itemBinding.cvPhotoContainer.visibility = View.GONE
                }

                // 삭제 버튼 클릭 이벤트 할당
                itemBinding.btnDeleteReview.setOnClickListener {
                    onDeleteClick(review.id)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 아이템 뷰 바인딩 인플레이트
            val binding = ItemMyReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
}