package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)
        if (restaurantId == -1L) {
            // Intent extra name might be different depending on where it's called from
            restaurantId = intent.getLongExtra("REVIEW_ID", -1L) // fallback if needed
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnWriteReview.setOnClickListener {
            val intent = Intent(this, ReviewWriteActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurantId)
            }
            startActivity(intent)
        }
        
        loadReviews()
    }

    override fun onResume() {
        super.onResume()
        loadReviews() // 새 리뷰 작성 후 돌아왔을 때 갱신
    }

    private fun loadReviews() {
        if (restaurantId == -1L) return
        val pageable = mapOf("page" to "0", "size" to "50")
        apiService.getReviews(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, response: Response<ApiResponse<PageResponse<ReviewResponse>>>) {
                if (response.isSuccessful) {
                    val reviews = response.body()?.data?.content ?: emptyList()
                    binding.rvReviews.adapter = ReviewAdapter(reviews)
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
                itemBinding.tvReviewerName.text = review.writerName ?: review.user?.nickname ?: "익명"

                // 프로필 이미지 설정
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

                // 리뷰 이미지 설정 (현재 API는 단일 이미지 지원)
                if (!review.imageUrl.isNullOrEmpty()) {
                    val imageList = listOf(review.imageUrl)
                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE
                    // itemBinding.indicatorLayout.visibility = View.VISIBLE // 이미지가 하나일 때는 인디케이터 숨김 처리 가능
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
