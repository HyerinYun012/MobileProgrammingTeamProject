package com.gabojameong.petplace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityReviewReadBinding
import com.gabojameong.petplace.databinding.ItemReviewReadBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewReadBinding
    private val apiService = RetrofitClient.apiService
    private var restaurantId: Long = -1L
    private var reviewId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }

        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)
        reviewId = intent.getLongExtra("REVIEW_ID", -1L)

        if (reviewId != -1L) {
        } else if (restaurantId != -1L) {
            // 레스토랑 ID만 전달된 경우, 바로 리뷰 목록 로드
            loadReviews()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }



        binding.btnWriteReview.setOnClickListener {
            if (restaurantId != -1L) {
                val intent = Intent(this, ReviewWriteActivity::class.java).apply {
                    putExtra("RESTAURANT_ID", restaurantId)
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (restaurantId != -1L) {
            loadReviews()
        }
    }
/*
    private fun getReviewDetail() {
        if (reviewId == -1L) return

        apiService.getReviewDetail(reviewId).enqueue(object : Callback<ApiResponse<ReviewDetailResponse>> {
            override fun onResponse(call: Call<ApiResponse<ReviewDetailResponse>>, response: Response<ApiResponse<ReviewDetailResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val review = response.body()?.data
                    review?.restaurantId?.let { id ->
                        restaurantId = id
                        binding.btnWriteReview.visibility = View.VISIBLE
                        loadReviews()
                    }
                } else {
                    Log.e("ReviewRead", "Failed to get review detail")
                }
            }

            override fun onFailure(call: Call<ApiResponse<ReviewDetailResponse>>, t: Throwable) {
                Log.e("ReviewRead", "Error getting review detail", t)
            }
        })
    }
    */
    private fun loadReviews() {
        if (restaurantId == -1L) return
        
        val pageable = mapOf("page" to "0", "size" to "50")
        apiService.getReviews(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, response: Response<ApiResponse<PageResponse<ReviewResponse>>>) {
                if (response.isSuccessful) {
                    val reviews = response.body()?.data?.content ?: emptyList()
                    binding.rvReviews.adapter = ReviewAdapter(reviews)
                    
                    // 특정 리뷰 ID가 있는 경우 해당 위치로 스크롤 (선택 사항)
                    if (reviewId != -1L) {
                        val position = reviews.indexOfFirst { it.id == reviewId }
                        if (position != -1) {
                            binding.rvReviews.scrollToPosition(position)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                Log.e("ReviewRead", "Error loading reviews", t)
            }
        })
    }

    inner class ReviewAdapter(private val reviews: List<ReviewResponse>) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {
        inner class ViewHolder(private val itemBinding: ItemReviewReadBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(review: ReviewResponse) {
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content
                itemBinding.tvReviewerName.text = review.writerName ?: "익명"

                // 프로필 이미지 설정..인데 반환값에 user가 갑자기 또 없음..
                /*
                val profileUrl = review.user?.profileUrl
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(profileUrl)
                        .circleCrop()
                        .placeholder(R.drawable.icon_pfp1)
                        .into(itemBinding.ivReviewerProfile)
                } else {
                    itemBinding.ivReviewerProfile.setImageResource(R.drawable.icon_pfp1)
                }
                */
                // 리뷰 이미지 설정
                if (!review.imageUrl.isNullOrEmpty()) {
                    val imageList = listOf(review.imageUrl)
                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE
                    itemBinding.vpReviewPhotos.adapter = ReadImageSliderAdapter(imageList)
                } else {
                    itemBinding.vpReviewPhotos.visibility = View.GONE
                    itemBinding.indicatorLayout.visibility = View.GONE
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemReviewReadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(reviews[position])
        override fun getItemCount() = reviews.size
    }
}
