package com.example.petplace // 🚨 본인 패키지명 확인!

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
import com.example.petplace.databinding.ActivityStoreManageBinding
import com.example.petplace.network.RetrofitClient
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.RestaurantRequest
import com.example.petplace.network.OperatingHourRequest
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.location.Geocoder
import android.content.Intent
import java.util.Locale

class StoreManageActivity : AppCompatActivity() {

    private var finalLatitude: Double = 0.0
    private var finalLongitude: Double = 0.0
    private var finalRegionCode: String = "ETC" // 기본값

    private lateinit var binding: ActivityStoreManageBinding

    private var thumbnailUri: Uri? = null
    private val bannerUriList = mutableListOf<Uri>()
    private var editIndex: Int = -1

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

        binding.ivThumbnailAdd.setOnClickListener {
            thumbnailLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivBannerAdd.setOnClickListener {
            editIndex = -1
            bannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.cbIndoor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.cbOutdoor.isChecked = false
        }

        binding.cbOutdoor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.cbIndoor.isChecked = false
        }

        binding.btnBack.setOnClickListener { finish() }

        setupSpinner()
        setupCategorySpinner()
        setupTimePicker()

        binding.ivSaveButton.setOnClickListener {
            packAndSendToServer() // 🔥 이제 서버로 진짜 발송!
        }

