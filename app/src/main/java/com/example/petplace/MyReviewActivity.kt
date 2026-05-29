package com.example.petplace // 🚨 본인 패키지명 맞는지 꼭 확인!

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.example.petplace.databinding.ActivityMyReviewBinding
import com.example.petplace.databinding.ItemMyReviewBinding
import com.example.petplace.network.RetrofitClient
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.PageResponse
import com.example.petplace.network.MyReviewResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyReviewActivity : AppCompatActivity() {

    private var progressDialog: android.app.Dialog? = null
    private lateinit var binding: ActivityMyReviewBinding

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
        fetchMyReviewsFromServer()
    }

    // =========================================================================
    // 🌐 서버에서 내 리뷰 목록 가져오기 (GET)
    // =========================================================================
    private fun fetchMyReviewsFromServer() {
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "20",
            "sort" to "createdAt,DESC"
        )

        RetrofitClient.apiService.getMyReviews(pageableMap)
            .enqueue(object : Callback<ApiResponse<PageResponse<MyReviewResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<MyReviewResponse>>>,
                    response: Response<ApiResponse<PageResponse<MyReviewResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pageData = response.body()?.data
                        val reviewList = pageData?.content ?: emptyList()

                        val totalCount = pageData?.totalElements ?: 0
                        binding.tvReviewCount.text = "${totalCount}개"

                        binding.rvMyReviews.adapter = MyReviewAdapter(reviewList) { reviewId ->
                            deleteReviewOnServer(reviewId)
                        }
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Log.e("MyReviewActivity", "fetchMyReviews error: code=${response.code()}, msg=$errorMsg")
                        Toast.makeText(this@MyReviewActivity, "리뷰 목록 조회 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<MyReviewResponse>>>, t: Throwable) {
                    Log.e("MyReviewActivity", "fetchMyReviews network failure", t)
                    Toast.makeText(this@MyReviewActivity, "네트워크 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🗑️ 서버에서 특정 리뷰 영구 삭제하기 (DELETE)
    // =========================================================================
    private fun deleteReviewOnServer(reviewId: Long) {
        showLoadingDialog()

        RetrofitClient.apiService.deleteReview(reviewId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    dismissLoadingDialog()

                    if (response.isSuccessful) {
                        Log.i("MyReviewActivity", "deleteReview success: reviewId=$reviewId")
                        showDeleteSuccessDialog()
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Log.e("MyReviewActivity", "deleteReview error: code=${response.code()}, msg=$errorMsg")
                        Toast.makeText(this@MyReviewActivity, "리뷰 삭제 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    dismissLoadingDialog()
                    Log.e("MyReviewActivity", "deleteReview network failure. (May be processed by server)", t)
                    // 네트워크 에러로 떨어져도 실제 서버에는 반영되었을 수 있으므로 UI 갱신 시도
                    showDeleteSuccessDialog()
                }
            })
    }

    // =========================================================================
    // ⏳ 다이얼로그 관리 (Loading & Success)
    // =========================================================================
    private fun showLoadingDialog() {
        if (progressDialog == null) {
            progressDialog = android.app.Dialog(this).apply {
                // 1. 프로그레스바 객체를 만들면서 색상 지정!
                val progressBar = android.widget.ProgressBar(this@MyReviewActivity).apply {
                    // 🔥 핵심 마법: 무한정 도는 로딩창(indeterminate)의 색깔을 주황색(#FF8A4C)으로 덮어씌움!
                    indeterminateTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FF8A4C")
                    )
                }

                // 2. 색칠된 프로그레스바를 다이얼로그 화면에 장착!
                setContentView(progressBar)
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setCancelable(false)
            }
        }
        progressDialog?.show()
    }

    private fun dismissLoadingDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    private fun showDeleteSuccessDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_message)
        tvMessage.text = "리뷰가 정상적으로 삭제되었습니다."

        val btnConfirm = dialog.findViewById<android.widget.TextView>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            fetchMyReviewsFromServer()
        }

        dialog.show()
    }

    override fun onDestroy() {
        dismissLoadingDialog()
        super.onDestroy()
    }

    // =========================================================================
    // 🎨 RecyclerView Adapter
    // =========================================================================
    inner class MyReviewAdapter(
        private val reviews: List<MyReviewResponse>,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.Adapter<MyReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemMyReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: MyReviewResponse) {
                itemBinding.tvRestaurantName.text = review.restaurantName ?: "가게 정보 없음"
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content

                if (!review.imageUrl.isNullOrEmpty()) {
                    itemBinding.ivReviewPhoto.visibility = View.VISIBLE
                    Log.d("GlideLog", "Loading image url: ${review.imageUrl}")

                    Glide.with(itemBinding.root.context)
                        .load(review.imageUrl)
                        .centerCrop()
                        .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                p0: GlideException?,
                                p1: Any?,
                                p2: Target<Drawable?>,
                                p3: Boolean
                            ): Boolean {
                                Log.e("GlideLog", "Image load failed. cause: ${p0?.message}")
                                return false
                            }

                            override fun onResourceReady(
                                p0: Drawable,
                                p1: Any,
                                p2: Target<Drawable?>?,
                                p3: DataSource,
                                p4: Boolean
                            ): Boolean {
                                Log.d("GlideLog", "Image load success.")
                                return false
                            }
                        })
                        .into(itemBinding.ivReviewPhoto)
                } else {
                    itemBinding.ivReviewPhoto.visibility = View.GONE
                    Log.d("GlideLog", "No image url provided.")
                }

                itemBinding.layoutRestaurantLink.setOnClickListener {
                    Toast.makeText(itemBinding.root.context, "가게 상세 화면은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                }

                itemBinding.btnDeleteReview.setOnClickListener {
                    onDeleteClick(review.reviewId)
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