package com.gabojameong.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.FragmentPlaceInfoReviewBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceInfoReviewFragment : Fragment() {
    private var _binding: FragmentPlaceInfoReviewBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceInfoReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val restaurant = arguments?.getSerializable("restaurant", RestaurantResponse::class.java)
        
        restaurant?.let {
            fetchReviews(it.id)
            
            binding.btnNaverPage.setOnClickListener { _ ->
                val searchQuery = "${it.name} ${it.address}"
                val encodedQuery = Uri.encode(searchQuery)
                val url = "https://m.place.naver.com/place/list?query=$encodedQuery"
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }
    }

    private fun fetchReviews(restaurantId: Long) {
        // 7-7: 명세의 pageable (Object) 대응을 위한 QueryMap 구성
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "10"
        )

        apiService.getReviews(restaurantId, pageableMap).enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, response: Response<ApiResponse<PageResponse<ReviewResponse>>>) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    val reviews = apiResponse.data?.content ?: emptyList()
                    updateReviewUI(reviews)
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                Log.e("PlaceInfoReview", "Review load error: ${t.message}")
            }
        })
    }

    private fun updateReviewUI(reviews: List<ReviewResponse>) {
        if (reviews.isEmpty()) {
            binding.tvRate.text = "0.0 · "
            binding.tvRateCount.text = "0개 평점"
            binding.btnRecentReview.text = "아직 등록된 리뷰가 없습니다."
            binding.ivRecentReview.visibility = View.GONE
            return
        }

        // 1. 평점 및 개수 업데이트
        val avgRate = reviews.map { it.rating }.average()
        binding.tvRate.text = String.format("%.1f · ", avgRate)
        binding.tvRateCount.text = "${reviews.size}개 평점"

        // 2. 최신 리뷰 미리보기
        val recentReview = reviews[0]
        binding.btnRecentReview.text = getString(R.string.best_review_format, recentReview.writerName, recentReview.content)
        
        if (recentReview.imageUrl != null) {
            binding.ivRecentReview.visibility = View.VISIBLE
            Glide.with(this)
                .load(recentReview.imageUrl)
                .centerCrop()
                .into(binding.ivRecentReview)
        } else {
            binding.ivRecentReview.visibility = View.GONE
        }

        binding.btnRecentReview.setOnClickListener {
            Toast.makeText(requireContext().applicationContext, "리뷰 목록 화면으로 이동합니다 (구현 예정)", Toast.LENGTH_SHORT).show()
        }

        // 3. 사진 리뷰 목록 동적 생성
        binding.llImageReviews.removeAllViews()
        val imageReviews = reviews.filter { !it.imageUrl.isNullOrEmpty() }
        
        for (review in imageReviews) {
            val imageView = ImageView(requireContext().applicationContext).apply {
                val size = dpToPx(120)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 0, dpToPx(10), 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_round_gray)
                clipToOutline = true
            }
            
            Glide.with(this)
                .load(review.imageUrl)
                .centerCrop()
                .into(imageView)
            
            binding.llImageReviews.addView(imageView)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