        binding.btnSearchAddress.setOnClickListener {
            val intent = Intent(this, AddressSearchActivity::class.java)
            addressLauncher.launch(intent)
        }
    }

    private val addressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val fullAddress = data?.getStringExtra("address") ?: ""
            val dongName = data?.getStringExtra("dong") ?: ""

            // 화면에 선택한 주소 글씨 띄우기
            binding.etStoreAddress.setText(fullAddress)

            // 텍스트 주소를 위도/경도로 변환!! (Geocoder)
            convertAddressToCoordinates(fullAddress)

            // 동을 영어 이름으로 바꿈
            finalRegionCode = convertDongToRegionCode(dongName)

            Toast.makeText(this, "지역코드: $finalRegionCode \n좌표: ($finalLatitude, $finalLongitude)", Toast.LENGTH_LONG).show()
        }
    }

    // 🔥 3. 텍스트 주소 -> 위도/경도 숫자 변환기
    private fun convertAddressToCoordinates(address: String) {
        try {
            val geocoder = Geocoder(this, Locale.KOREA)
            // 안드로이드 13(Tiramisu) 이상부터는 Geocoder 사용법이 바뀜 (동기/비동기 호환)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                finalLatitude = addresses[0].latitude
                finalLongitude = addresses[0].longitude
            } else {
                Toast.makeText(this, "지도에서 좌표를 찾을 수 없는 주소입니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GEOCODER", "좌표 변환 실패", e)
        }
    }

    // 🔥 4. 한글 동 이름 -> 영어 지역 코드 번역기 (팀원 명세서 기준)
    private fun convertDongToRegionCode(dong: String): String {
        return when {
            dong.contains("대야") -> "DAEYA"
            dong.contains("신천") -> "SINCHEON"
            dong.contains("신현") -> "SINHYEON"
            dong.contains("은행") -> "EUNHAENG"
            dong.contains("매화") -> "MAEHWA"
            dong.contains("목감") -> "MOKGAM"
            dong.contains("군자") -> "GUNJA"
            dong.contains("월곶") -> "WOLGOT"
            dong.contains("정왕") -> "JEONGWANG"
            dong.contains("거북섬") -> "GEOBUKSEOM"
            dong.contains("배곧") -> "BAEGOT"
            dong.contains("과림") -> "GWARIM"
            dong.contains("연성") -> "YEONSEONG"
            dong.contains("능곡") -> "NEUNGGOK"
            dong.contains("장곡") -> "JANGGOK"
            else -> "ETC" // 시흥시 외의 기타 지역이 들어올 경우 기본값
        }
    }

    // =========================================================================
    // 🔥 [핵심] 데이터를 패키징해서 진짜 서버로 쏘는 함수
    // =========================================================================
    private fun packAndSendToServer() {
        val storeName = binding.etStoreName.text.toString().trim()
        val storeAddress = binding.etStoreAddress.text.toString().trim()
        val storePhone = binding.etStorePhone.text.toString().trim()

        // 간단한 유효성 검사
        if (storeName.isEmpty() || storeAddress.isEmpty() || storePhone.isEmpty()) {
            Toast.makeText(this, "필수 항목(이름, 주소, 전화번호)을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (thumbnailUri == null) {
            Toast.makeText(this, "썸네일 사진은 필수입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1️⃣ Chip 텍스트들을 긁어모아 서버가 원하는 요일별 리스트로 조각조각 변환!
        val timeChipTexts = mutableListOf<String>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            timeChipTexts.add(chip.text.toString())
        }
        val serverOperatingHours = parseHoursToSeverFormat(timeChipTexts)
        val selectedCategory = binding.spinnerCategory.selectedItem.toString()
        val finalCategoryCode = convertCategoryToCode(selectedCategory)

        // 2️⃣ 서버에 보낼 JSON 바디 객체 조립!
        val restaurantRequest = RestaurantRequest(
            name = storeName,
            address = storeAddress,
            phone = storePhone,
            businessNo = "120-00-12346", // 화면에 칸이 없어서 일단 더미로 세팅
            category = finalCategoryCode,
            region = finalRegionCode, // BAEGOT 같은 영어 코드
            latitude = finalLatitude, // 37.xxx
            longitude = finalLongitude, // 126.xxx
            hasFence = binding.cbFence.isChecked,
            hasArtificialGrass = binding.cbArtificialGrass.isChecked,
            hasNaturalGrass = binding.cbNaturalGrass.isChecked,
            hasSnack = binding.cbSnack.isChecked,
            hasParking = binding.cbParking.isChecked,
            hasRestroom = binding.cbRestroom.isChecked,
            hasIndoor = binding.cbIndoor.isChecked,
            hasOutdoor = binding.cbOutdoor.isChecked,
            allowSmall = binding.cbSmallAnimal.isChecked,
            allowMedium = binding.cbMediumAnimal.isChecked,
            allowLarge = binding.cbLargeAnimal.isChecked,
            menus = emptyList(), // 메뉴 등록 화면은 따로 있으니 일단 패스!
            operatingHours = serverOperatingHours
        )

        // 3️⃣ JSON 객체를 서버 전송용 Multipart RequestBody로 포장 (비닐 래핑 작업)
        val jsonString = Gson().toJson(restaurantRequest)
        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

        // 4️⃣ 이미지들을 하나의 리스트로 합체! (★팀원분 조건: 0번째가 썸ne일, 그 뒤가 배너)
        val imageParts = mutableListOf<MultipartBody.Part>()

        // 0번째 썸네일 장전
        uriToMultipartPart(thumbnailUri!!, "images")?.let { imageParts.add(it) }

        // 그 뒤로 배너 사진들 연달아 장전
        for (bannerUri in bannerUriList) {
            uriToMultipartPart(bannerUri, "images")?.let { imageParts.add(it) }
        }

        // 5️⃣ 대망의 레트로핏 배달원 출발!!
        Toast.makeText(this, "업장 등록 중...", Toast.LENGTH_SHORT).show()

        RetrofitClient.setToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxNCIsInJvbGUiOiJPV05FUiIsImlhdCI6MTc3OTk1NDQxNiwiZXhwIjoxNzgwMDQwODE2fQ.y7TZrp61HdL51plxasWppZdE9g-zWsQAOGed1udmKVRYj8ccUW9xGh0QovKeQI8GEryegWUdqJsEVr8jc8bfpw")

        RetrofitClient.apiService.registerRestaurant(requestBody, imageParts)
            .enqueue(object : Callback<ApiResponse<Long>> {
                override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverId = response.body()?.data
                        Toast.makeText(this@StoreManageActivity, "성공적으로 등록되었습니다! ID: $serverId", Toast.LENGTH_LONG).show()
                        finish() // 성공하면 화면 닫기
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        Toast.makeText(this@StoreManageActivity, "등록 실패: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                    Toast.makeText(this@StoreManageActivity, "네트워크 연결 실패..", Toast.LENGTH_SHORT).show()
                    Log.e("API_FAIL", "통신 에러 발생", t)
                }
            })
    }

    // =========================================================================
    // 🛠️ 대박 중요: "월, 화 09:00 ~ 18:00" 문자열을 서버용 리스트로 분해하는 기계
    // =========================================================================
    private fun parseHoursToSeverFormat(chipTexts: List<String>): List<OperatingHourRequest> {
        val resultList = mutableListOf<OperatingHourRequest>()
        val dayMap = mapOf("월" to "MON", "화" to "TUE", "수" to "WED", "목" to "THU", "금" to "FRI", "토" to "SAT", "일" to "SUN")

        for (text in chipTexts) {
            val isHoliday = text.contains("휴무")
            var openTime: String? = null
            var closeTime: String? = null

            if (!isHoliday) {
                // "09:00 ~ 18:00" 부분 가려내기
                val timePart = text.split(" ").lastOrNull() ?: ""
                if (timePart.contains("~")) {
                    val times = timePart.split("~")
                    openTime = times.getOrNull(0)?.trim()
                    closeTime = times.getOrNull(1)?.trim()
                }
            }

            if (text.startsWith("매일")) {
                val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                for (d in allDays) {
                    resultList.add(OperatingHourRequest(d, openTime, closeTime, isHoliday))
                }
            } else {
                // "월, 화" 처럼 적힌 한국어 요일을 찾아서 영어 코드로 매핑
                for ((kor, eng) in dayMap) {
                    if (text.contains(kor)) {
                        resultList.add(OperatingHourRequest(eng, openTime, closeTime, isHoliday))
                    }
                }
            }
        }
        return resultList
    }

    // =========================================================================
    // 🖼️ Uri 주소를 진짜 파일 비트(MultipartBody.Part)로 굽는 헬퍼 함수
    // =========================================================================
    private fun uriToMultipartPart(uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // 1. 이미지를 안드로이드가 다룰 수 있는 도화지(Bitmap)로 불러오기
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // 2. 압축기(ByteArrayOutputStream) 준비
            val outputStream = ByteArrayOutputStream()

            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            // 4. 다이어트 끝난 데이터(Byte 배열) 꺼내기
            val compressedBytes = outputStream.toByteArray()

            // 5. 서버로 보낼 택배(RequestBody)로 포장
            val requestFile = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, compressedBytes.size)

            // 6. 최종 Multipart 파트로 완성! (이름도 .jpg로 고정)
            MultipartBody.Part.createFormData(partName, "store_img_${System.currentTimeMillis()}.jpg", requestFile)

        } catch (e: Exception) {
            Log.e("IMAGE_CONVERT_ERROR", "이미지 압축/변환 실패", e)
            null
        }
    }

    // =========================================================================
    // 기존에 주원님이 만들어둔 스피너 및 UI 부가 기능들 (그대로 유지!)
    // =========================================================================
    private fun setupSpinner() {
        val repeatItems = arrayOf("매일", "매주")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, repeatItems)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        binding.spinnerRepeat.adapter = adapter
        binding.spinnerRepeat.setSelection(0)

        binding.spinnerRepeat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = repeatItems[position]
                binding.cgSelectedTimes.removeAllViews()
                isDailyAdded = false
                addedWeeklyDays.clear()
                resetTimeInputs()

                if (selectedItem == "매주") {
                    binding.layoutWeeklyDays.visibility = View.VISIBLE
                } else {
                    binding.layoutWeeklyDays.visibility = View.GONE
                    clearCheckboxes()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTimePicker() {
        binding.tvStartTime.setOnClickListener {
            val bottomSheet = StoreTimeBottomSheet { selectedTime ->
                if (selectedTime == "휴무") {
                    binding.tvStartTime.text = "휴무"
                    binding.tvEndTime.text = "휴무"
                } else {
                    binding.tvStartTime.text = selectedTime
                }
                checkAndCreateTimeChip()
            }
            bottomSheet.show(supportFragmentManager, "TimePicker")
        }

        binding.tvEndTime.setOnClickListener {
            val bottomSheet = StoreTimeBottomSheet { selectedTime ->
                if (selectedTime == "휴무") {
                    binding.tvStartTime.text = "휴무"
                    binding.tvEndTime.text = "휴무"
                } else {
                    binding.tvEndTime.text = selectedTime
                }
                checkAndCreateTimeChip()
            }
            bottomSheet.show(supportFragmentManager, "TimePicker")
        }
    }

    private fun checkAndCreateTimeChip() {
        val startTime = binding.tvStartTime.text.toString()
        val endTime = binding.tvEndTime.text.toString()
        if (startTime == "입력" || endTime == "입력") return

        val isDaily = binding.spinnerRepeat.selectedItem.toString() == "매일"
        val isHoliday = startTime == "휴무" || endTime == "휴무"
        val timeString = if (isHoliday) "휴무" else "$startTime ~ $endTime"

        if (isDaily) {
            if (isDailyAdded) {
                Toast.makeText(this, "이미 설정한 날은 다시 설정할 수 없습니다.", Toast.LENGTH_SHORT).show()
                resetTimeInputs()
                return
            }
            addChipToGroup("매일 $timeString", isDaily = true, days = emptyList())
            isDailyAdded = true
        } else {
            val selectedDays = getSelectedDays()
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "적용할 요일을 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
                resetTimeInputs()
                return
            }
            val overlap = selectedDays.intersect(addedWeeklyDays)
            if (overlap.isNotEmpty()) {
                Toast.makeText(this, "이미 설정한 날은 다시 설정할 수 없습니다.", Toast.LENGTH_SHORT).show()
                resetTimeInputs()
                return
            }
            val daysString = selectedDays.joinToString(", ")
            addChipToGroup("$daysString $timeString", isDaily = false, days = selectedDays)
            addedWeeklyDays.addAll(selectedDays)
            clearCheckboxes()
        }
        resetTimeInputs()
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
                if (isDaily) {
                    isDailyAdded = false
                } else {
                    addedWeeklyDays.removeAll(days.toSet())
                }
            }
        }
        binding.cgSelectedTimes.addView(chip)
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

    private fun clearCheckboxes() {
        val checkBoxes = listOf(binding.cbMon, binding.cbTue, binding.cbWed, binding.cbThu, binding.cbFri, binding.cbSat, binding.cbSun)
        checkBoxes.forEach { it.isChecked = false }
    }

    private fun resetTimeInputs() {
        binding.tvStartTime.text = "입력"
        binding.tvEndTime.text = "입력"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun addBannerImageToLayout(uri: Uri) {
        val newImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(100)).apply {
                marginEnd = dpToPx(10)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this).load(uri).into(newImageView)
        newImageView.setOnClickListener {
            editIndex = binding.layoutBannerContainer.indexOfChild(it)
            bannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        val insertIndex = binding.layoutBannerContainer.childCount - 1
        binding.layoutBannerContainer.addView(newImageView, insertIndex)
    }

    // =========================================================================
    // 🏷️ 카테고리 스피너 세팅 및 영문 변환기
    // =========================================================================
    private fun setupCategorySpinner() {
        val categories = arrayOf("카페", "식당")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, categories)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        binding.spinnerCategory.adapter = adapter
        binding.spinnerCategory.setSelection(0) // 기본값 "카페"
    }

    private fun convertCategoryToCode(koreanCategory: String): String {
        return when (koreanCategory) {
            "카페" -> "CAFE"
            "식당" -> "RESTAURANT"
            else -> "ETC"
        }
    }
}

