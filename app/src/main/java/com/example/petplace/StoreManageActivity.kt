package com.example.petplace // 🚨 본인 패키지명 확인!

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityStoreManageBinding
import com.example.petplace.network.ApiResponse
import com.example.petplace.network.OperatingHourRequest
import com.example.petplace.network.RestaurantDetailResponse
import com.example.petplace.network.RestaurantRequest
import com.example.petplace.network.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.Locale

class StoreManageActivity : AppCompatActivity() {

    private var finalLatitude: Double = 0.0
    private var finalLongitude: Double = 0.0
    private var finalRegionCode: String = "ETC"

    private lateinit var binding: ActivityStoreManageBinding
    private var progressDialog: android.app.Dialog? = null
    private var savedRestaurantId: Long = -1L

    private var thumbnailUri: Uri? = null
    private val bannerUriList = mutableListOf<Uri>()
    private var editIndex: Int = -1

    private var isDailyAdded = false
    private val addedWeeklyDays = mutableSetOf<String>()

    // 💡 테스트용 고정 토큰 (실제 서비스에서는 로그인할 때 받은 토큰으로 교체해야 함!)
    private val testToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxNCIsInJvbGUiOiJPV05FUiIsImlhdCI6MTc4MDA1NzQ4OSwiZXhwIjoxNzgwMTQzODg5fQ.i9FqNdwNfRoSW2xHipgxfrWmNESjehIjLUduSy2SmTv0feuTxQxGjEdoDrMJhqBkgGJMvcbvkpRXfIsCdSLWsg"

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

        // 🔥 주의: onCreate에서는 토큰을 세팅하지 않습니다! API 호출 직전에 세팅할 겁니다.

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

        // 저장 버튼 클릭 시 항상 수정(PUT) API 호출
        binding.ivSaveButton.setOnClickListener {
            packAndSendToServer()
        }

        binding.btnSearchAddress.setOnClickListener {
            val intent = Intent(this, AddressSearchActivity::class.java)
            addressLauncher.launch(intent)
        }

        // 1. SharedPreferences에서 회원가입 시 연동된 식당 고유 ID 로드 (주석 처리)
        // val sharedPref = getSharedPreferences("PetPlacePrefs", Context.MODE_PRIVATE)
        // savedRestaurantId = sharedPref.getLong("OWNER_RESTAURANT_ID", -1L)

        // 🔥 테스트용 하드코딩 식당 ID
        savedRestaurantId = 1L

