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

class RecentShopActivity : AppCompatActivity() {

    private lateinit var rvRecentStores: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_shop)

        rvRecentStores = findViewById(R.id.rv_recent_stores)
        rvRecentStores.layoutManager = GridLayoutManager(this, 2)

        findViewById<ImageView>(R.id.imageView7).setOnClickListener {
            startActivity(Intent(this, CustomerMypageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }

        fetchRecentShops()
    }

    override fun onResume() {
        super.onResume()
        fetchRecentShops()
    }

    private fun fetchRecentShops() {
        val pageable = mapOf("page" to "0", "size" to "20")
        RetrofitClient.apiService.getRecentViews(pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<RecentViewResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<RecentViewResponse>>>,
                    response: Response<ApiResponse<PageResponse<RecentViewResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = response.body()?.data?.content ?: emptyList()
                        Log.d("RecentShop", "loaded: ${list.size}")
                        setupAdapter(list)
                    } else {
                        Toast.makeText(this@RecentShopActivity, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<RecentViewResponse>>>, t: Throwable) {
                    Log.e("RecentShop", "서버 통신 실패", t)
                    Toast.makeText(this@RecentShopActivity, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupAdapter(shopList: List<RecentViewResponse>) {
        val adapter = RecentShopAdapter(shopList) { shop ->
            startActivity(
                Intent(this, PlaceInfoActivity::class.java)
                    .putExtra("restaurantId", shop.restaurantId)
            )
        }
        rvRecentStores.adapter = adapter
    }
}
