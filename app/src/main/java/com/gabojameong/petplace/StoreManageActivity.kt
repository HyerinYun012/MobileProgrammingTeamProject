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

    // 이미지 관리 변수
    private var thumbnailUri: Uri? = null
    private val bannerUriList = mutableListOf<Uri>()
    private var editIndex: Int = -1

    // 영업시간 상태 관리
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

        binding.cbSmallAnimal.isChecked = data.allowSmall
        binding.cbMediumAnimal.isChecked = data.allowMedium
        binding.cbLargeAnimal.isChecked = data.allowLarge
        binding.cbFence.isChecked = data.hasFence
        binding.cbArtificialGrass.isChecked = data.hasArtificialGrass
        binding.cbNaturalGrass.isChecked = data.hasNaturalGrass
        binding.cbSnack.isChecked = data.hasSnack
        binding.cbIndoor.isChecked = data.hasIndoor
        binding.cbOutdoor.isChecked = data.hasOutdoor

        if (!data.imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(data.imageUrl).centerCrop().into(binding.ivThumbnailAdd)
        }

        data.images?.forEach { img ->
            addBannerImageFromUrl(img.imageUrl)
        }

        data.operatingHours?.let { restoreOperatingHours(it) }
    }

    private fun restoreOperatingHours(hours: List<OperatingHour>) {
        binding.cgSelectedTimes.removeAllViews()
        addedWeeklyDays.clear()
        isDailyAdded = false

        hours.forEach { hour ->
            val dayKor = when (hour.dayOfWeek) {
                "MON" -> "월"
                "TUE" -> "화"
                "WED" -> "수"
                "THU" -> "목"
                "FRI" -> "금"
                "SAT" -> "토"
                "SUN" -> "일"
                else -> hour.dayOfWeek
            }
            val timeStr = if (hour.regularHoliday) "휴무" else "${hour.openTime} ~ ${hour.closeTime}"
            addChipToGroup("$dayKor $timeStr", false, listOf(dayKor))
            addedWeeklyDays.add(dayKor)
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

        // --- 영업시간 Chip 데이터 파싱 ---
        val operatingHours = mutableListOf<OperatingHourRequest>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            val text = chip.text.toString()

            val isHoliday = text.contains("휴무")
            val timePart = if (!isHoliday) text.substringAfterLast(" ") else null
            val openTime = if (!isHoliday) timePart?.split(" ~ ")?.get(0) else null
            val closeTime = if (!isHoliday) timePart?.split(" ~ ")?.get(1) else null

            val daysPart = text.substringBefore(if (isHoliday) " 휴무" else " $timePart")
            val days = if (daysPart == "매일") {
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            } else {
                daysPart.split(", ").map { dayToEng(it) }
            }

            days.forEach { day ->
                operatingHours.add(OperatingHourRequest(day, openTime, closeTime, isHoliday))
            }
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
            operatingHours = operatingHours
        )

        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())

        val imageParts = mutableListOf<MultipartBody.Part>()
        thumbnailUri?.let { uri ->
            val file = getFileFromUri(uri)
            imageParts.add(MultipartBody.Part.createFormData("images", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())))
        }
        bannerUriList.forEach { uri ->
            val file = getFileFromUri(uri)
            imageParts.add(MultipartBody.Part.createFormData("images", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())))
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

    private fun dayToEng(day: String): String = when (day) {
        "월" -> "MON"
        "화" -> "TUE"
        "수" -> "WED"
        "목" -> "THU"
        "금" -> "FRI"
        "토" -> "SAT"
        "일" -> "SUN"
        else -> day
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

    // --- 영업시간 설정 헬퍼 함수들 ---
    private fun setupSpinner() {
        val repeatItems = arrayOf("매일", "매주")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, repeatItems)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        binding.spinnerRepeat.adapter = adapter
        binding.spinnerRepeat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (repeatItems[position] == "매주") binding.layoutWeeklyDays.visibility = View.VISIBLE
                else binding.layoutWeeklyDays.visibility = View.GONE
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupTimePicker() {
        binding.tvStartTime.setOnClickListener {
            StoreTimeBottomSheet { time ->
                if (time == "휴무") { binding.tvStartTime.text = "휴무"; binding.tvEndTime.text = "휴무" }
                else binding.tvStartTime.text = time
                checkAndCreateTimeChip()
            }.show(supportFragmentManager, "start")
        }
        binding.tvEndTime.setOnClickListener {
            StoreTimeBottomSheet { time ->
                if (time == "휴무") { binding.tvStartTime.text = "휴무"; binding.tvEndTime.text = "휴무" }
                else binding.tvEndTime.text = time
                checkAndCreateTimeChip()
            }.show(supportFragmentManager, "end")
        }
    }

    private fun checkAndCreateTimeChip() {
        val start = binding.tvStartTime.text.toString()
        val end = binding.tvEndTime.text.toString()
        if (start == "입력" || end == "입력") return

        val isDaily = binding.spinnerRepeat.selectedItem.toString() == "매일"
        val timeStr = if (start == "휴무") "휴무" else "$start ~ $end"

        if (isDaily) {
            if (!isDailyAdded) {
                addChipToGroup("매일 $timeStr", true, emptyList())
                isDailyAdded = true
            }
        } else {
            val days = getSelectedDays()
            if (days.isNotEmpty()) {
                addChipToGroup("${days.joinToString(", ")} $timeStr", false, days)
                addedWeeklyDays.addAll(days)
            }
        }
        binding.tvStartTime.text = "입력"; binding.tvEndTime.text = "입력"
    }

    private fun getSelectedDays(): List<String> {
        val days = mutableListOf<String>()
        if (binding.cbMon.isChecked) days.add("월")
        if (binding.cbTue.isChecked) days.add("화")
        if (binding.cbWed.isChecked) days.add("수")
        if (binding.cbThu.isChecked) days.add("목")
        if (binding.cbFri.isChecked) days.add("금")
        if (binding.cbSat.isChecked) days.add("토")
        if (binding.cbSun.isChecked) days.add("일")
        return days
    }

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