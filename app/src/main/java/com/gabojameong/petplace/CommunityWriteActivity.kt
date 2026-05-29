package com.gabojameong.petplace

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gabojameong.petplace.databinding.ActivityWriteCommunityBinding
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

class CommunityWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWriteCommunityBinding

    private val allImages = mutableListOf<PostImageItem>()
    private lateinit var imageAdapter: SelectedImageAdapter

    private var editMode = false
    private var postId: Long = -1L

    private val pickMultipleImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { allImages.add(PostImageItem.New(it)) }
                imageAdapter.notifyDataSetChanged()
                updateImageVisibility()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId   = intent.getLongExtra("POST_ID", -1L)
        editMode = postId != -1L

        binding.tvExistingImageHint.visibility = View.GONE

        if (editMode) {
            binding.etTitle.setText(intent.getStringExtra("POST_TITLE") ?: "")
            binding.etContent.setText(intent.getStringExtra("POST_CONTENT") ?: "")
            binding.btnSave.text = "수정"
            val existing = intent.getStringArrayListExtra("IMAGE_URLS") ?: emptyList<String>()
            existing.forEach { url -> allImages.add(PostImageItem.Existing(url)) }
        }

        imageAdapter = SelectedImageAdapter(allImages) { position ->
            allImages.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            imageAdapter.notifyItemRangeChanged(position, allImages.size)
            updateImageVisibility()
        }

        binding.rvSelectedImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(this@CommunityWriteActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        updateImageVisibility()

        binding.btnAddImage.setOnClickListener { pickMultipleImages.launch("image/*") }
        binding.imageView7.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { savePost() }
    }

    private fun savePost() {
        val title   = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty()) { Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show(); return }
        if (content.isEmpty()) { Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show(); return }

        val keptUrls = allImages.filterIsInstance<PostImageItem.Existing>().map { it.url }
        val newUris  = allImages.filterIsInstance<PostImageItem.New>().map { it.uri }

        val dataMap: Map<String, Any?> = if (editMode) {
            mapOf("title" to title, "content" to content, "existingImageUrls" to keptUrls)
        } else {
            mapOf("title" to title, "content" to content)
        }
        val dataBody = Gson().toJson(dataMap).toRequestBody("application/json".toMediaTypeOrNull())

        val imageParts: List<MultipartBody.Part> = newUris.mapNotNull { uri ->
            val file = uriToFile(this, uri) ?: return@mapNotNull null
            MultipartBody.Part.createFormData("images", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
        }

        val callback = object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CommunityWriteActivity,
                        if (editMode) "글이 수정되었습니다." else "글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(this@CommunityWriteActivity,
                        "${if (editMode) "수정" else "등록"} 실패: $msg", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(this@CommunityWriteActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
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
            Log.e("CommunityWrite", "File conversion failed", e)
            null
        }
    }

    private fun updateImageVisibility() {
        binding.rvSelectedImages.visibility = if (allImages.isEmpty()) View.GONE else View.VISIBLE
    }
}
