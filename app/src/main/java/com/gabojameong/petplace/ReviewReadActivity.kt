package com.gabojameong.petplace

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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
        binding.btnBack.setOnClickListener { finish() }

        binding.btnWriteReview.setOnClickListener {
            val intent = Intent(this, ReviewWriteActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurantId)
            }
            startActivity(intent)
        }
        
        loadReviews()
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
                // item_review_read.xml의 ID가 tv_reviewer_name인지 확인 필요
                // itemBinding.tvReviewerName.text = review.writerName ?: "익명"

                if (!review.imageUrl.isNullOrEmpty()) {
                    val imageList = listOf(review.imageUrl)
                    itemBinding.vpReviewPhotos.visibility = View.VISIBLE
                    itemBinding.indicatorLayout.visibility = View.VISIBLE
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
