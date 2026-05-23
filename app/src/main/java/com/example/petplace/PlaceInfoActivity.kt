package com.example.petplace

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityPlaceInfoBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceInfoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPlaceInfoBinding.inflate(layoutInflater) }
    private val apiService = RetrofitClient.apiService
    private var isBookmarked = false
    private var restaurantId: Long = -1L
    private var restaurant: RestaurantResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        restaurant = intent.getSerializableExtra("restaurant", RestaurantResponse::class.java)

        if (restaurant == null) {
            Toast.makeText(applicationContext, "장소 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        restaurantId = restaurant!!.id
        isBookmarked = restaurant!!.isBookmarked
        
        initUI(restaurant!!)
        initFragmentNavigation()
        
        // 서버에서 최신 상세 정보를 가져와 북마크 상태 동기화
        fetchLatestRestaurantDetail()

        binding.btnReturn.setOnClickListener { finish() }
        binding.btnFavorite.setOnClickListener { toggleBookmark() }

        if (savedInstanceState == null) {
            replaceFragment(PlaceInfoHomeFragment(), 0.06f)
        }
    }

    private fun initUI(restaurant: RestaurantResponse) {
        binding.textViewLocation.text = restaurant.name

        val imageUrl = restaurant.imageUrl
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.mipmap.icon)
            .error(R.mipmap.icon)
            .centerCrop()
            .into(binding.imageViewMain)

        updateBookmarkUI()
    }

    private fun fetchLatestRestaurantDetail() {
        apiService.getRestaurantDetail(restaurantId).enqueue(object : Callback<ApiResponse<RestaurantDetailResponse>> {
            override fun onResponse(call: Call<ApiResponse<RestaurantDetailResponse>>, response: Response<ApiResponse<RestaurantDetailResponse>>) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { detail ->
                        isBookmarked = detail.isBookmarked
                        updateBookmarkUI()
                        // 로컬 객체 상태도 업데이트
                        restaurant = restaurant?.copy(isBookmarked = isBookmarked)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<RestaurantDetailResponse>>, t: Throwable) {}
        })
    }

    private fun initFragmentNavigation() {
        binding.btnHome.setOnClickListener { replaceFragment(PlaceInfoHomeFragment(), 0.06f) }
        binding.btnMenu.setOnClickListener { replaceFragment(PlaceInfoMenuFragment(), 0.355f) }
        binding.btnAnnouncement.setOnClickListener { replaceFragment(PlaceInfoAnnouncementFragment(), 0.645f) }
        binding.btnReview.setOnClickListener { replaceFragment(PlaceInfoReviewFragment(), 0.94f) }
    }

    private fun replaceFragment(fragment: Fragment, bias: Float) {
        val bundle = Bundle()
        bundle.putSerializable("restaurant", restaurant)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()

        val layoutParams = binding.verticalLineIndicator.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.horizontalBias = bias
        binding.verticalLineIndicator.layoutParams = layoutParams
    }

    private fun toggleBookmark() {
        apiService.toggleBookmark(restaurantId).enqueue(object : Callback<ApiResponse<Boolean>> {
            override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    isBookmarked = apiResponse.data ?: false
                    updateBookmarkUI()
                    restaurant = restaurant?.copy(isBookmarked = isBookmarked)
                    val message = if (isBookmarked) "북마크에 추가되었습니다." else "북마크가 취소되었습니다."
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                Toast.makeText(applicationContext, "연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateBookmarkUI() {
        binding.btnFavorite.setBackgroundResource(
            if (isBookmarked) R.drawable.icon_heart else R.drawable.icon_heart_empty
        )
    }
}
