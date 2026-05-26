package com.example.petplace

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2 // 🔥 ViewPager2 임포트 추가!
import com.example.petplace.databinding.ActivityMyReviewBinding
import com.example.petplace.databinding.ItemMyReviewBinding // 🔥 내 리뷰 전용 바인딩 임포트!

class MyReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyReviewBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val reviewList = dbHelper.getAllReviews()
        binding.tvReviewCount.text = "${reviewList.size}개"

        // 어댑터 이름 'MyReviewAdapter'로 통일!
        binding.rvMyReviews.adapter = MyReviewAdapter(reviewList) { reviewId ->
            dbHelper.deleteReview(reviewId)
            showCustomDialog("리뷰가 정상적으로 삭제되었습니다.") {
                onResume()
            }
        }
    }

    // 🔥 슬라이더 + 인디케이터 기능 탑재된 '내 리뷰' 전용 어댑터!
    inner class MyReviewAdapter(
        private val reviews: List<ReviewData>,
        private val onDeleteClick: (Int) -> Unit // 삭제 콜백 추가
    ) : RecyclerView.Adapter<MyReviewAdapter.ViewHolder>() {

        // 🔥 ItemMyReviewBinding 으로 싹 교체!
        inner class ViewHolder(private val itemBinding: ItemMyReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewData) {
                // 텍스트, 평점 등 기본 데이터 연결
                itemBinding.itemRatingBar.rating = review.rating
                itemBinding.tvReviewContent.text = review.content

                // 📸 다중 이미지 처리 마법 시작!
                if (!review.imageUri.isNullOrEmpty()) {
                    val imageList = review.imageUri.split(",")

                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE
                    itemBinding.indicatorLayout.visibility = View.VISIBLE

                    val sliderAdapter = ReadImageSliderAdapter(imageList)
                    itemBinding.vpReviewPhotos.adapter = sliderAdapter

                    if (imageList.size > 1) {
                        setupIndicators(imageList.size, itemBinding.indicatorLayout)
                    } else {
                        itemBinding.indicatorLayout.removeAllViews()
                    }

                    itemBinding.vpReviewPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            updateIndicators(position, itemBinding.indicatorLayout)
                        }
                    })
                } else {
                    itemBinding.vpReviewPhotos.visibility = View.GONE
                    itemBinding.indicatorLayout.visibility = View.GONE
                }

                itemBinding.btnDeleteReview.setOnClickListener {
                    onDeleteClick(review.id) // DB에 저장된 리뷰 고유 ID(Int형) 넘기기
                }
            }

            // 🎨 동그라미 점 UI 생성기
            private fun setupIndicators(count: Int, container: LinearLayout) {
                container.removeAllViews()
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) }

                for (i in 0 until count) {
                    val indicator = ImageView(container.context)
                    indicator.setImageDrawable(getIndicatorDrawable(i == 0))
                    indicator.layoutParams = layoutParams
                    container.addView(indicator)
                }
            }

            // 🎨 점 색깔 칠해주기
            private fun updateIndicators(position: Int, container: LinearLayout) {
                for (i in 0 until container.childCount) {
                    val imageView = container.getChildAt(i) as ImageView
                    imageView.setImageDrawable(getIndicatorDrawable(i == position))
                }
            }

            // 🎨 점 디자인
            private fun getIndicatorDrawable(isActive: Boolean): android.graphics.drawable.GradientDrawable {
                return android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setSize(20, 20)
                    setColor(if (isActive) android.graphics.Color.parseColor("#FF8A4C") else android.graphics.Color.parseColor("#E0E0E0"))
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // 🔥 여기도 ItemMyReviewBinding.inflate 로 교체!
            val binding = ItemMyReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
}