package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gabojameong.petplace.databinding.ActivityCommunityListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityListBinding
    private lateinit var adapter: CommunityPostAdapter
    private val posts = mutableListOf<PostDetailResponse>()

    private val writeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadPosts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CommunityPostAdapter(posts) { post ->
            val intent = Intent(this, CommunityDetailActivity::class.java)
            intent.putExtra("POST_ID", post.id)
            startActivity(intent)
        }
        binding.rvCommunityList.apply {
            adapter = this@CommunityListActivity.adapter
            layoutManager = LinearLayoutManager(this@CommunityListActivity)
        }

        binding.imageView7.setOnClickListener { finish() }
        binding.btnWrite.setOnClickListener {
            writeLauncher.launch(Intent(this, CommunityWriteActivity::class.java))
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
                        Toast.makeText(this@CommunityListActivity, "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<PostDetailResponse>>>, t: Throwable) {
                    Toast.makeText(this@CommunityListActivity, "서버 연결 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
