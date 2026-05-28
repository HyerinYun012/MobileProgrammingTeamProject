package com.example.petplace // 🚨 본인 패키지명 확인

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityReviewWriteBinding
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.RetrofitClient
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

    private var selectedPhotoUri: Uri? = null
    private var targetRestaurantId: Long = 1L

    // 이미지 단일 선택 런처
    private val singleImagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedPhotoUri = uri

            binding.btnAddPhoto.visibility = View.GONE
            binding.layoutSelectedPhoto.visibility = View.VISIBLE

            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivSelectedPhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetRestaurantId = intent.getLongExtra("RESTAURANT_ID", 1L)

        binding.btnBack.setOnClickListener { finish() }

        // ➕ 1. 사진 추가 버튼 클릭 시
        binding.btnAddPhoto.setOnClickListener {
            openGallery()
        }

        // 🔄 2. 선택된 사진을 다시 눌렀을 때도 다른 사진으로 변경 가능하도록 연동
        binding.ivSelectedPhoto.setOnClickListener {
            openGallery()
        }

        // 🗑️ 3. X 텍스트 버튼 클릭 시 사진 삭제
        binding.btnDeleteSelectedPhoto.setOnClickListener {
            selectedPhotoUri = null
            binding.btnAddPhoto.visibility = View.VISIBLE
            binding.layoutSelectedPhoto.visibility = View.GONE
            binding.ivSelectedPhoto.setImageDrawable(null)
        }

        // 리뷰 등록 버튼 클릭 시
        binding.btnSubmitReview.setOnClickListener {
            // 📸 CCTV 1번: 버튼이 눌리긴 눌렸나?
            Log.d("WriteDebug", "🚨 [1] 등록 버튼 터치 감지 완료!!")

            val reviewText = binding.etReviewContent.text.toString().trim()
            // 📸 CCTV 2번: EditText에서 글자를 제대로 가져오고 있나?
            Log.d("WriteDebug", "🚨 [2] 현재 입력된 텍스트 길이: ${reviewText.length}, 내용: [$reviewText]")

            // 글자 수가 5자 미만인지 검사!
            if (reviewText.length < 5) {
                Log.d("WriteDebug", "🚨 [3] 5자 미만 컷 당함! 알림창 띄우기 직전!")

                androidx.appcompat.app.AlertDialog.Builder(this@ReviewWriteActivity)
                    .setTitle("안내")
                    .setMessage("리뷰는 최소 5자 이상 작성해주세요.")
                    .setPositiveButton("확인") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()

                return@setOnClickListener // 여기서 함수 강제 종료
            }

            // 📸 CCTV 3번: 검사 다 통과하고 서버로 넘어가기 직전인가?
            Log.d("WriteDebug", "🚨 [4] 글자 수 통과! 서버 통신 함수(postReviewToServer) 호출함!")
            val rating = binding.ratingBar.rating
            postReviewToServer(rating, reviewText)
        }
    }

    private fun openGallery() {
        singleImagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // =========================================================================
    // 🌐 서버로 리뷰 전송 (Multipart / POST)
    // =========================================================================
    private fun postReviewToServer(rating: Float, content: String) {

        // 🔥 1. 서버 명세서에 맞게 데이터 묶기 (Gson 맵 활용)
        val requestMap = mutableMapOf<String, Any>(
            "rating" to rating.toInt(), // 만약 서버가 소수점 원하면 그냥 rating 만 쓰기!
            "content" to content,
            // 🚨 백엔드 DTO에 식당 아이디가 아직 남아있을 수 있으니 일단 같이 쏴주자!
            "restaurantId" to targetRestaurantId
        )

        // 🔥 2. JSON 덩어리를 UTF-8 타입의 RequestBody로 변환! (한글 깨짐 방지)
        val jsonString = com.google.gson.Gson().toJson(requestMap)
        val dataBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // 3. 이미지 처리 (주원님 코드 완벽함. 그대로 유지!)
        var imagePart: MultipartBody.Part? = null
        selectedPhotoUri?.let { uri ->
            val file = getFileFromUri(this, uri)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                // 🚨 "imageFile" 이라는 이름표가 백엔드랑 100% 똑같은지 팀원한테 꼭 확인!!
                imagePart = MultipartBody.Part.createFormData("imageFile", file.name, requestFile)
            }
        }

        // 4. Retrofit 쏘기! (파라미터가 3개로 줄어듦!)
        RetrofitClient.apiService.writeReview(targetRestaurantId, dataBody, imagePart)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ReviewWriteActivity, "리뷰가 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // 🔥 500 에러의 단서를 쥐어짜내는 최후의 로그!
                        val errorBody = response.errorBody()?.string()
                        Log.e("WriteDebug", "🚨 서버 에러 코드: ${response.code()}")
                        Log.e("WriteDebug", "🚨 서버가 쥐고 쓰러진 쪽지(errorBody): $errorBody")

                        Toast.makeText(this@ReviewWriteActivity, "리뷰 등록 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Log.e("ReviewWriteActivity", "통신 에러", t)
                    Toast.makeText(this@ReviewWriteActivity, "네트워크 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            // 1. 원본 사진을 비트맵(도화지)으로 불러오기 (최신 방식)
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // 2. 임시 파일 껍데기 만들기
            val tempFile = File(context.cacheDir, "review_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            // 🔥 3. 핵심 마법! 비트맵을 JPEG 형식으로 쫙 압축해서 파일에 밀어 넣기!
            // 50이라는 숫자는 화질(퀄리티)을 의미함 (0 ~ 100).
            // 50~60 정도로 낮추면 화질은 크게 안 깨지면서 용량은 기가 막히게 줄어들어!
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)

            outputStream.flush()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            Log.e("ReviewWriteActivity", "파일 압축 변환 에러", e)
            null
        }
    }
}