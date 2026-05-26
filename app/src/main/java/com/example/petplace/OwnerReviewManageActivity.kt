package com.example.petplace

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
import com.example.petplace.databinding.ActivityOwnerReviewManageBinding
import com.example.petplace.databinding.ItemOwnerReviewBinding // 🔥 사장님 전용 바인딩!

class OwnerReviewManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerReviewManageBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerReviewManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val reviewList = dbHelper.getAllReviews()
        binding.tvReviewCount.text = "${reviewList.size}개"

        // 어댑터 이름 'OwnerReviewAdapter'로 통일!
        binding.rvOwnerReviews.adapter = OwnerReviewAdapter(reviewList) { reviewId ->
            dbHelper.reportReview(reviewId)
            showCustomDialog("관리자에게 신고가 접수되었습니다.") {
                onResume()
            }
        }
    }

    // 🔥 슬라이더 + 인디케이터 기능 탑재된 '사장님' 전용 어댑터!
    inner class OwnerReviewAdapter(
        private val reviews: List<ReviewData>,
        private val onReportClick: (Int) -> Unit
    ) : RecyclerView.Adapter<OwnerReviewAdapter.ViewHolder>() {

        // 🔥 ItemOwnerReviewBinding 으로 싹 교체!
        inner class ViewHolder(private val itemBinding: ItemOwnerReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewData) {
                itemBinding.itemRatingBar.rating = review.rating
                itemBinding.tvReviewContent.text = review.content
                // 가게 이름 등이 있다면 여기서 추가 바인딩!

                // 📸 다중 이미지 처리 마법!
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

                // 🚨 신고 버튼 연결 (XML id가 btn_report_review 라고 가정!)
                // 만약 XML id가 다르면 itemBinding.버튼ID 로 수정해줘!
                itemBinding.btnReport.setOnClickListener {
                    onReportClick(review.id)
                }
            }

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

            private fun updateIndicators(position: Int, container: LinearLayout) {
                for (i in 0 until container.childCount) {
                    val imageView = container.getChildAt(i) as ImageView
                    imageView.setImageDrawable(getIndicatorDrawable(i == position))
                }
            }

            private fun getIndicatorDrawable(isActive: Boolean): GradientDrawable {
                return GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setSize(20, 20)
                    setColor(if (isActive) Color.parseColor("#FF8A4C") else Color.parseColor("#E0E0E0"))
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
}