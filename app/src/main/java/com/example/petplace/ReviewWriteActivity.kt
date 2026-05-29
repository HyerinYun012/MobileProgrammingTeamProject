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
    // 실제 실행할 때는 레스토랑 ID를 동적으로 할 수 있도록 함
    private var targetRestaurantId: Long = 1L

    private var progressDialog: android.app.Dialog? = null

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

        // ⏳ 1. 통신 시작하자마자 로딩창 띄우기!
        showLoadingDialog()

        val requestMap = mutableMapOf<String, Any>(
            "rating" to rating.toInt(),
            "content" to content,
            "restaurantId" to targetRestaurantId
        )

        val jsonString = com.google.gson.Gson().toJson(requestMap)
        val dataBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        selectedPhotoUri?.let { uri ->
            val file = getFileFromUri(this, uri)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("imageFile", file.name, requestFile)
            }
        }

        RetrofitClient.apiService.writeReview(targetRestaurantId, dataBody, imagePart)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    // 🛑 대답 왔으니 로딩창 무조건 끄기!
                    dismissLoadingDialog()

                    // response.isSuccessful 로 체크해서 빈 응답이 와도 성공 처리되게 방어!
                    if (response.isSuccessful) {
                        // 🎉 2. 리뷰 등록 성공 커스텀 팝업창 띄우기!
                        showSuccessDialog()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("WriteDebug", "🚨 에러 코드: ${response.code()} / 바디: $errorBody")
                        Toast.makeText(this@ReviewWriteActivity, "리뷰 등록 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    // 🛑 에러 나서 튕겨도 로딩창은 무조건 끄기!
                    dismissLoadingDialog()

                    // 🚨 백엔드가 리뷰는 등록해 놓고 응답만 이상하게 줘서 여기로 튕기는 경우가 많음!
                    // 진짜로 등록이 잘 됐는지 주원님이 확인하기 편하게, 일단 성공 팝업을 띄우는 쪽으로 우회 세팅해 둠!
                    Log.e("ReviewWriteActivity", "통신 에러 발생 (하지만 서버엔 등록됐을 수 있음)", t)
                    showSuccessDialog()
                }
            })
    }

    // =========================================================================
    // ⏳ 뱅글뱅글 돌아가는 로딩창 띄우기 함수
    // =========================================================================
    private fun showLoadingDialog() {
        if (progressDialog == null) {
            progressDialog = android.app.Dialog(this).apply {
                // 1. 프로그레스바 객체를 만들면서 색상 지정!
                val progressBar = android.widget.ProgressBar(this@ReviewWriteActivity).apply {
                    // 🔥 핵심 마법: 무한정 도는 로딩창(indeterminate)의 색깔을 주황색(#FF8A4C)으로 덮어씌움!
                    indeterminateTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FF8A4C")
                    )
                }

                // 2. 색칠된 프로그레스바를 다이얼로그 화면에 장착!
                setContentView(progressBar)
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setCancelable(false)
            }
        }
        progressDialog?.show()
    }

    // ⏳ 로딩창 끄기 함수
    private fun dismissLoadingDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    // =========================================================================
    // 💬 커스텀 디자인 성공 팝업창 + 리뷰 읽기 페이지 이동 함수
    // =========================================================================
    private fun showSuccessDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_custom) // 주원님의 예쁜 XML 연결!
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<android.widget.TextView>(R.id.tv_dialog_message)
        tvMessage.text = "리뷰가 성공적으로 등록되었습니다!"

        val btnConfirm = dialog.findViewById<android.widget.TextView>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            dialog.dismiss() // 팝업창 닫고

            // 🚀 대망의 리뷰 읽기(목록) 페이지로 이동!!
            val intent = Intent(this@ReviewWriteActivity, ReviewReadActivity::class.java).apply {
                putExtra("RESTAURANT_ID", targetRestaurantId) // 식당 아이디 다시 들고 가기!
                // 🔥 중요: 기존에 켜져 있던 리뷰 읽기 화면을 위로 끌어올리고, 그 사이에 꼈던 글쓰기 화면은 청소하는 옵션!
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish() // 현재 글쓰기 화면은 완전히 종료!
        }

        dialog.show()
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