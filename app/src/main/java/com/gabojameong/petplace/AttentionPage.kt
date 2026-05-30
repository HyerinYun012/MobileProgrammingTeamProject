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

class AttentionPage : AppCompatActivity() {

    private lateinit var rvFavoriteList: RecyclerView
    private lateinit var adapter: FavoriteShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attentionpage)

        findViewById<ImageView>(R.id.imageView7).setOnClickListener {
            finish()
        }

        rvFavoriteList = findViewById(R.id.rv_favorite_list)
        rvFavoriteList.layoutManager = GridLayoutManager(this, 2)

        fetchFavoriteListFromServer()
    }

    private fun fetchFavoriteListFromServer() {
        val pageable = mapOf("page" to "0", "size" to "20")

        RetrofitClient.apiService.getBookmarks(pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<BookmarkResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<BookmarkResponse>>>,
                    response: Response<ApiResponse<PageResponse<BookmarkResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val bookmarkList = response.body()?.data?.content ?: emptyList()
                        val uiList = bookmarkList.map {
                            FavoriteUiModel(
                                restaurantId = it.restaurantId,
                                restaurantName = it.restaurantName,
                                address = it.address,
                                imageUrl = it.imageUrl,
                                isBookmarked = true
                            )
                        }.toMutableList()
                        setupAdapter(uiList)
                    } else {
                        Toast.makeText(this@AttentionPage, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<BookmarkResponse>>>, t: Throwable) {
                    Log.e("AttentionPage", "통신 실패: ${t.message}")
                    Toast.makeText(this@AttentionPage, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupAdapter(dataList: MutableList<FavoriteUiModel>) {
        adapter = FavoriteShopAdapter(
            shopList = dataList,
            onItemClick = { restaurantId ->
                startActivity(
                    Intent(this, PlaceInfoActivity::class.java)
                        .putExtra("restaurantId", restaurantId)
                )
            },
            onBookmarkClick = { restaurantId, _ ->
            RetrofitClient.apiService.toggleBookmark(restaurantId)
                .enqueue(object : Callback<ApiResponse<Boolean>> {
                    override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                        if (!response.isSuccessful || response.body()?.success != true) {
                            Toast.makeText(this@AttentionPage, "상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                        Toast.makeText(this@AttentionPage, "네트워크 오류 발생", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        )
        rvFavoriteList.adapter = adapter
    }
}
