package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class activity_recent_shop : AppCompatActivity() {

    private lateinit var rvRecentStores: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_shop)

        rvRecentStores = findViewById(R.id.rv_recent_stores)
        val btnBack = findViewById<ImageView>(R.id.imageView7)

        rvRecentStores.layoutManager = GridLayoutManager(this, 2)

        btnBack.setOnClickListener {
            val intent = Intent(this, customer_mypage::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)
            finish()
        }

        fetchRecentShopsFromServer()
    }

    override fun onResume() {
        super.onResume()
        fetchRecentShopsFromServer()
    }

    private fun fetchRecentShopsFromServer() {
        val pageable = mapOf("page" to "0", "size" to "20")

        RetrofitClient.apiService.getRecentViews(pageable).enqueue(object : Callback<ApiResponse<PageResponse<RecentViewResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<RecentViewResponse>>>,
                response: Response<ApiResponse<PageResponse<RecentViewResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val recentShops = response.body()?.data?.content ?: emptyList()
                    Log.d("activity_recent_shop", "Recent shops loaded: ${recentShops.size}")
                    setupAdapter(recentShops)
                } else {
                    Log.e("API_ERROR", "응답 에러: ${response.code()} / ${response.message()}")
                    Toast.makeText(this@activity_recent_shop, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<RecentViewResponse>>>, t: Throwable) {
                Log.e("API_ERROR", "서버 통신 실패", t)
                Toast.makeText(this@activity_recent_shop, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAdapter(shopList: List<RecentViewResponse>) {
        val adapter = RecentShopAdapter(shopList) { shop ->
            val intent = Intent(this, PlaceInfoActivity::class.java)

            val restaurantStub = RestaurantResponse(
                id = shop.restaurantId,
                name = shop.restaurantName,
                category = shop.category,
                imageUrl = shop.imageUrl,
                region = "",
                address = "",
                latitude = 0.0,
                longitude = 0.0,
                phone = "",
                operatingHours = null,
                allowSmall = true,
                allowMedium = false,
                allowLarge = false,
                hasFence = false,
                hasArtificialGrass = false,
                hasNaturalGrass = false,
                hasSnack = false,
                hasParking = false,
                hasRestroom = false,
                hasIndoor = false,
                hasOutdoor = false,
                verified = false,
                bookmarked = false
            )

            intent.putExtra("restaurant", restaurantStub)
            startActivity(intent)
        }
        rvRecentStores.adapter = adapter
    }
}