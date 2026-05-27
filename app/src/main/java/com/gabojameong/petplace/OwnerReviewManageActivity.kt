package com.gabojameong.petplace

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gabojameong.petplace.databinding.ActivityOwnerReviewManageBinding
import com.gabojameong.petplace.databinding.DialogReportBinding
import com.gabojameong.petplace.databinding.ItemOwnerReviewBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerReviewManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerReviewManageBinding
    private val apiService = RetrofitClient.apiService
    private var restaurantId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerReviewManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트로부터 식당 ID 수신
        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadReviews()
    }

    private fun loadReviews() {
        if (restaurantId == -1L) {
            Toast.makeText(this, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getReviews(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<ReviewResponse>>>,
                response: Response<ApiResponse<PageResponse<ReviewResponse>>>
            ) {
                if (response.isSuccessful) {
                    val reviews = response.body()?.data?.content ?: emptyList()
                    binding.tvReviewCount.text = "${reviews.size}개"
                    binding.rvOwnerReviews.adapter = OwnerReviewAdapter(reviews) { reviewId ->
                        showReportDialog(reviewId)
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                Log.e("OwnerReview", "리뷰 로드 실패", t)
            }
        })
    }

    private fun showReportDialog(reviewId: Long) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogReportBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnReportConfirm.setOnClickListener {
            val reason = dialogBinding.etReportReason.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(this, "신고 사유를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            performReport(reviewId, reason)
        }

        dialog.show()
    }

    private fun performReport(reviewId: Long, reason: String) {
        val reasonMap = mapOf("reason" to reason)
        apiService.reportReview(reviewId, reasonMap).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    showCustomDialog("관리자에게 신고가 접수되었습니다.") {
                        loadReviews()
                    }
                } else {
                    Toast.makeText(applicationContext, "신고 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Log.e("OwnerReview", "신고 요청 네트워크 오류", t)
            }
        })
    }

    inner class OwnerReviewAdapter(
        private val reviews: List<ReviewResponse>,
        private val onReportClick: (Long) -> Unit
    ) : RecyclerView.Adapter<OwnerReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemOwnerReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewResponse) {
                itemBinding.tvName.text = review.writerName ?: "익명"
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content

                // 📸 다중 이미지 처리 (서버에서 준 imageUrl 사용)
                if (!review.imageUrl.isNullOrEmpty()) {
                    val imageList = listOf(review.imageUrl)
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
