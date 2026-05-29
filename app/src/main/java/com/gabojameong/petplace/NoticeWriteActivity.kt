package com.gabojameong.petplace

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.yalantis.ucrop.UCrop
import com.gabojameong.petplace.databinding.ActivityNoticeWriteBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*

class NoticeWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoticeWriteBinding
    private val apiService = RetrofitClient.apiService
    private var restaurantId: Long = 1L
    private var noticeId: Long = -1L

    private var bannerImageUri: Uri? = null
    private var originalImageUri: Uri? = null
    private var tempOriginalPickUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            tempOriginalPickUri = uri
            startCropForBanner(uri)
        }
    }

    private val bannerCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            bannerImageUri = UCrop.getOutput(result.data!!)
            tempOriginalPickUri?.let { startCropForOriginal(it) }
        }
    }

    private val originalCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            originalImageUri = UCrop.getOutput(result.data!!)
            Glide.with(this).load(bannerImageUri).into(binding.ivAttachedPhoto)
            binding.btnDeletePhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getLongExtra("RESTAURANT_ID", 1L)
        noticeId = intent.getLongExtra("NOTICE_ID", -1L)

        binding.btnBack.setOnClickListener { finish() }

        if (noticeId != -1L) {
            fetchNoticeDetail()
        }

        binding.ivAttachedPhoto.setOnClickListener {
            if (bannerImageUri == null) {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        binding.btnDeletePhoto.setOnClickListener {
            bannerImageUri = null
            originalImageUri = null
            binding.ivAttachedPhoto.setImageResource(R.drawable.ic_add_photo)
            binding.btnDeletePhoto.visibility = View.GONE
        }

        binding.btnSaveNotice.setOnClickListener {
            saveNotice()
        }
    }

    private fun fetchNoticeDetail() {
        val pageable = mapOf("page" to "0", "size" to "100")
        apiService.getNotices(restaurantId, pageable).enqueue(object : Callback<ApiResponse<PageResponse<NoticeResponse>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, response: Response<ApiResponse<PageResponse<NoticeResponse>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val notice = response.body()?.data?.content?.find { it.id == noticeId }
                    notice?.let {
                        binding.etNoticeTitle.setText(it.title)
                        binding.etNoticeContent.setText(it.content)
                        if (!it.thumbnailUrl.isNullOrEmpty()) {
                            bannerImageUri = Uri.parse(it.thumbnailUrl)
                            Glide.with(this@NoticeWriteActivity).load(bannerImageUri).into(binding.ivAttachedPhoto)
                            binding.btnDeletePhoto.visibility = View.VISIBLE
                        }
                        if (!it.imageUrl.isNullOrEmpty()) {
                            originalImageUri = Uri.parse(it.imageUrl)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<NoticeResponse>>>, t: Throwable) {
                Toast.makeText(this@NoticeWriteActivity, "공지 로드 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveNotice() {
        val title = binding.etNoticeTitle.text.toString().trim()
        val content = binding.etNoticeContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            this.showCustomDialog("제목과 내용을 모두 입력해주세요.")
            return
        }

        val thumbnailPart = bannerImageUri?.let { uri ->
            if (uri.toString().startsWith("content") || uri.toString().startsWith("file")) {
                val file = getFileFromUri(uri, "banner")
                MultipartBody.Part.createFormData("thumbnail", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            } else null
        }

        val originalPart = originalImageUri?.let { uri ->
            if (uri.toString().startsWith("content") || uri.toString().startsWith("file")) {
                val file = getFileFromUri(uri, "original")
                MultipartBody.Part.createFormData("descriptionImage", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            } else null
        }

        val callback = object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                if (response.isSuccessful) {
                    val msg = if (noticeId == -1L) "공지가 등록되었습니다." else "공지가 수정되었습니다."
                    showCustomDialog(msg) { finish() }
                } else {
                    Toast.makeText(this@NoticeWriteActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                Toast.makeText(this@NoticeWriteActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }

        if (noticeId == -1L) {
            apiService.registerNotice(restaurantId, title, content, thumbnailPart, originalPart).enqueue(callback)
        } else {
            val dataObj = TitleContentRequest(title, content)
            val json = Gson().toJson(dataObj)
            val dataBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            apiService.updateNotice(restaurantId, noticeId, dataBody, thumbnailPart, originalPart).enqueue(callback)
        }
    }

    private fun getFileFromUri(uri: Uri, prefix: String): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun startCropForBanner(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "banner_${UUID.randomUUID()}.jpg"))
        val options = UCrop.Options().apply {
            setToolbarTitle("목록 배너용으로 자르기")
            setStatusBarColor(Color.parseColor("#FFFFFF"))
            setToolbarColor(Color.parseColor("#FFFFFF"))
            setToolbarWidgetColor(Color.parseColor("#FF8A4C"))
            setActiveControlsWidgetColor(Color.parseColor("#FF8A4C"))
        }
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(3f, 1f)
            .withOptions(options)
            .getIntent(this)
        bannerCropLauncher.launch(uCropIntent)
    }

    private fun startCropForOriginal(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "original_${UUID.randomUUID()}.jpg"))
        val options = UCrop.Options().apply {
            setToolbarTitle("본문 상세용으로 자르기")
            setStatusBarColor(Color.parseColor("#FFFFFF"))
            setToolbarColor(Color.parseColor("#FFFFFF"))
            setToolbarWidgetColor(Color.parseColor("#FF8A4C"))
            setActiveControlsWidgetColor(Color.parseColor("#FF8A4C"))
        }
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .useSourceImageAspectRatio()
            .withOptions(options)
            .getIntent(this)
        originalCropLauncher.launch(uCropIntent)
    }
}
