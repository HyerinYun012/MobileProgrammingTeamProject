package com.gabojameong.petplace

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class activity_write_community : AppCompatActivity() {

    /** 화면에 표시되는 이미지 목록 (기존 URL + 새로 선택한 URI 통합) */
    private val allImages = mutableListOf<PostImageItem>()
    private lateinit var imageAdapter: SelectedImageAdapter
    private lateinit var rvImages: RecyclerView
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnSave: Button

    private var editMode = false
    private var postId: Long = -1L

    private val pickMultipleImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { allImages.add(PostImageItem.New(it)) }
                imageAdapter.notifyDataSetChanged()
                updateRecyclerViewVisibility()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_community)

        postId   = intent.getLongExtra("POST_ID", -1L)
        editMode = postId != -1L

        val btnAddImage = findViewById<ImageButton>(R.id.btn_add_image)
        rvImages  = findViewById(R.id.rv_selected_images)
        etTitle   = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        btnSave   = findViewById(R.id.btn_save)

        // tv_existing_image_hint 는 수정 모드에서도 더 이상 사용하지 않음 (목록에 직접 표시)
        findViewById<android.widget.TextView>(R.id.tv_existing_image_hint)?.visibility = View.GONE

        if (editMode) {
            etTitle.setText(intent.getStringExtra("POST_TITLE") ?: "")
            etContent.setText(intent.getStringExtra("POST_CONTENT") ?: "")
            btnSave.text = "수정"

            // 기존 이미지 URL 목록을 allImages에 추가 → RecyclerView에 표시
            val existing = intent.getStringArrayListExtra("IMAGE_URLS") ?: emptyList<String>()
            existing.forEach { url -> allImages.add(PostImageItem.Existing(url)) }
        }

        imageAdapter = SelectedImageAdapter(allImages) { position ->
            allImages.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            imageAdapter.notifyItemRangeChanged(position, allImages.size)
            updateRecyclerViewVisibility()
        }

        rvImages.apply {
            adapter       = imageAdapter
            layoutManager = LinearLayoutManager(
                this@activity_write_community, LinearLayoutManager.HORIZONTAL, false
            )
        }
        updateRecyclerViewVisibility()

        btnAddImage.setOnClickListener { pickMultipleImages.launch("image/*") }
        findViewById<ImageView>(R.id.imageView7).setOnClickListener { finish() }
        btnSave.setOnClickListener { savePost() }
    }

    private fun savePost() {
        val title   = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // ── allImages 분리 ──────────────────────────────────────────
        // 유지할 기존 이미지 URL (사용자가 X 버튼으로 제거하지 않은 것)
        val keptExistingUrls: List<String> = allImages
            .filterIsInstance<PostImageItem.Existing>()
            .map { it.url }

        // 새로 추가된 이미지 URI
        val newUris: List<Uri> = allImages
            .filterIsInstance<PostImageItem.New>()
            .map { it.uri }
        // ────────────────────────────────────────────────────────────

        // data 파트 JSON — 수정 모드에서는 유지할 기존 URL 포함
        val dataMap: Map<String, Any?> = if (editMode) {
            mapOf("title" to title, "content" to content, "existingImageUrls" to keptExistingUrls)
        } else {
            mapOf("title" to title, "content" to content)
        }
        val dataBody = Gson().toJson(dataMap).toRequestBody("application/json".toMediaTypeOrNull())

        // 새 이미지 → MultipartBody.Part 변환
        val imageParts: List<MultipartBody.Part> = newUris.mapNotNull { uri ->
            val file = uriToFile(this, uri) ?: return@mapNotNull null
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images", file.name, requestFile)
        }

        val callback = object : Callback<ApiResponse<Any>> {
            override fun onResponse(
                call: Call<ApiResponse<Any>>,
                response: Response<ApiResponse<Any>>
            ) {
                if (response.isSuccessful) {
                    val msg = if (editMode) "글이 수정되었습니다." else "글이 등록되었습니다."
                    Toast.makeText(this@activity_write_community, msg, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = RetrofitClient.parseErrorMessage(response)
                    val op = if (editMode) "수정" else "등록"
                    Toast.makeText(
                        this@activity_write_community,
                        "${op} 실패: $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(
                    this@activity_write_community,
                    "네트워크 오류: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (editMode) {
            RetrofitClient.apiService.updatePost(postId, dataBody, imageParts).enqueue(callback)
        } else {
            RetrofitClient.apiService.writePost(dataBody, imageParts).enqueue(callback)
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "community_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out -> inputStream.use { it.copyTo(out) } }
            tempFile
        } catch (e: Exception) {
            Log.e("WriteCommunity", "File conversion failed", e)
            null
        }
    }

    private fun updateRecyclerViewVisibility() {
        rvImages.visibility = if (allImages.isEmpty()) View.GONE else View.VISIBLE
    }
}
