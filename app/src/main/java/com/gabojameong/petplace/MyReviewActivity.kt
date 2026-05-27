package com.gabojameong.petplace

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gabojameong.petplace.databinding.ActivityMyReviewBinding
import com.gabojameong.petplace.databinding.ItemMyReviewBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyReviewBinding
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadMyReviews()
    }

    private fun loadMyReviews() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getMyReviews(pageable).enqueue(object : Callback<ApiResponse<PageResponse<MyReviewResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MyReviewResponse>>>,
                response: Response<ApiResponse<PageResponse<MyReviewResponse>>>
            ) {
                if (response.isSuccessful) {
                    val reviews = response.body()?.data?.content ?: emptyList()
                    binding.tvReviewCount.text = "${reviews.size}개"

                    binding.rvMyReviews.adapter = MyReviewAdapter(reviews) { reviewId ->
                        deleteReview(reviewId)
                    }
                } else {
                    Toast.makeText(this@MyReviewActivity, "리뷰를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MyReviewResponse>>>, t: Throwable) {
                Log.e("MyReviewActivity", "네트워크 오류", t)
            }
        })
    }

    private fun deleteReview(reviewId: Long) {
        apiService.deleteReview(reviewId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    showCustomDialog("리뷰가 정상적으로 삭제되었습니다.") {
                        loadMyReviews()
                    }
                } else {
                    Toast.makeText(this@MyReviewActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Log.e("MyReviewActivity", "삭제 요청 실패", t)
            }
        })
    }

    inner class MyReviewAdapter(
        private val reviews: List<MyReviewResponse>,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.Adapter<MyReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemMyReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: MyReviewResponse) {
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content

                // 📸 이미지 처리: ViewPager2와 Adapter를 사용한 다중 이미지 구조 유지
                if (!review.imageUrl.isNullOrEmpty()) {
                    // 일단은 단일 이미지라도 리스트로 만들어 어댑터에 전달
                    val imageList = listOf(review.imageUrl)
                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE

                    val sliderAdapter = ReadImageSliderAdapter(imageList)
                    itemBinding.vpReviewPhotos.adapter = sliderAdapter

                    // 인디케이터 설정 (이미지가 2개 이상일 때만 필요하지만 구조는 유지)
                    if (imageList.size > 1) {
                        itemBinding.indicatorLayout.visibility = View.VISIBLE
                        setupIndicators(imageList.size, itemBinding.indicatorLayout)
                    } else {
                        itemBinding.indicatorLayout.visibility = View.GONE
                    }

                    itemBinding.vpReviewPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            if (imageList.size > 1) {
                                updateIndicators(position, itemBinding.indicatorLayout)
                            }
                        }
                    })
                } else {
                    itemBinding.vpReviewPhotos.visibility = View.GONE
                    itemBinding.indicatorLayout.visibility = View.GONE
                }

                itemBinding.btnDeleteReview.setOnClickListener {
                    onDeleteClick(review.reviewId)
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
            val binding = ItemMyReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
}