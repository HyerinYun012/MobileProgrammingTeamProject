package com.gabojameong.petplace

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.Locale
import com.bumptech.glide.Glide
import com.gabojameong.petplace.databinding.ActivityStoreManageBinding
import com.google.android.material.chip.Chip
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class StoreManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreManageBinding
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    private var restaurantId: Long = -1L
    private var originalData: RestaurantDetailResponse? = null
    private val myRestaurants = mutableListOf<OwnerRestaurantSummary>()

    // 주소 검색 결과
    private var finalLatitude: Double = 0.0
    private var finalLongitude: Double = 0.0
    private var finalRegionCode: String = "ETC"

    private val addressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fullAddress = result.data?.getStringExtra("address") ?: ""
            val dongName    = result.data?.getStringExtra("dong") ?: ""
            val lat         = result.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lng         = result.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            binding.etStoreAddress.setText(fullAddress)
            // 카카오 API가 반환한 좌표 직접 사용 (Geocoder 불필요)
            if (lat != 0.0 && lng != 0.0) {
                finalLatitude  = lat
                finalLongitude = lng
            } else {
                // 좌표 없으면 Geocoder 폴백
                convertAddressToCoordinates(fullAddress)
            }
            finalRegionCode = convertDongToRegionCode(dongName)
        }
    }

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
        fetchMyRestaurants()
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
        setupCategorySpinner()
        setupTimePicker()

        binding.ivSaveButton.setOnClickListener { saveStoreInfo() }

        binding.btnSearchAddress.setOnClickListener {
            addressLauncher.launch(Intent(this, AddressSearchActivity::class.java))
        }

        // 사업자등록번호 자동 포맷팅 (xxx-xx-xxxxx)
        binding.etBusinessNo.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val digits = s.toString().replace("-", "").filter { it.isDigit() }.take(10)
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    formatted.append(digits[i])
                    if (i == 2 && digits.length > 3) formatted.append("-")
                    else if (i == 4 && digits.length > 5) formatted.append("-")
                }
                val finalStr = formatted.toString()
                if (s.toString() != finalStr) {
                    s.replace(0, s.length, finalStr)
                    binding.etBusinessNo.setSelection(s.length)
                }
                isFormatting = false
            }
        })
    }

    private fun convertAddressToCoordinates(address: String) {
        try {
            val geocoder = Geocoder(this, Locale.KOREA)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                finalLatitude  = addresses[0].latitude
                finalLongitude = addresses[0].longitude
            }
        } catch (e: Exception) {
            Log.e("StoreManage", "좌표 변환 실패", e)
        }
    }

    private fun convertDongToRegionCode(dong: String): String = when {
        dong.contains("대야")  -> "DAEYA"
        dong.contains("신천")  -> "SINCHEON"
        dong.contains("신현")  -> "SINHYEON"
        dong.contains("은행")  -> "EUNHAENG"
        dong.contains("매화")  -> "MAEHWA"
        dong.contains("목감")  -> "MOKGAM"
        dong.contains("군자")  -> "GUNJA"
        dong.contains("월곶")  -> "WOLGOT"
        dong.contains("정왕")  -> "JEONGWANG"
        dong.contains("거북섬") -> "GEOBUKSEOM"
        dong.contains("배곧")  -> "BAEGOT"
        dong.contains("과림")  -> "GWARIM"
        dong.contains("연성")  -> "YEONSEONG"
        dong.contains("능곡")  -> "NEUNGGOK"
        dong.contains("장곡")  -> "JANGGOK"
        else -> "ETC"
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("카페", "식당")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    // 서버 Category enum: RESTAURANT, REST_AREA, BAKERY, CAFE
    private fun convertCategoryToCode(kor: String) = when (kor) {
        "카페" -> "CAFE"
        "식당" -> "RESTAURANT"
        else   -> "CAFE"  // ETC 없음, 기본값 CAFE
    }

    private fun fetchMyRestaurants() {
        RetrofitClient.apiService.getMyRestaurants()
            .enqueue(object : Callback<ApiResponse<List<OwnerRestaurantSummary>>> {
                override fun onResponse(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, response: Response<ApiResponse<List<OwnerRestaurantSummary>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        myRestaurants.clear()
                        myRestaurants.addAll(response.body()?.data ?: emptyList())
                        setupRestaurantSpinner()  // 내부에서 restaurantId 보존해 Spinner 선택
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<OwnerRestaurantSummary>>>, t: Throwable) {
                    Log.e("StoreManage", "업장 목록 로드 실패", t)
                }
            })
    }

    private fun setupRestaurantSpinner() {
        val names = mutableListOf("＋ 새로 만들기")
        names.addAll(myRestaurants.map { it.name })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRestaurant.adapter = adapter

        // intent로 받은 ID가 있으면 해당 업장으로 Spinner 초기화
        if (restaurantId != -1L) {
            val idx = myRestaurants.indexOfFirst { it.id == restaurantId }
            if (idx >= 0) binding.spinnerRestaurant.setSelection(idx + 1)
        }

        binding.spinnerRestaurant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // 새로 만들기: 폼 초기화 + 사업자번호 입력 표시
                    restaurantId = -1L
                    originalData = null
                    clearForm()
                    binding.layoutBusinessNo.visibility = View.VISIBLE
                } else {
                    binding.layoutBusinessNo.visibility = View.GONE
                    restaurantId = myRestaurants[position - 1].id
                    loadStoreDetail()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun clearForm() {
        binding.etStoreName.text?.clear()
        binding.etBusinessNo.text?.clear()
        binding.etStoreAddress.text?.clear()
        binding.etStorePhone.text?.clear()
        binding.cbSmallAnimal.isChecked = false
        binding.cbMediumAnimal.isChecked = false
        binding.cbLargeAnimal.isChecked = false
        binding.cbFence.isChecked = false
        binding.cbArtificialGrass.isChecked = false
        binding.cbNaturalGrass.isChecked = false
        binding.cbSnack.isChecked = false
        binding.cbIndoor.isChecked = false
        binding.cbOutdoor.isChecked = false
        binding.cgSelectedTimes.removeAllViews()
        isDailyAdded = false
        addedWeeklyDays.clear()
        thumbnailUri = null
        bannerUriList.clear()
        binding.ivThumbnailAdd.setImageResource(R.drawable.ic_add_photo)
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

        // 카테고리 Spinner 초기화
        val categoryIdx = when (data.category?.uppercase()) {
            "RESTAURANT" -> 1
            else -> 0  // 기본값 카페
        }
        binding.spinnerCategory.setSelection(categoryIdx)

        // 주소 검색 결과가 없을 때 기존 데이터로 fallback
        if (finalLatitude == 0.0) finalLatitude = data.latitude
        if (finalLongitude == 0.0) finalLongitude = data.longitude
        if (finalRegionCode == "ETC" && data.region != null) finalRegionCode = data.region

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
            // openTime/closeTime이 null이면 Gson이 String에 null 주입 → "null~null" 방지
            if (!hour.regularHoliday && (hour.openTime == null || hour.closeTime == null)) return@forEach
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

        // 새로 만들기일 때 주소 검색(좌표 변환)을 하지 않은 경우 차단
        if (restaurantId == -1L && finalLatitude == 0.0 && finalLongitude == 0.0) {
            Toast.makeText(this, "주소 검색 버튼을 눌러 주소를 검색해주세요.\n(지도 위치 등록에 필요합니다.)", Toast.LENGTH_LONG).show()
            return
        }

        // --- 영업시간 Chip 데이터 파싱 ---
        val operatingHours = mutableListOf<OperatingHourRequest>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            val text = chip.text.toString()

            val isHoliday = text.contains("휴무")
            val timePart = if (!isHoliday) text.substringAfterLast(" ") else null
            val timeSplit = timePart?.split(" ~ ")
            val openTime = if (!isHoliday) timeSplit?.getOrNull(0) else null
            val closeTime = if (!isHoliday) timeSplit?.getOrNull(1) else null

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

        // 주소 검색을 통해 lat/lng/region 이 갱신됐으면 우선 사용, 없으면 기존 데이터 fallback
        val lat  = if (finalLatitude  != 0.0) finalLatitude  else originalData?.latitude
        val lng  = if (finalLongitude != 0.0) finalLongitude else originalData?.longitude
        val region = if (finalRegionCode != "ETC") finalRegionCode else originalData?.region ?: "ETC"
        val category = convertCategoryToCode(binding.spinnerCategory.selectedItem?.toString() ?: "카페")
            .takeIf { it != "ETC" } ?: originalData?.category ?: "CAFE"

        // 새로 만들기: 입력된 사업자번호 사용 / 기존 수정: 서버 데이터 유지
        val businessNo = if (restaurantId == -1L) {
            val input = binding.etBusinessNo.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "사업자등록번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            input
        } else {
            originalData?.businessNo ?: ""
        }

        val request = RestaurantRequest(
            name = name,
            address = address,
            phone = phone,
            businessNo = businessNo,
            category = category,
            region = region,
            latitude = lat,
            longitude = lng,
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
        thumbnailUri?.let { uri -> uriToMultipartPart(uri, "imageFile")?.let { imageParts.add(it) } }
        bannerUriList.forEach { uri -> uriToMultipartPart(uri, "imageFile")?.let { imageParts.add(it) } }

        val imagePartsArg = if (imageParts.isEmpty()) null else imageParts

        if (restaurantId == -1L) {
            // 새로 만들기 → registerRestaurant (POST)
            apiService.registerRestaurant(requestBody, imagePartsArg)
                .enqueue(object : Callback<ApiResponse<Long>> {
                    override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val newId = response.body()?.data ?: -1L
                            Toast.makeText(this@StoreManageActivity, "업장이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                            // 새 업장 ID 반영 후 목록 새로고침
                            restaurantId = newId
                            fetchMyRestaurants()
                        } else {
                            val msg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@StoreManageActivity, "등록 실패: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                        Toast.makeText(this@StoreManageActivity, "등록 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // 기존 업장 수정 → updateRestaurant (PUT)
            apiService.updateRestaurant(restaurantId, requestBody, imagePartsArg)
                .enqueue(object : Callback<ApiResponse<Long>> {
                    override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@StoreManageActivity, "정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val msg = RetrofitClient.parseErrorMessage(response)
                            Toast.makeText(this@StoreManageActivity, "수정 실패: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                        Toast.makeText(this@StoreManageActivity, "수정 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
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

    // Bitmap JPEG 80% 압축 → 서버 업로드 용량 절약
    private fun uriToMultipartPart(uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, bytes.size)
            MultipartBody.Part.createFormData(partName, "store_img_${System.currentTimeMillis()}.jpg", requestFile)
        } catch (e: Exception) {
            Log.e("StoreManage", "이미지 압축 실패", e)
            null
        }
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