        // 2. 초기화 단계 검증: 회원가입 시 생성되므로 ID가 무조건 존재해야 함
        if (savedRestaurantId != -1L) {
            Log.d("StoreManageActivity", "Saved restaurant ID verified: $savedRestaurantId. Executing initial fetch.")
            fetchRestaurantDetailFromServer(savedRestaurantId)
        } else {
            Log.e("StoreManageActivity", "Initialization failed: OWNER_RESTAURANT_ID is missing.")
            Toast.makeText(this, "식당 정보 고유 ID를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val addressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val fullAddress = data?.getStringExtra("address") ?: ""
            val dongName = data?.getStringExtra("dong") ?: ""

            binding.etStoreAddress.setText(fullAddress)
            convertAddressToCoordinates(fullAddress)
            finalRegionCode = convertDongToRegionCode(dongName)

            Toast.makeText(this, "지역코드: $finalRegionCode \n좌표: ($finalLatitude, $finalLongitude)", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertAddressToCoordinates(address: String) {
        try {
            val geocoder = Geocoder(this, Locale.KOREA)
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                finalLatitude = addresses[0].latitude
                finalLongitude = addresses[0].longitude
            } else {
                Toast.makeText(this, "지도에서 좌표를 찾을 수 없는 주소입니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("StoreManageActivity", "convertAddressToCoordinates error", e)
        }
    }

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
            dong.contains("배곧") -> "BAEGON"
            dong.contains("과림") -> "GWARIM"
            dong.contains("연성") -> "YEONSEONG"
            dong.contains("능곡") -> "NEUNGGOK"
            dong.contains("장곡") -> "JANGGOK"
            else -> "ETC"
        }
    }

    // =========================================================================
    // 🌐 1. 서버에서 기존 등록된 업장 정보 가져오기 (GET)
    // =========================================================================
    private fun fetchRestaurantDetailFromServer(restaurantId: Long) {
        showLoadingDialog()

        // 🌟 핵심 수정 1: GET 통신 출발 직전에 토큰을 확실하게 장착!
        RetrofitClient.setToken(testToken)

        RetrofitClient.apiService.getRestaurantDetail(restaurantId)
            .enqueue(object : Callback<ApiResponse<RestaurantDetailResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<RestaurantDetailResponse>>,
                    response: Response<ApiResponse<RestaurantDetailResponse>>
                ) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val restaurantData = response.body()?.data
                        if (restaurantData != null) {
                            Log.i("StoreManageActivity", "fetchRestaurantDetail success: id=${restaurantData.id}")
                            bindRestaurantDataToUI(restaurantData)
                        }
                    } else {
                        val errorMsg = RetrofitClient.parseErrorMessage(response)
                        val rawErrorBody = response.errorBody()?.string() ?: "에러 본문 없음"

                        Log.e("StoreManageActivity", "API Request Failed. Code: ${response.code()}")
                        Log.e("StoreManageActivity", "Parsed Error: $errorMsg")
                        Log.e("StoreManageActivity", "Raw Error Body: $rawErrorBody")

                        android.app.AlertDialog.Builder(this@StoreManageActivity)
                            .setTitle("정보 불러오기 실패 (${response.code()})")
                            .setMessage(rawErrorBody)
                            .setPositiveButton("확인", null)
                            .show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<RestaurantDetailResponse>>, t: Throwable) {
                    dismissLoadingDialog()
                    Log.e("StoreManageActivity", "fetchRestaurantDetail network failure", t)
                    Toast.makeText(this@StoreManageActivity, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🌐 2. 데이터를 패키징해서 서버로 수정 본문 발송 (PUT)
    // =========================================================================
    private fun packAndSendToServer() {
        val storeName = binding.etStoreName.text.toString().trim()
        val storeAddress = binding.etStoreAddress.text.toString().trim()
        val storePhone = binding.etStorePhone.text.toString().trim()

        if (storeName.isEmpty() || storeAddress.isEmpty() || storePhone.isEmpty()) {
            Toast.makeText(this, "필수 항목(이름, 주소, 전화번호)을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (savedRestaurantId == -1L) {
            Log.e("StoreManageActivity", "packAndSendToServer abort: invalid savedRestaurantId")
            return
        }

        showLoadingDialog()

        val timeChipTexts = mutableListOf<String>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            timeChipTexts.add(chip.text.toString())
        }
        val serverOperatingHours = parseHoursToSeverFormat(timeChipTexts)
        val selectedCategory = binding.spinnerCategory.selectedItem.toString()
        val finalCategoryCode = convertCategoryToCode(selectedCategory)

        val existingUrlsToKeep = mutableListOf<String>()
        val imageParts = mutableListOf<MultipartBody.Part>()

        // 썸네일 분류
        thumbnailUri?.let {
            if (it.scheme == "http" || it.scheme == "https") {
                existingUrlsToKeep.add(it.toString())
            } else {
                uriToMultipartPart(it, "imageFile")?.let { part -> imageParts.add(part) }
            }
        }

        // 배너 리스트 분류
        for (bannerUri in bannerUriList) {
            if (bannerUri.scheme == "http" || bannerUri.scheme == "https") {
                existingUrlsToKeep.add(bannerUri.toString())
            } else {
                uriToMultipartPart(bannerUri, "imageFile")?.let { part -> imageParts.add(part) }
            }
        }

        val restaurantUpdateRequest = com.example.petplace.network.RestaurantUpdateRequest(
            name = storeName,
            address = storeAddress,
            phone = storePhone,
            category = finalCategoryCode,
            region = finalRegionCode,
            latitude = finalLatitude,
            longitude = finalLongitude,
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
            operatingHours = serverOperatingHours,
            existingImageUrls = existingUrlsToKeep
        )

        val jsonString = Gson().toJson(restaurantUpdateRequest)
        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

        Log.i("StoreManageActivity", "--- [PUT Request Data Check] ---")
        Log.i("StoreManageActivity", "3. existingUrlsToKeep (Size: ${existingUrlsToKeep.size}): $existingUrlsToKeep")
        Log.i("StoreManageActivity", "4. imageParts (New Files): ${imageParts.size}")
        Log.i("StoreManageActivity", "--------------------------------")

        // 🌟 핵심 수정 2: PUT 통신 출발 직전에도 토큰을 확실하게 장착!
        RetrofitClient.setToken(testToken)

        RetrofitClient.apiService.updateRestaurant(savedRestaurantId, requestBody, imageParts)
            .enqueue(object : Callback<ApiResponse<Long>> {
                override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.i("StoreManageActivity", "updateRestaurant PUT success. id: $savedRestaurantId")
                        fetchRestaurantDetailFromServer(savedRestaurantId)
                        Toast.makeText(this@StoreManageActivity, "장소 정보가 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorCode = response.code()
                        val rawErrorBody = response.errorBody()?.string() ?: "에러 본문 없음"

                        Log.e("StoreManageActivity", "🚨🚨🚨 ERROR CODE: $errorCode 🚨🚨🚨")
                        Log.e("StoreManageActivity", "🚨🚨🚨 ERROR BODY: $rawErrorBody 🚨🚨🚨")

                        android.app.AlertDialog.Builder(this@StoreManageActivity)
                            .setTitle("수정 실패 ($errorCode)")
                            .setMessage(rawErrorBody)
                            .setPositiveButton("확인", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                    dismissLoadingDialog()
                    Log.e("StoreManageActivity", "updateRestaurant PUT network failure", t)
                    Toast.makeText(this@StoreManageActivity, "네트워크 통신 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // =========================================================================
    // 🎨 3. 서버에서 가져온 초기 생성 데이터를 UI에 매핑
    // =========================================================================
    private fun bindRestaurantDataToUI(data: RestaurantDetailResponse) {
        binding.etStoreName.setText(data.name)
        binding.etStoreAddress.setText(data.address)
        binding.etStorePhone.setText(data.phone)

        finalLatitude = data.latitude
        finalLongitude = data.longitude
        finalRegionCode = data.region ?: "ETC"

        binding.cbFence.isChecked = data.hasFence
        binding.cbArtificialGrass.isChecked = data.hasArtificialGrass
        binding.cbNaturalGrass.isChecked = data.hasNaturalGrass
        binding.cbSnack.isChecked = data.hasSnack
        binding.cbParking.isChecked = data.hasParking
        binding.cbRestroom.isChecked = data.hasRestroom
        binding.cbIndoor.isChecked = data.hasIndoor
        binding.cbOutdoor.isChecked = data.hasOutdoor

        binding.cbSmallAnimal.isChecked = data.allowSmall
        binding.cbMediumAnimal.isChecked = data.allowMedium
        binding.cbLargeAnimal.isChecked = data.allowLarge

        if (data.category == "CAFE") {
            binding.spinnerCategory.setSelection(0)
        } else if (data.category == "RESTAURANT") {
            binding.spinnerCategory.setSelection(1)
        }

        // 이미지 초기화
        thumbnailUri = null
        bannerUriList.clear()

        while (binding.layoutBannerContainer.childCount > 1) {
            binding.layoutBannerContainer.removeViewAt(0)
        }

        // 🌟 바뀐 imageUrls 필드 처리 (0번 인덱스 방어 코드 추가)
        data.imageUrls?.let { urls ->
            if (urls.isNotEmpty()) {
                val thumbUrl = urls[0]
                thumbnailUri = Uri.parse(thumbUrl)
                Glide.with(this)
                    .load(thumbUrl)
                    .centerCrop()
                    .into(binding.ivThumbnailAdd)

                for (i in 1 until urls.size) {
                    val bannerUrl = urls[i]
                    val bannerUri = Uri.parse(bannerUrl)

                    bannerUriList.add(bannerUri)
                    addBannerImageToLayout(bannerUri)
                }
            } else {
                // 이미지가 하나도 없을 땐 썸네일 비우기
                binding.ivThumbnailAdd.setImageDrawable(null)
            }
        }

        // ⏰ 영업시간 처리
        binding.cgSelectedTimes.removeAllViews()
        data.operatingHours?.let { hours ->
            val dayMap = mapOf("MON" to "월", "TUE" to "화", "WED" to "수", "THU" to "목", "FRI" to "금", "SAT" to "토", "SUN" to "일")
            val dayOrder = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

            fun formatTimeWithoutSeconds(time: String?): String {
                if (time == null) return "00:00"
                return if (time.length >= 5) time.substring(0, 5) else time
            }

            data class ScheduleGroupKey(val open: String, val close: String, val isHoliday: Boolean)

            val groupedSchedules = hours.groupBy { hour ->
                ScheduleGroupKey(
                    open = formatTimeWithoutSeconds(hour.openTime),
                    close = formatTimeWithoutSeconds(hour.closeTime),
                    isHoliday = hour.isRegularHoliday
                )
            }

            for ((key, hourList) in groupedSchedules) {
                val timeString = if (key.isHoliday) "휴무" else "${key.open} ~ ${key.close}"
                val presentDays = hourList.map { it.dayOfWeek }

                if (presentDays.size == 7 && presentDays.containsAll(dayOrder)) {
                    addChipToGroup("매일 $timeString", isDaily = true, days = emptyList())
                    isDailyAdded = true
                } else {
                    val sortedKorDays = dayOrder.filter { presentDays.contains(it) }.map { dayMap[it] ?: "" }
                    val daysString = sortedKorDays.joinToString(", ")
                    addChipToGroup("$daysString $timeString", isDaily = false, days = sortedKorDays)
                    addedWeeklyDays.addAll(sortedKorDays)
                }
            }
        }
    }

    // =========================================================================
    // ⏳ 4. 로딩 다이얼로그 제어
    // =========================================================================
    private fun showLoadingDialog() {
        if (progressDialog == null) {
            progressDialog = android.app.Dialog(this).apply {
                val progressBar = android.widget.ProgressBar(this@StoreManageActivity).apply {
                    indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#FF8A4C"))
                }
                setContentView(progressBar)
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setCancelable(false)
            }
        }
        progressDialog?.show()
    }

    private fun dismissLoadingDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        dismissLoadingDialog()
        super.onDestroy()
    }

    // =========================================================================
    // 이하 헬퍼 함수들 (기존과 동일)
    // =========================================================================
    private fun parseHoursToSeverFormat(chipTexts: List<String>): List<OperatingHourRequest> {
        val resultList = mutableListOf<OperatingHourRequest>()
        val dayMap = mapOf("월" to "MON", "화" to "TUE", "수" to "WED", "목" to "THU", "금" to "FRI", "토" to "SAT", "일" to "SUN")

        for (text in chipTexts) {
            val isHoliday = text.contains("휴무")
            var openTime: String? = null
            var closeTime: String? = null

            if (!isHoliday) {
                val timeSplit = text.split(" ~ ")
                if (timeSplit.size == 2) {
                    openTime = convertTo24HourFormat(timeSplit[0])
                    closeTime = convertTo24HourFormat(timeSplit[1])
                }
            }

            if (text.startsWith("매일")) {
                val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                for (d in allDays) {
                    resultList.add(OperatingHourRequest(d, openTime, closeTime, isHoliday))
                }
            } else {
                for ((kor, eng) in dayMap) {
                    if (text.contains(kor)) {
                        resultList.add(OperatingHourRequest(eng, openTime, closeTime, isHoliday))
                    }
                }
            }
        }
        return resultList
    }

    private fun uriToMultipartPart(uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            // 화질 너무 박살 나지 않게 50 정도로 타협
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val compressedBytes = outputStream.toByteArray()
            val requestFile = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, compressedBytes.size)
            MultipartBody.Part.createFormData(partName, "store_img_${System.currentTimeMillis()}.jpg", requestFile)
        } catch (e: Exception) {
            Log.e("StoreManageActivity", "uriToMultipartPart error", e)
            null
        }
    }

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

    private fun setupCategorySpinner() {
        val categories = arrayOf("카페", "식당")
        val adapter = ArrayAdapter(this, R.layout.item_spinner, categories)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        binding.spinnerCategory.adapter = adapter
        binding.spinnerCategory.setSelection(0)
    }

    private fun convertCategoryToCode(koreanCategory: String): String {
        return when (koreanCategory) {
            "카페" -> "CAFE"
            "식당" -> "RESTAURANT"
            else -> "ETC"
        }
    }

    private fun convertTo24HourFormat(timeString: String?): String? {
        if (timeString == null) return null

        val regex = Regex("(오전|오후)?\\s*(\\d{1,2}):(\\d{2})")
        val matchResult = regex.find(timeString)

        if (matchResult != null) {
            val amPm = matchResult.groupValues[1]
            val hourStr = matchResult.groupValues[2]
            val minuteStr = matchResult.groupValues[3]

            var hour = hourStr.toIntOrNull() ?: return timeString

            if (amPm == "오후" && hour < 12) {
                hour += 12
            } else if (amPm == "오전" && hour == 12) {
                hour = 0
            }

            return String.format(java.util.Locale.getDefault(), "%02d:%s", hour, minuteStr)
        }
        return timeString
    }
}