package com.gabojameong.petplace

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gabojameong.petplace.databinding.ActivityReviewWriteBinding
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

class ReviewWriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewWriteBinding
    private lateinit var photoAdapter: ReviewPhotoAdapter
    private val selectedPhotos = mutableListOf<Uri>()
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()
    private var restaurantId: Long = -1L

    // 사진 선택 런처 (현재 API는 단일 이미지이므로 1개만 선택)
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            selectedPhotos.clear()
            selectedPhotos.add(it)
            photoAdapter.setPhotos(selectedPhotos)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)

        binding.btnBack.setOnClickListener { finish() }

        // 사진 어댑터 설정
        photoAdapter = ReviewPhotoAdapter(
            onAddClick = {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeleteClick = { uri ->
                selectedPhotos.remove(uri)
                photoAdapter.setPhotos(selectedPhotos)
            }
        )

        binding.rvReviewPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvReviewPhotos.adapter = photoAdapter

        binding.btnSubmitReview.setOnClickListener {
            val rating = binding.ratingBar.rating
            val content = binding.etReviewContent.text.toString().trim()

            if (rating == 0f || content.isEmpty()) {
                showCustomDialog("별점과 리뷰 내용을 모두 작성해주세요!")
                return@setOnClickListener
            }

            if (restaurantId == -1L) {
                Toast.makeText(this, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(rating.toInt(), content)
        }
    }

    private fun submitReview(rating: Int, content: String) {
        // 1. JSON 데이터 파트 생성 (request)
        val reviewReq = ReviewRequest(rating = rating, content = content)
        val json = gson.toJson(reviewReq)
        val dataPart = json.toRequestBody("application/json".toMediaTypeOrNull())

        // 2. 이미지 파일 파트 생성 (Swagger의 'imageFile' 파트)
        val imagePart = if (selectedPhotos.isNotEmpty()) {
            val file = getFileFromUri(selectedPhotos[0])
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("imageFile", file.name, requestFile)
        } else null

        // 3. API 호출
        apiService.writeReview(restaurantId, dataPart, imagePart).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    showCustomDialog("리뷰가 등록되었습니다!") {
                        finish()
                    }
                } else {
                    val errorMsg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(this@ReviewWriteActivity, "등록 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(this@ReviewWriteActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_review_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
