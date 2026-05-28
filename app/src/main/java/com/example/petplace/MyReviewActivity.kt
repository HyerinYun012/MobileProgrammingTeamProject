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
import kotlin.jvm.java

class MyReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 켜질 때마다 서버에서 최신 데이터 불러오기
        fetchMyReviewsFromServer()
    }

    // =========================================================================
    // 🌐 서버에서 내 리뷰 목록 가져오기 (GET)
    // =========================================================================
    private fun fetchMyReviewsFromServer() {
        // 백엔드 명세서에 맞춘 페이징 조건 (0페이지부터 최신순으로 20개 가져오기)
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

                        // 상단에 총 리뷰 개수 반영
                        val totalCount = pageData?.totalElements ?: 0
                        binding.tvReviewCount.text = "${totalCount}개"

                        // 리사이클러뷰에 어댑터 연결!
                        binding.rvMyReviews.adapter = MyReviewAdapter(reviewList) { reviewId ->
                            // 삭제 버튼 눌렸을 때 실행될 로직
                            deleteReviewOnServer(reviewId)
                        }
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Log.e("MyReviewActivity", "조회 실패: $errorMsg")
                        Toast.makeText(this@MyReviewActivity, "리뷰를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<MyReviewResponse>>>, t: Throwable) {
                    Log.e("MyReviewActivity", "통신 에러", t)
                    Toast.makeText(this@MyReviewActivity, "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🗑️ 서버에서 특정 리뷰 영구 삭제하기 (DELETE)
    // =========================================================================
    private fun deleteReviewOnServer(reviewId: Long) {
        RetrofitClient.apiService.deleteReview(reviewId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@MyReviewActivity, "리뷰가 정상적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        // 삭제 성공 시, 화면 갱신을 위해 서버에서 목록을 다시 불러옴!
                        fetchMyReviewsFromServer()
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@MyReviewActivity, "삭제 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Log.e("MyReviewActivity", "삭제 중 통신 에러", t)
                    Toast.makeText(this@MyReviewActivity, "통신 에러로 삭제하지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    inner class MyReviewAdapter(
        private val reviews: List<MyReviewResponse>,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.Adapter<MyReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemMyReviewBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            // 🚨 수정 1: ReviewResponse -> MyReviewResponse 로 변경!
            fun bind(review: MyReviewResponse) {
                // 1️⃣ 동적으로 가게 이름 데이터 띄우기
                itemBinding.tvRestaurantName.text = review.restaurantName ?: "가게 정보 없음"

                // 기본 리뷰 데이터 세팅 (별점, 내용)
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content

                // 2️⃣ Glide 사진 세팅
                if (!review.imageUrl.isNullOrEmpty()) {
                    itemBinding.ivReviewPhoto.visibility = View.VISIBLE

                    Log.d("GlideDebug", "🎯 내 리뷰 이미지 주소: ${review.imageUrl}")

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
                                Log.e("GlideDebug", "🚨 내 리뷰 사진 로드 실패!! 원인: ${p0?.message}")
                                return false
                            }

                            override fun onResourceReady(
                                p0: Drawable,
                                p1: Any,
                                p2: Target<Drawable?>?,
                                p3: DataSource,
                                p4: Boolean
                            ): Boolean {
                                Log.d("GlideDebug", "✅ 내 리뷰 사진 띄우기 성공!")
                                return false
                            }

                        })
                        .into(itemBinding.ivReviewPhoto)
                } else {
                    itemBinding.ivReviewPhoto.visibility = View.GONE
                    Log.d("GlideDebug", "⚠️ 사진 없음 (imageUrl 데이터가 비어있음!)")
                }

                // 3️⃣ 식당 상세페이지로 이동하는 클릭 기능!
                itemBinding.layoutRestaurantLink.setOnClickListener {
                    // 💡 아직 액티비티가 없으니, 클릭이 잘 되는지 확인용 토스트를 띄웁니다!
                    Toast.makeText(itemBinding.root.context, "가게 상세 화면은 준비 중입니다!", Toast.LENGTH_SHORT).show()

                    /* 🚀 나중에 가게 상세 액티비티 만들면 이 주석을 풀고 연결해 주기!
                    val intent = Intent(itemBinding.root.context, RestaurantDetailActivity::class.java)
                    intent.putExtra("RESTAURANT_ID", review.restaurantId)
                    itemBinding.root.context.startActivity(intent)
                    */
                }

                // 4️⃣ 🔥 수정 2: 기존 ic_delete 버튼 클릭 시 진짜 삭제되도록 연결!
                itemBinding.btnDeleteReview.setOnClickListener {
                    // 어댑터 만들 때 뚫어놓은 통로(onDeleteClick)로 리뷰 아이디를 쏙 넘겨줌!
                    // 이러면 액티비티에 있는 deleteReviewOnServer 함수가 알아서 실행됨!
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