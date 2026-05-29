package com.gabojameong.petplace

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityCommunityDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityDetailBinding

    private var postId: Long = -1L
    private var postAuthorId: Long = -1L
    private var currentUserId: Long = -1L
    private var currentPostImageUrls: ArrayList<String> = arrayListOf()

    private val comments = mutableListOf<CommentResponse>()
    private lateinit var commentAdapter: CommunityCommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getLongExtra("POST_ID", -1L)
        if (postId == -1L) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sharedPref = getSharedPreferences("PetPlacePref", android.content.Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("userId", -1L)

        setupViews()
        loadPostDetail()
        loadComments()
    }

    private fun setupViews() {
        commentAdapter = CommunityCommentAdapter(this, comments, postAuthorId) {}
        binding.rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(this@CommunityDetailActivity)
            isNestedScrollingEnabled = false
        }

        binding.imageView7.setOnClickListener { finish() }
        binding.btnCommentSubmit.setOnClickListener { submitComment() }

        binding.btnEditPost.setOnClickListener {
            val intent = Intent(this, CommunityWriteActivity::class.java).apply {
                putExtra("POST_ID", postId)
                putExtra("POST_TITLE", binding.tvTitle.text.toString())
                putExtra("POST_CONTENT", binding.tvContent.text.toString())
                putStringArrayListExtra("IMAGE_URLS", currentPostImageUrls)
            }
            startActivityForResult(intent, REQUEST_EDIT_POST)
        }

        binding.btnDeletePost.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("이 게시글을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ -> deletePost() }
                .setNegativeButton("취소", null)
                .show()
        }

        binding.btnReportPost.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("게시글 신고")
                .setMessage("이 게시글을 신고하시겠습니까?")
                .setPositiveButton("신고") { _, _ -> reportPost() }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun loadPostDetail() {
        RetrofitClient.apiService.getPostDetail(postId)
            .enqueue(object : Callback<ApiResponse<PostDetailResponse>> {
                override fun onResponse(call: Call<ApiResponse<PostDetailResponse>>, response: Response<ApiResponse<PostDetailResponse>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updatePostUI(it) }
                    } else {
                        Toast.makeText(this@CommunityDetailActivity, "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PostDetailResponse>>, t: Throwable) {
                    Toast.makeText(this@CommunityDetailActivity, "서버 연결 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updatePostUI(post: PostDetailResponse) {
        postAuthorId = post.userId

        binding.tvPostAuthor.text = post.writerNickname
        binding.tvPostDate.text = formatDate(post.createdAt)
        binding.tvTitle.text = post.title
        binding.tvContent.text = post.content

        Glide.with(this)
            .load(post.writerProfileUrl)
            .circleCrop()
            .placeholder(R.drawable.union)
            .error(R.drawable.union)
            .into(binding.ivPostAuthorProfile)

        when (post.writerRole) {
            "OWNER" -> { binding.tvPostBadgeOwner.visibility = View.VISIBLE; binding.tvPostBadgeCustomer.visibility = View.GONE }
            "CUSTOMER" -> { binding.tvPostBadgeOwner.visibility = View.GONE; binding.tvPostBadgeCustomer.visibility = View.VISIBLE }
            else -> { binding.tvPostBadgeOwner.visibility = View.GONE; binding.tvPostBadgeCustomer.visibility = View.GONE }
        }

        if (currentUserId != -1L && post.userId == currentUserId) {
            binding.btnEditPost.visibility = View.VISIBLE
            binding.btnDeletePost.visibility = View.VISIBLE
            binding.btnReportPost.visibility = View.GONE
        } else {
            binding.btnEditPost.visibility = View.GONE
            binding.btnDeletePost.visibility = View.GONE
            binding.btnReportPost.visibility = View.VISIBLE
        }

        val images = post.imageUrls?.filter { it.isNotBlank() } ?: emptyList()
        currentPostImageUrls = ArrayList(images)

        if (images.isNotEmpty()) {
            binding.rvPostImages.visibility = View.VISIBLE
            binding.rvPostImages.adapter = PostImageAdapter(images) { url ->
                startActivity(Intent(this, ImageViewerActivity::class.java).putExtra("IMAGE_URL", url))
            }
            binding.rvPostImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        } else {
            binding.rvPostImages.visibility = View.GONE
        }

        commentAdapter = CommunityCommentAdapter(this, comments, postAuthorId) {}
        binding.rvComments.adapter = commentAdapter
    }

    private fun loadComments() {
        val pageable = mapOf("page" to "0", "size" to "100", "sort" to "createdAt,asc")
        RetrofitClient.apiService.getPostComments(postId, pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<CommentResponse>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<CommentResponse>>>, response: Response<ApiResponse<PageResponse<CommentResponse>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        comments.clear()
                        comments.addAll(response.body()?.data?.content ?: emptyList())
                        commentAdapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<CommentResponse>>>, t: Throwable) {}
            })
    }

    private fun submitComment() {
        val content = binding.etCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.apiService.writeComment(postId, CommentRequest(null, content))
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        binding.etCommentInput.setText("")
                        loadComments()
                    } else {
                        Toast.makeText(this@CommunityDetailActivity, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@CommunityDetailActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deletePost() {
        RetrofitClient.apiService.deletePost(postId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CommunityDetailActivity, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CommunityDetailActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@CommunityDetailActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun reportPost() {
        RetrofitClient.apiService.reportPost(postId, CommunityReportBody("부적절한 게시글"))
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    Toast.makeText(this@CommunityDetailActivity,
                        if (response.isSuccessful) "신고가 접수되었습니다." else "신고 실패",
                        Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@CommunityDetailActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_POST && resultCode == RESULT_OK) {
            loadPostDetail()
        }
    }

    private fun formatDate(dateStr: String) = try {
        if (dateStr.length >= 16) dateStr.substring(0, 16).replace("T", " ") else dateStr
    } catch (e: Exception) { dateStr }

    companion object {
        private const val REQUEST_EDIT_POST = 1001
    }
}
