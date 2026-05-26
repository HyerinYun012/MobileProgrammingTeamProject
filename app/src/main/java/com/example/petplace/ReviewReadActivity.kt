package com.example.petplace

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.petplace.databinding.ActivityReviewReadBinding
import com.example.petplace.databinding.ItemReviewReadBinding

class ReviewReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewReadBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnWriteReview.setOnClickListener {
            val intent = Intent(this, ReviewWriteActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면 재활성화 시 최신 리뷰 데이터 로드 및 어댑터 연결
        val reviewList = dbHelper.getAllReviews()
        binding.rvReviews.adapter = ReviewAdapter(reviewList)
    }

    // 리뷰 목록 RecyclerView 어댑터
    inner class ReviewAdapter(private val reviews: List<ReviewData>) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemReviewReadBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewData) {
                // 텍스트 및 평점 데이터 바인딩
                itemBinding.itemRatingBar.rating = review.rating
                itemBinding.tvReviewContent.text = review.content

                // 다중 이미지 데이터 존재 시 슬라이더 구성
                if (!review.imageUri.isNullOrEmpty()) {
                    val imageList = review.imageUri.split(",")

                    // ViewPager2 및 인디케이터 레이아웃 가시화
                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE
                    itemBinding.indicatorLayout.visibility = View.VISIBLE

                    // 어댑터 연결
                    val sliderAdapter = ReadImageSliderAdapter(imageList)
                    itemBinding.vpReviewPhotos.adapter = sliderAdapter

                    // 인디케이터 초기 생성 및 설정 (사진이 2장 이상일 때만 표시)
                    if (imageList.size > 1) {
                        setupIndicators(imageList.size, itemBinding.indicatorLayout)
                    } else {
                        itemBinding.indicatorLayout.removeAllViews()
                    }

                    // 페이지 변경 시 인디케이터 상태 갱신 콜백 등록
                    itemBinding.vpReviewPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            updateIndicators(position, itemBinding.indicatorLayout)
                        }
                    })
                } else {
                    // 이미지가 없는 경우 관련 뷰 숨김 처리
                    itemBinding.vpReviewPhotos.visibility = View.GONE
                    itemBinding.indicatorLayout.visibility = View.GONE
                }
            }

            // 인디케이터(원형 점) UI 동적 생성 메서드
            private fun setupIndicators(count: Int, container: LinearLayout) {
                container.removeAllViews()
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) } // 점 사이 간격 설정

                for (i in 0 until count) {
                    val indicator = ImageView(container.context)
                    indicator.setImageDrawable(getIndicatorDrawable(i == 0)) // 첫 번째 점 활성화
                    indicator.layoutParams = layoutParams
                    container.addView(indicator)
                }
            }

            // 인디케이터 활성 상태 업데이트 메서드
            private fun updateIndicators(position: Int, container: LinearLayout) {
                for (i in 0 until container.childCount) {
                    val imageView = container.getChildAt(i) as ImageView
                    imageView.setImageDrawable(getIndicatorDrawable(i == position))
                }
            }

            // 원형 점 Drawable 생성 메서드 (true: 주황색, false: 회색)
            private fun getIndicatorDrawable(isActive: Boolean): GradientDrawable {
                return GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setSize(20, 20) // 점 크기 설정
                    setColor(if (isActive) Color.parseColor("#FF8A4C") else Color.parseColor("#E0E0E0"))
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemReviewReadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
}