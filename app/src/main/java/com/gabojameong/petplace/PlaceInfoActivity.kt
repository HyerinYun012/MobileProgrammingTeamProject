package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityPlaceInfoBinding
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

        restaurantId = intent.getLongExtra("restaurantId", -1L)
        restaurant = intent.getSerializableExtra("restaurant", RestaurantResponse::class.java)

        if (restaurant == null && restaurantId > 0) {
            // restaurantId만 전달된 경우 서버에서 데이터 로드 (관심목록, 최근 본 가게 등)
            fetchRestaurantById(restaurantId, savedInstanceState)
            return
        }
        if (restaurant == null) {
            Toast.makeText(applicationContext, "장소 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI(restaurant!!, savedInstanceState)
    }

    private fun fetchRestaurantById(id: Long, savedInstanceState: Bundle?) {
        apiService.getRestaurantDetail(id).enqueue(object : Callback<ApiResponse<RestaurantDetailResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<RestaurantDetailResponse>>,
                response: Response<ApiResponse<RestaurantDetailResponse>>
            ) {
                val detail = response.body()?.data
                if (response.isSuccessful && detail != null) {
                    // RestaurantDetailResponse → RestaurantResponse 변환 (필요한 필드만)
                    val stub = RestaurantResponse(
                        id = detail.id, name = detail.name, category = detail.category,
                        region = detail.region, address = detail.address,
                        latitude = detail.latitude, longitude = detail.longitude,
                        phone = detail.phone, operatingHours = detail.operatingHours,
                        allowSmall = detail.allowSmall, allowMedium = detail.allowMedium,
                        allowLarge = detail.allowLarge, hasFence = detail.hasFence,
                        hasArtificialGrass = detail.hasArtificialGrass,
                        hasNaturalGrass = detail.hasNaturalGrass, hasSnack = detail.hasSnack,
                        hasParking = detail.hasParking, hasRestroom = detail.hasRestroom,
                        hasIndoor = detail.hasIndoor, hasOutdoor = detail.hasOutdoor,
                        imageUrl = detail.imageUrl,
                        imageUrls = detail.imageUrls ?: detail.images?.map { it.imageUrl },
                        verified = detail.verified, bookmarked = detail.bookmarked
                    )
                    restaurant = stub
                    setupUI(stub, savedInstanceState)
                } else {
                    Toast.makeText(applicationContext, "장소 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<ApiResponse<RestaurantDetailResponse>>, t: Throwable) {
                Toast.makeText(applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupUI(r: RestaurantResponse, savedInstanceState: Bundle?) {
        restaurantId = r.id
        isBookmarked = r.bookmarked

        recordRecentShop(restaurantId)

        initUI(r)
        initFragmentNavigation()

        fetchLatestRestaurantDetail()

        binding.btnReturn.setOnClickListener { finish() }
        binding.btnBookmark.setOnClickListener { toggleBookmark() }

        if (savedInstanceState == null) {
            replaceFragment(PlaceInfoHomeFragment(), 0.06f)
        }
    }

    private fun initUI(restaurant: RestaurantResponse) {
        binding.textViewLocation.text = restaurant.name

        val imageUrl = restaurant.imageUrls?.firstOrNull() ?: restaurant.imageUrl
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
                        isBookmarked = detail.bookmarked
                        updateBookmarkUI()

                        restaurant = restaurant?.copy(bookmarked = isBookmarked)
                        setBookmarkResult()
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
                    restaurant = restaurant?.copy(bookmarked = isBookmarked)
                    setBookmarkResult()
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
        binding.btnBookmark.setBackgroundResource(
            if (isBookmarked) R.drawable.icon_heart else R.drawable.icon_heart_empty
        )
    }

    private fun recordRecentShop(id: Long) {
        if (id <= 0) return
        RetrofitClient.apiService.addRecentView(id).enqueue(object : retrofit2.Callback<ApiResponse<Any>> {
            override fun onResponse(call: retrofit2.Call<ApiResponse<Any>>, response: retrofit2.Response<ApiResponse<Any>>) {}
            override fun onFailure(call: retrofit2.Call<ApiResponse<Any>>, t: Throwable) {}
        })
    }

    private fun setBookmarkResult() {
        val data = Intent().apply {
            putExtra("restaurantId", restaurantId)
            putExtra("isBookmarked", isBookmarked)
        }
        setResult(RESULT_OK, data)
    }
}
