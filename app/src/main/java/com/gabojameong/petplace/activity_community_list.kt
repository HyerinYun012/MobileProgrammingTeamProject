package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class activity_community_list : AppCompatActivity() {

    private lateinit var rvCommunityList: RecyclerView
    private lateinit var adapter: CommunityPostAdapter
    private val posts = mutableListOf<PostDetailResponse>()

    private val writeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadPosts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_list)

        rvCommunityList = findViewById(R.id.rv_community_list)

        adapter = CommunityPostAdapter(posts) { post ->
            val intent = Intent(this, activity_community_detail::class.java)
            intent.putExtra("POST_ID", post.id)
            startActivity(intent)
        }
        rvCommunityList.apply {
            adapter = this@activity_community_list.adapter
            layoutManager = LinearLayoutManager(this@activity_community_list)
        }

        val btnWrite = findViewById<ImageButton>(R.id.btn_write)
        val tvWriteLabel = findViewById<TextView>(R.id.tv_write_label)
        val writeAction = {
            writeLauncher.launch(Intent(this, activity_write_community::class.java))
        }
        btnWrite.setOnClickListener { writeAction() }
        tvWriteLabel.setOnClickListener { writeAction() }

        findViewById<ImageView>(R.id.imageView7).setOnClickListener {
            finish()
        }

        loadPosts()
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }

    private fun loadPosts() {
        val pageable = mapOf("page" to "0", "size" to "50", "sort" to "createdAt,desc")
        RetrofitClient.apiService.getCommunityPosts(pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<PostDetailResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<PostDetailResponse>>>,
                    response: Response<ApiResponse<PageResponse<PostDetailResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newPosts = response.body()?.data?.content ?: emptyList()
                        posts.clear()
                        posts.addAll(newPosts)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this@activity_community_list,
                            "게시글을 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<PageResponse<PostDetailResponse>>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@activity_community_list,
                        "서버 연결 오류: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
