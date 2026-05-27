package com.gabojameong.petplace

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityStoreManageBinding
import com.google.android.material.chip.Chip
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class StoreManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreManageBinding
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    private var restaurantId: Long = -1L
    private var originalData: RestaurantDetailResponse? = null

    // 이미지 관리를 위한 변수
    private var thumbnailUri: Uri? = null
    private val bannerUriList = mutableListOf<Uri>() // 새로 추가된 이미지들
    private var editIndex: Int = -1

    // 영업시간 상태 관리를 위한 변수
    private var isDailyAdded = false
    private val addedWeeklyDays = mutableSetOf<String>()

    private val thumbnailLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            thumbnailUri = it
            Glide.with(this).load(it).centerCrop().into(binding.ivThumbnailAdd)
        }
    }

    private val bannerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            if (editIndex == -1) {
                bannerUriList.add(it)
                addBannerImageToLayout(it)
            } else {
                bannerUriList[editIndex] = it
                val targetImageView = binding.layoutBannerContainer.getChildAt(editIndex) as ImageView
                Glide.with(this).load(it).centerCrop().into(targetImageView)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restaurantId = intent.getLongExtra("RESTAURANT_ID", -1L)

        setupUI()
        if (restaurantId != -1L) {
            loadStoreDetail()
        }
    }

    private fun setupUI() {
        binding.ivThumbnailAdd.setOnClickListener {
            thumbnailLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivBannerAdd.setOnClickListener {
            editIndex = -1
            bannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnBack.setOnClickListener { finish() }

        setupSpinner()
        setupTimePicker()

        binding.ivSaveButton.setOnClickListener {
            saveStoreInfo()
        }
    }

    private fun loadStoreDetail() {
        apiService.getRestaurantDetail(restaurantId).enqueue(object : Callback<ApiResponse<RestaurantDetailResponse>> {
            override fun onResponse(call: Call<ApiResponse<RestaurantDetailResponse>>, response: Response<ApiResponse<RestaurantDetailResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    originalData = response.body()?.data
                    originalData?.let { bindData(it) }
                }
            }
            override fun onFailure(call: Call<ApiResponse<RestaurantDetailResponse>>, t: Throwable) {
                Log.e("StoreManage", "Failed to load store detail", t)
            }
        })
    }

    private fun bindData(data: RestaurantDetailResponse) {
        binding.etStoreName.setText(data.name)
        binding.etStoreAddress.setText(data.address)
        binding.etStorePhone.setText(data.phone)

        // 반려동물 및 시설 정보
        binding.cbSmallAnimal.isChecked = data.allowSmall
        binding.cbMediumAnimal.isChecked = data.allowMedium
        binding.cbLargeAnimal.isChecked = data.allowLarge
        binding.cbFence.isChecked = data.hasFence
        binding.cbArtificialGrass.isChecked = data.hasArtificialGrass
        binding.cbNaturalGrass.isChecked = data.hasNaturalGrass
        binding.cbSnack.isChecked = data.hasSnack
        binding.cbIndoor.isChecked = data.hasIndoor
        binding.cbOutdoor.isChecked = data.hasOutdoor

        // 썸네일 로드
        if (!data.imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(data.imageUrl).centerCrop().into(binding.ivThumbnailAdd)
        }

        // 배너 이미지 로드
        data.images?.forEach { img ->
            addBannerImageFromUrl(img.imageUrl)
        }

        // 영업시간 복원
        data.operatingHours?.let { restoreOperatingHours(it) }
    }

    private fun restoreOperatingHours(hours: List<OperatingHour>) {
        binding.cgSelectedTimes.removeAllViews()
        
        // 서버 데이터를 화면의 Chip 형태로 변환 (간소화된 구현)
        hours.forEach { hour ->
            val day = when(hour.dayOfWeek) {
                "MON" -> "월" "TUE" -> "화" "WED" -> "수" "THU" -> "목" "FRI" -> "금" "SAT" -> "토" "SUN" -> "일"
                else -> hour.dayOfWeek
            }
            val timeStr = if (hour.regularHoliday) "휴무" else "${hour.openTime} ~ ${hour.closeTime}"
            addChipToGroup("$day $timeStr", false, listOf(day))
            addedWeeklyDays.add(day)
        }
    }

    private fun saveStoreInfo() {
        val name = binding.etStoreName.text.toString().trim()
        val address = binding.etStoreAddress.text.toString().trim()
        val phone = binding.etStorePhone.text.toString().trim()

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "이름과 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 영업시간 데이터 생성
        val operatingHours = mutableListOf<OperatingHourRequest>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            val chipText = chip.text.toString()
            // Chip 텍스트 파싱 로직 (실제로는 더 정교해야 함)
            // 예: "월, 화 09:00 ~ 18:00" -> OperatingHourRequest 리스트로 변환
        }

        val request = RestaurantRequest(
            name = name,
            address = address,
            phone = phone,
            businessNo = originalData?.businessNo ?: "",
            category = originalData?.category ?: "CAFE",
            region = originalData?.region,
            latitude = originalData?.latitude,
            longitude = originalData?.longitude,
            hasFence = binding.cbFence.isChecked,
            hasArtificialGrass = binding.cbArtificialGrass.isChecked,
            hasNaturalGrass = binding.cbNaturalGrass.isChecked,
            hasSnack = binding.cbSnack.isChecked,
            hasIndoor = binding.cbIndoor.isChecked,
            hasOutdoor = binding.cbOutdoor.isChecked,
            allowSmall = binding.cbSmallAnimal.isChecked,
            allowMedium = binding.cbMediumAnimal.isChecked,
            allowLarge = binding.cbLargeAnimal.isChecked,
            operatingHours = emptyList() // TODO: 파싱된 데이터 넣기
        )

        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        
        // 이미지 파트 생성 (썸네일 + 배너)
        val imageParts = mutableListOf<MultipartBody.Part>()
        thumbnailUri?.let { uri ->
            val file = getFileFromUri(uri)
            val body = file.asRequestBody("image/*".toMediaTypeOrNull())
            imageParts.add(MultipartBody.Part.createFormData("images", file.name, body))
        }
        bannerUriList.forEach { uri ->
            val file = getFileFromUri(uri)
            val body = file.asRequestBody("image/*".toMediaTypeOrNull())
            imageParts.add(MultipartBody.Part.createFormData("images", file.name, body))
        }

        apiService.updateRestaurant(restaurantId, requestBody, if (imageParts.isEmpty()) null else imageParts)
            .enqueue(object : Callback<ApiResponse<Long>> {
                override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@StoreManageActivity, "정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                    Toast.makeText(this@StoreManageActivity, "수정 실패", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        return file
    }

    private fun addBannerImageFromUrl(url: String) {
        val newImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(100)).apply { marginEnd = dpToPx(10) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this).load(url).into(newImageView)
        val insertIndex = binding.layoutBannerContainer.childCount - 1
        binding.layoutBannerContainer.addView(newImageView, insertIndex)
    }

    // 기존 헬퍼 함수들 (addBannerImageToLayout, setupSpinner 등은 유지...)
    private fun setupSpinner() { /* 기존 코드 유지 */ }
    private fun setupTimePicker() { /* 기존 코드 유지 */ }
    private fun addChipToGroup(chipText: String, isDaily: Boolean, days: List<String>) {
        val chip = Chip(this).apply {
            text = chipText
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.white)
            setTextColor(Color.parseColor("#FF8A4C"))
            closeIconTint = ColorStateList.valueOf(Color.parseColor("#FF8A4C"))
            chipStrokeWidth = dpToPx(1).toFloat()
            chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#FF8A4C"))

            setOnCloseIconClickListener {
                binding.cgSelectedTimes.removeView(this)
                if (isDaily) isDailyAdded = false else addedWeeklyDays.removeAll(days.toSet())
            }
        }
        binding.cgSelectedTimes.addView(chip)
    }
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    private fun addBannerImageToLayout(uri: Uri) {
        val newImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(100)).apply { marginEnd = dpToPx(10) }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this).load(uri).into(newImageView)
        val insertIndex = binding.layoutBannerContainer.childCount - 1
        binding.layoutBannerContainer.addView(newImageView, insertIndex)
    }
}
