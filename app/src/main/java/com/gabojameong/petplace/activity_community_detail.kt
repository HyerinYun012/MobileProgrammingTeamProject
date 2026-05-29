package com.gabojameong.petplace

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class activity_community_detail : AppCompatActivity() {

    private var postId: Long = -1L
    private var postAuthorId: Long = -1L
    private var currentUserId: Long = -1L
    // 현재 게시글의 이미지 URL 목록 — 수정 화면에 그대로 전달
    private var currentPostImageUrls: ArrayList<String> = arrayListOf()

    private lateinit var ivPostAuthorProfile: ImageView
    private lateinit var tvAuthor: TextView
    private lateinit var tvBadgeOwner: TextView
    private lateinit var tvBadgeCustomer: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var rvImages: RecyclerView
    private lateinit var rvComments: RecyclerView
    private lateinit var btnEditPost: TextView
    private lateinit var btnDeletePost: TextView
    private lateinit var btnReportPost: TextView
    private lateinit var etCommentInput: EditText
    private lateinit var btnCommentSubmit: Button

    private val comments = mutableListOf<CommentResponse>()
    private lateinit var commentAdapter: CommunityCommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_detail)

        postId = intent.getLongExtra("POST_ID", -1L)
        if (postId == -1L) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("userId", -1L)

        initViews()
        loadPostDetail()
        loadComments()
    }

    private fun initViews() {
        ivPostAuthorProfile = findViewById(R.id.iv_post_author_profile)
        tvAuthor = findViewById(R.id.tv_post_author)
        tvBadgeOwner = findViewById(R.id.tv_post_badge_owner)
        tvBadgeCustomer = findViewById(R.id.tv_post_badge_customer)
        tvDate = findViewById(R.id.tv_post_date)
        tvTitle = findViewById(R.id.tv_title)
        tvContent = findViewById(R.id.tv_content)
        rvImages = findViewById(R.id.rv_post_images)
        rvComments = findViewById(R.id.rv_comments)
        btnEditPost = findViewById(R.id.btn_edit_post)
        btnDeletePost = findViewById(R.id.btn_delete_post)
        btnReportPost = findViewById(R.id.btn_report_post)
        etCommentInput = findViewById(R.id.et_comment_input)
        btnCommentSubmit = findViewById(R.id.btn_comment_submit)

        commentAdapter = CommunityCommentAdapter(this, comments, postAuthorId) {}
        rvComments.apply {
            adapter = commentAdapter
            layoutManager = LinearLayoutManager(this@activity_community_detail)
            isNestedScrollingEnabled = false
        }

        findViewById<ImageView>(R.id.imageView7).setOnClickListener { finish() }
        btnCommentSubmit.setOnClickListener { submitComment() }

        // 수정 버튼 — 현재 이미지 URL 목록을 그대로 전달
        btnEditPost.setOnClickListener {
            val intent = Intent(this, activity_write_community::class.java).apply {
                putExtra("POST_ID", postId)
                putExtra("POST_TITLE", tvTitle.text.toString())
                putExtra("POST_CONTENT", tvContent.text.toString())
                putStringArrayListExtra("IMAGE_URLS", currentPostImageUrls)
            }
            startActivityForResult(intent, REQUEST_EDIT_POST)
        }

        btnDeletePost.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("이 게시글을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ -> deletePost() }
                .setNegativeButton("취소", null)
                .show()
        }

        btnReportPost.setOnClickListener {
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
                override fun onResponse(
                    call: Call<ApiResponse<PostDetailResponse>>,
                    response: Response<ApiResponse<PostDetailResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { post -> updatePostUI(post) }
                    } else {
                        Toast.makeText(
                            this@activity_community_detail,
                            "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PostDetailResponse>>, t: Throwable) {
                    Toast.makeText(
                        this@activity_community_detail,
                        "서버 연결 오류: ${t.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updatePostUI(post: PostDetailResponse) {
        postAuthorId = post.userId

        tvAuthor.text = post.writerNickname
        tvDate.text = formatDate(post.createdAt)

        Glide.with(this)
            .load(post.writerProfileUrl)
            .circleCrop()
            .placeholder(R.drawable.union)
            .error(R.drawable.union)
            .into(ivPostAuthorProfile)
        tvTitle.text = post.title
        tvContent.text = post.content

        when (post.writerRole) {
            "OWNER" -> { tvBadgeOwner.visibility = View.VISIBLE; tvBadgeCustomer.visibility = View.GONE }
            "CUSTOMER" -> { tvBadgeOwner.visibility = View.GONE; tvBadgeCustomer.visibility = View.VISIBLE }
            else -> { tvBadgeOwner.visibility = View.GONE; tvBadgeCustomer.visibility = View.GONE }
        }

        if (currentUserId != -1L && post.userId == currentUserId) {
            btnEditPost.visibility = View.VISIBLE
            btnDeletePost.visibility = View.VISIBLE
            btnReportPost.visibility = View.GONE
        } else {
            btnEditPost.visibility = View.GONE
            btnDeletePost.visibility = View.GONE
            btnReportPost.visibility = View.VISIBLE
        }

        // 이미지 목록 저장 및 표시 (다중 이미지 지원)
        val images = post.imageUrls?.filter { it.isNotBlank() } ?: emptyList()
        currentPostImageUrls = ArrayList(images)

        if (images.isNotEmpty()) {
            rvImages.visibility = View.VISIBLE
            rvImages.adapter = PostImageAdapter(images) { url ->
                val intent = Intent(this, ImageViewerActivity::class.java)
                intent.putExtra("IMAGE_URL", url)
                startActivity(intent)
            }
            rvImages.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false
            )
        } else {
            rvImages.visibility = View.GONE
        }

        commentAdapter = CommunityCommentAdapter(this, comments, postAuthorId) {}
        rvComments.adapter = commentAdapter
    }

    private fun loadComments() {
        val pageable = mapOf("page" to "0", "size" to "100", "sort" to "createdAt,asc")
        RetrofitClient.apiService.getPostComments(postId, pageable)
            .enqueue(object : Callback<ApiResponse<PageResponse<CommentResponse>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<CommentResponse>>>,
                    response: Response<ApiResponse<PageResponse<CommentResponse>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newComments = response.body()?.data?.content ?: emptyList()
                        comments.clear()
                        comments.addAll(newComments)
                        commentAdapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<CommentResponse>>>, t: Throwable) {}
            })
    }

    private fun submitComment() {
        val content = etCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.apiService.writeComment(postId, CommentRequest(null, content))
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        etCommentInput.setText("")
                        loadComments()
                    } else {
                        Toast.makeText(this@activity_community_detail, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@activity_community_detail, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deletePost() {
        RetrofitClient.apiService.deletePost(postId)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@activity_community_detail, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@activity_community_detail, "삭제 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@activity_community_detail, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun reportPost() {
        RetrofitClient.apiService.reportPost(postId, CommunityReportRequest("부적절한 게시글"))
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@activity_community_detail, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@activity_community_detail, "신고 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(this@activity_community_detail, "네트워크 오류", Toast.LENGTH_SHORT).show()
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

    private fun formatDate(dateStr: String): String {
        return try {
            if (dateStr.length >= 16) dateStr.substring(0, 16).replace("T", " ") else dateStr
        } catch (e: Exception) { dateStr }
    }

    companion object {
        private const val REQUEST_EDIT_POST = 1001
    }
}
