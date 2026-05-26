package com.example.petplace // 🚨 본인 패키지명에 맞게 꼭 수정해!

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
import com.google.android.material.chip.Chip

class StoreManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreManageBinding

    // 이미지 관리를 위한 변수
    private var thumbnailUri: Uri? = null
    private val bannerUriList = mutableListOf<Uri>()
    private var editIndex: Int = -1

    // 영업시간 상태 관리를 위한 변수
    private var isDailyAdded = false
    private val addedWeeklyDays = mutableSetOf<String>()

    // 이미지 런처 세팅
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

        // 버튼 클릭 이벤트 세팅
        binding.ivThumbnailAdd.setOnClickListener {
            thumbnailLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.ivBannerAdd.setOnClickListener {
            editIndex = -1
            bannerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.cbIndoor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.cbOutdoor.isChecked = false // 실내 켜지면 실외 꺼!
        }

        binding.cbOutdoor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.cbIndoor.isChecked = false // 실외 켜지면 실내 꺼!
        }

        binding.btnBack.setOnClickListener { finish() }

        // 영업시간 관련 초기화
        setupSpinner()
        setupTimePicker()

        binding.ivSaveButton.setOnClickListener {
            packDataForDetail() // 우리가 아까 만든 '데이터 싹 긁어모으는 함수' 실행!
        }
    }

    // =========================================================================
    // 데이터로 포장하는 함수
    // =========================================================================
    private fun packDataForDetail() {
        // 1. Chip(태그)에 적힌 시간 텍스트만 뽑아옴
        val timeList = mutableListOf<String>()
        for (i in 0 until binding.cgSelectedTimes.childCount) {
            val chip = binding.cgSelectedTimes.getChildAt(i) as Chip
            timeList.add(chip.text.toString())
        }

        // 🚨 주의: etStoreName, etStoreAddress는 XML에 있는 실제 EditText ID로 맞춰줘야 함
        val storeData = StoreDetailData(
            storeName = binding.etStoreName.text.toString(),
            storeAddress = binding.etStoreAddress.text.toString(),
            thumbnailUri = thumbnailUri?.toString(),
            bannerUris = bannerUriList.map { it.toString() },
            operatingHours = timeList
        )

        Log.d("API테스트", "포장 완료된 데이터: $storeData")
        Toast.makeText(this, "상세페이지로 넘길 데이터 완료!", Toast.LENGTH_SHORT).show()

        // 나중에 이 storeData를 Intent에 실어서 상세 액티비티로 넘기거나, 서버(API)로 쏘면 됨
    }


    // =========================================================================
    // 1. 스피너 세팅 (매일 / 매주)
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

    // =========================================================================
    // 2. 바텀시트 시간 선택 세팅
    // =========================================================================
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

    // =========================================================================
    // 3. 칩(Chip) 생성 조건 검사 및 추가 로직
    // =========================================================================
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

    // =========================================================================
    // 4. 동적으로 Chip(태그) UI 생성 및 X 버튼 동작 처리
    // =========================================================================
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

    // =========================================================================
    // Helper 함수들
    // =========================================================================
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
        val checkBoxes = listOf(
            binding.cbMon, binding.cbTue, binding.cbWed, binding.cbThu,
            binding.cbFri, binding.cbSat, binding.cbSun
        )
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
}

// =========================================================================
// 사용할 데이터 박스 (Data Class)
// =========================================================================
data class StoreDetailData(
    val storeName: String,       // 가게 이름
    val storeAddress: String,    // 가게 주소
    val thumbnailUri: String?,   // 썸네일 사진 주소
    val bannerUris: List<String>,// 배너 사진 주소들
    val operatingHours: List<String> // 영업시간 모음
)