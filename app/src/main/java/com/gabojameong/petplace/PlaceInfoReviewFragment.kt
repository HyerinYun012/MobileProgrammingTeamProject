package com.gabojameong.petplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.gabojameong.petplace.databinding.FragmentPlaceInfoReviewBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceInfoReviewFragment : Fragment() {
    private var _binding: FragmentPlaceInfoReviewBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService
    private var currentRestaurantId: Long = -1L

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
            currentRestaurantId = it.id
            fetchReviews(it.id)
            
            binding.btnNaverPage.setOnClickListener { _ ->
                val searchQuery = "${it.name} ${it.address}"
                val encodedQuery = Uri.encode(searchQuery)
                val url = "https://m.place.naver.com/place/list?query=$encodedQuery"
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }

        binding.btnRecentReview.setOnClickListener {
            if (currentRestaurantId != -1L) {
                val intent = Intent(requireContext(), ReviewReadActivity::class.java).apply {
                    putExtra("RESTAURANT_ID", currentRestaurantId)
                }
                startActivity(intent)
            }
        }
    }

    private fun fetchReviews(restaurantId: Long) {
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
        binding.llRecentReviews.removeAllViews()

        if (reviews.isEmpty()) {
            binding.tvRate.text = "0.0 · "
            binding.tvRateCount.text = "0개 평점"
            binding.tvImageReviews.visibility = View.GONE
            binding.hsvImageReviews.visibility = View.GONE

            val emptyTv = TextView(requireContext()).apply {
                text = "아직 등록된 리뷰가 없습니다."
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(20))
            }
            binding.llRecentReviews.addView(emptyTv)
            return
        }

        val avgRate = reviews.map { it.rating }.average()
        binding.tvRate.text = String.format("%.1f · ", avgRate)
        binding.tvRateCount.text = "${reviews.size}개 평점"

        // 최근 리뷰 1개 → 프로필 사진 포함 카드로 표시
        reviews.take(1).forEach { review ->
            binding.llRecentReviews.addView(buildReviewCard(review))
        }

        // 사진 리뷰 가로 갤러리
        binding.llImageReviews.removeAllViews()
        val imageReviews = reviews.filter { !it.imageUrl.isNullOrEmpty() }

        if (imageReviews.isEmpty()) {
            binding.tvImageReviews.visibility = View.GONE
            binding.hsvImageReviews.visibility = View.GONE
        } else {
            binding.tvImageReviews.visibility = View.VISIBLE
            binding.hsvImageReviews.visibility = View.VISIBLE

            for (review in imageReviews) {
                val iv = ImageView(requireContext()).apply {
                    val size = dpToPx(120)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(0, 0, dpToPx(10), 0) }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                    setOnClickListener {
                        startActivity(Intent(requireContext(), ReviewReadActivity::class.java)
                            .putExtra("RESTAURANT_ID", currentRestaurantId))
                    }
                }
                Glide.with(this).load(review.imageUrl).centerCrop().into(iv)
                binding.llImageReviews.addView(iv)
            }
        }
    }

    /** 프로필 사진 + 닉네임 + 별점 + 내용을 담은 리뷰 카드 뷰를 코드로 생성 */
    private fun buildReviewCard(review: ReviewResponse): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(8)) }
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.bright_gray))
            setOnClickListener {
                startActivity(Intent(ctx, ReviewReadActivity::class.java)
                    .putExtra("RESTAURANT_ID", currentRestaurantId))
            }
        }

        // 프로필 원형 이미지
        val profileSize = dpToPx(40)
        val ivProfile = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(profileSize, profileSize).apply { setMargins(0, 0, dpToPx(12), 0) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this)
            .load(review.writerProfileUrl)
            .placeholder(R.drawable.icon_pfp1)
            .error(R.drawable.icon_pfp1)
            .transform(CircleCrop())
            .into(ivProfile)

        // 우측: 닉네임 + 별점 + 내용
        val right = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvName = TextView(ctx).apply {
            text = review.writerName ?: "익명"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
        }

        val ratingBar = RatingBar(ctx, null, android.R.attr.ratingBarStyleSmall).also { rb ->
            rb.numStars = 5
            rb.rating = review.rating.toFloat()
            rb.setIsIndicator(true)
            rb.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(2) }
            rb.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.orange)))
        }

        val tvContent = TextView(ctx).apply {
            text = review.content
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(4) }
        }

        right.addView(tvName)
        right.addView(ratingBar)
        right.addView(tvContent)

        card.addView(ivProfile)
        card.addView(right)
        return card
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
