package com.example.petplace // 본인 패키지명 꼭 확인!

import android.content.Intent
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
import com.example.petplace.databinding.ActivityReviewReadBinding
import com.example.petplace.databinding.ItemReviewReadBinding
import com.example.petplace.network.RetrofitClient
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.PageResponse
import com.example.petplace.network.ReviewResponse // 팀원분이 만들어둔 모델!
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewReadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewReadBinding

    // 🔥 이전 화면(식당 상세 화면)에서 넘겨받아야 할 식당 아이디! (임시로 1L 지정)
    private var targetRestaurantId: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 만약 식당 상세화면에서 intent.putExtra("RESTAURANT_ID", 식당번호) 로 넘겨준다면 여기서 받음!
        targetRestaurantId = intent.getLongExtra("RESTAURANT_ID", 1L)

        binding.btnBack.setOnClickListener { finish() }

        // 리뷰 쓰기 버튼 클릭 시
        binding.btnWriteReview.setOnClickListener {
            val intent = Intent(this, ReviewWriteActivity::class.java)
            // 쓰기 화면으로 넘어갈 때도 식당 아이디를 넘겨줘야 저장할 때 써먹을 수 있음!
            intent.putExtra("RESTAURANT_ID", targetRestaurantId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면 재활성화 시 최신 리뷰 데이터 서버에서 긁어오기!
        fetchRestaurantReviews()
    }

    // =========================================================================
    // 🌐 특정 식당의 리뷰 목록 서버에서 가져오기 (GET)
    // =========================================================================
    private fun fetchRestaurantReviews() {
        val pageableMap = mapOf(
            "page" to "0",
            "size" to "20",
            "sort" to "createdAt,DESC"
        )

        RetrofitClient.apiService.getReviews(targetRestaurantId, pageableMap)
            .enqueue(object : Callback<ApiResponse<PageResponse<ReviewResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<ReviewResponse>>>,
                    response: Response<ApiResponse<PageResponse<ReviewResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val reviewList = response.body()?.data?.content ?: emptyList()
                        // 어댑터 연결!
                        binding.rvReviews.adapter = ReviewAdapter(reviewList)
                    }  else {
                    Log.e("ReviewReadActivity", "리뷰 조회 실패! 에러코드: ${response.code()} / 이유: ${response.message()}")
                    Toast.makeText(this@ReviewReadActivity, "리뷰를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<ReviewResponse>>>, t: Throwable) {
                    Log.e("ReviewReadActivity", "통신 에러", t)
                    Toast.makeText(this@ReviewReadActivity, "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🎨 서버 데이터(ReviewResponse) 전용 리뷰 목록 어댑터
    // =========================================================================
    inner class ReviewAdapter(private val reviews: List<ReviewResponse>) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: ItemReviewReadBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(review: ReviewResponse) {
                // 텍스트, 평점, 작성자 이름 바인딩 (명세서 보니까 writerName 이 있네!)
                itemBinding.itemRatingBar.rating = review.rating.toFloat()
                itemBinding.tvReviewContent.text = review.content
                itemBinding.tvNickname.text = review.writerName

                // 📸 단일 이미지 처리 로직
                if (!review.imageUrl.isNullOrEmpty()) {
                    itemBinding.ivReviewPhoto.visibility = View.VISIBLE

                    // 1. 일단 서버에서 넘어온 주소가 정상인지부터 로그로 찍어봄!
                    Log.d("GlideDebug", "🎯 서버에서 받은 이미지 주소: ${review.imageUrl}")

                    // 2. Glide 청문회 시작
                    Glide.with(itemBinding.root.context)
                        .load(review.imageUrl)
                        .centerCrop()
                        .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            // 🔥 여기 안쪽을 텅 비우기!
                            override fun onLoadFailed(
                                p0: GlideException?,
                                p1: Any?,
                                p2: Target<Drawable?>,
                                p3: Boolean
                            ): Boolean {
                                Log.e("GlideDebug", "🚨 Glide 사진 로드 실패!! 원인: ${p0?.message}")
                                return false // 꼭 false 리턴!
                            }

                            override fun onResourceReady(
                                p0: Drawable,
                                p1: Any,
                                p2: Target<Drawable?>?,
                                p3: DataSource,
                                p4: Boolean
                            ): Boolean {
                                Log.d("GlideDebug", "✅ Glide 사진 띄우기 대성공!")
                                return false // 꼭 false 리턴!
                            }
                        })
                        .into(itemBinding.ivReviewPhoto)
                } else {
                    itemBinding.ivReviewPhoto.visibility = View.GONE
                    Log.d("GlideDebug", "⚠️ 이 리뷰는 사진이 없음 (imageUrl이 비어있음)")
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemReviewReadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(reviews[position])
        }

        override fun getItemCount() = reviews.size
    }
}