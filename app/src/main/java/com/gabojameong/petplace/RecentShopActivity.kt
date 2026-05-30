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

        fetchRecentShopsWithBookmarks()
    }

    override fun onResume() {
        super.onResume()
        fetchRecentShopsWithBookmarks()
    }

    // 최근 본 가게 + 북마크 목록을 함께 로드해서 북마크 여부 교차 확인
    private fun fetchRecentShopsWithBookmarks() {
        val pageable = mapOf("page" to "0", "size" to "100")

        // 북마크 목록을 먼저 로드
        RetrofitClient.apiService.getBookmarks(pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<BookmarkResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<BookmarkResponse>>>,
                    response: Response<ApiResponse<PageResponse<BookmarkResponse>>>
                ) {
                    val bookmarkedIds = if (response.isSuccessful && response.body()?.success == true) {
                        (response.body()?.data?.content ?: emptyList())
                            .map { it.restaurantId }.toSet()
                    } else emptySet()

                    // 북마크 목록을 가져온 후 최근 본 가게 로드
                    fetchRecentShops(bookmarkedIds)
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<BookmarkResponse>>>, t: Throwable) {
                    // 북마크 로드 실패해도 최근 본 가게는 표시 (북마크 상태 모름)
                    fetchRecentShops(emptySet())
                }
            })
    }

    private fun fetchRecentShops(bookmarkedIds: Set<Long>) {
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
                        setupAdapter(list, bookmarkedIds)
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

    private fun setupAdapter(shopList: List<RecentViewResponse>, bookmarkedIds: Set<Long>) {
        val adapter = RecentShopAdapter(
            shopList = shopList,
            bookmarkedIds = bookmarkedIds.toMutableSet(),
            onItemClick = { shop ->
                startActivity(
                    Intent(this, PlaceInfoActivity::class.java)
                        .putExtra("restaurantId", shop.restaurantId)
                )
            },
            onBookmarkClick = { restaurantId, callback ->
                RetrofitClient.apiService.toggleBookmark(restaurantId)
                    .enqueue(object : Callback<ApiResponse<Boolean>> {
                        override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                            val isNowBookmarked = response.body()?.data ?: false
                            if (response.isSuccessful) callback(isNowBookmarked)
                        }
                        override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                            Toast.makeText(this@RecentShopActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        )
        rvRecentStores.adapter = adapter
    }
}
