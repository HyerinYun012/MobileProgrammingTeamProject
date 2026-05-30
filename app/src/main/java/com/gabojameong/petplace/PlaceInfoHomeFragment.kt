package com.gabojameong.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

import com.gabojameong.petplace.databinding.FragmentPlaceInfoHomeBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class PlaceInfoHomeFragment : Fragment(R.layout.fragment_place_info_home) {
    private var _binding: FragmentPlaceInfoHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceInfoHomeBinding.bind(view)

        val restaurant = arguments?.getSerializable("restaurant", RestaurantResponse::class.java)
        restaurant?.let {
            initUI(it)
            setupMapButton(it)
        }
    }

    private fun setupMapButton(res: RestaurantResponse) {
        val lat = res.latitude
        val lng = res.longitude

        // 좌표 없으면 버튼 숨김
        if (lat == 0.0 && lng == 0.0) {
            binding.btnOpenMap.visibility = View.GONE
            return
        }

        binding.btnOpenMap.visibility = View.VISIBLE
        binding.btnOpenMap.setOnClickListener {
            startActivity(
                Intent(requireContext(), MapActivity::class.java).apply {
                    putExtra("target_lat", lat)
                    putExtra("target_lng", lng)
                    putExtra("target_name", res.name)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
    }

    // 부모 Activity에서 상세 데이터를 새로 가져왔을 때 호출되어 UI를 갱신합니다.
    fun refreshData(newRes: RestaurantResponse) {
        if (_binding != null) {
            initUI(newRes)
        } else {
            arguments?.putSerializable("restaurant", newRes)
        }
    }

    private fun initUI(res: RestaurantResponse) {
        binding.textViewAddress.text = res.address
        binding.textViewPhone.text = formatPhoneNumber(res.phone)

        // 오늘 영업 상태 + 전체 스케줄
        updateWorkingStatus(res.operatingHours)
        showFullSchedule(res.operatingHours)

        val (iconRes, sizeText) = when {
            res.allowSmall && res.allowMedium && res.allowLarge -> R.drawable.icon_large_cap to "모든 견종 가능"
            res.allowSmall && res.allowMedium -> R.drawable.icon_medium_cap to "중·소형견 가능"
            res.allowSmall -> R.drawable.icon_small to "소형견 가능"
            res.allowMedium -> R.drawable.icon_medium_solo to "중형견 가능"
            res.allowLarge -> R.drawable.icon_large_solo to "대형견 가능"
            else -> 0 to ""
        }

        if (iconRes != 0) {
            binding.layoutDogSize.visibility = View.VISIBLE
            binding.imageViewDogSize.setImageResource(iconRes)
            binding.textViewDogSize.text = sizeText
        } else {
            binding.layoutDogSize.visibility = View.GONE
        }

        binding.layoutFence.visibility = if (res.hasFence) View.VISIBLE else View.GONE
        binding.layoutIndoor.visibility = if (res.hasIndoor) View.VISIBLE else View.GONE
        binding.layoutOutdoor.visibility = if (res.hasOutdoor) View.VISIBLE else View.GONE
        binding.layoutArtificialGrass.visibility = if (res.hasArtificialGrass) View.VISIBLE else View.GONE
        binding.layoutNaturalGrass.visibility = if (res.hasNaturalGrass) View.VISIBLE else View.GONE
        binding.layoutSnack.visibility = if (res.hasSnack) View.VISIBLE else View.GONE
        binding.layoutParking.visibility = if (res.hasParking) View.VISIBLE else View.GONE
        binding.layoutRestroom.visibility = if (res.hasRestroom) View.VISIBLE else View.GONE
    }

    private fun updateWorkingStatus(hours: List<OperatingHour>?) {
        if (hours.isNullOrEmpty()) {
            binding.textViewIsWorking.text = "정보 없음 · "
            binding.textViewTime.text = "-"
            binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            return
        }

        val calendar = Calendar.getInstance()
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "SUN"
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> ""
        }

        val todayHour = hours.find { it.dayOfWeek.startsWith(dayOfWeek, ignoreCase = true) }
        
        if (todayHour == null) {
            // 영업 정보가 등록되어 있지만 오늘은 휴무인 경우
            binding.textViewIsWorking.text = "오늘 휴무 · "
            // 다음 영업일 찾아서 표시
            val dayOrder = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
            val todayIdx = dayOrder.indexOf(dayOfWeek)
            val nextHour = (1..6).mapNotNull { offset ->
                val nextDay = dayOrder[(todayIdx + offset) % 7]
                hours.find { it.dayOfWeek.equals(nextDay, ignoreCase = true) && !it.regularHoliday }
            }.firstOrNull()
            if (nextHour != null) {
                val nextDayKor = when (nextHour.dayOfWeek.uppercase()) {
                    "MON" -> "월"; "TUE" -> "화"; "WED" -> "수"; "THU" -> "목"
                    "FRI" -> "금"; "SAT" -> "토"; "SUN" -> "일"; else -> nextHour.dayOfWeek
                }
                val open = parseLocalTime(nextHour.openTime)
                val close = parseLocalTime(nextHour.closeTime)
                val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                binding.textViewTime.text = if (open != null && close != null)
                    "${nextDayKor}요일 ${open.format(fmt)} - ${close.format(fmt)}"
                else "${nextDayKor}요일"
            } else {
                binding.textViewTime.text = "-"
            }
            binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            return
        }

        if (todayHour.regularHoliday) {
            binding.textViewIsWorking.text = "영업종료 · "
            binding.textViewTime.text = "정기휴무"
            binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            return
        }

        val open = parseLocalTime(todayHour.openTime)
        val close = if (todayHour.closeTime?.startsWith("24:00") == true || (todayHour.closeTime?.startsWith("00:00") == true && todayHour.openTime != "00:00:00")) {
            LocalTime.MAX
        } else {
            parseLocalTime(todayHour.closeTime)
        }

        if (open == null || close == null) {
            binding.textViewIsWorking.text = "정보 오류 · "
            binding.textViewTime.text = "-"
            binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            return
        }

        try {
            val now = LocalTime.now()

            // 영업 중인지 판단 (익일 마감 대응)
            val isWorking = if (close.isBefore(open)) {
                !now.isBefore(open) || now.isBefore(close)
            } else {
                !now.isBefore(open) && now.isBefore(close)
            }

            // 화면 표시용 (초 제외)
            val timeViewFormatter = DateTimeFormatter.ofPattern("HH:mm")
            binding.textViewTime.text = "${open.format(timeViewFormatter)} - ${if (close == LocalTime.MAX) "24:00" else close.format(timeViewFormatter)}"

            if (isWorking) {
                binding.textViewIsWorking.text = "영업중 · "
                binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            } else {
                binding.textViewIsWorking.text = "영업종료 · "
                binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }
        } catch (e: Exception) {
            Log.e("PlaceInfoHome", "Calculation error: ${e.message}")
            binding.textViewIsWorking.text = "정보 오류 · "
            binding.textViewTime.text = "-"
            binding.textViewIsWorking.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    private fun showFullSchedule(hours: List<OperatingHour>?) {
        binding.llOperatingHours.removeAllViews()
        if (hours.isNullOrEmpty()) return

        val dayOrder = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
        val dayKorMap = mapOf(
            "MON" to "월", "TUE" to "화", "WED" to "수", "THU" to "목",
            "FRI" to "금", "SAT" to "토", "SUN" to "일"
        )
        val fmt = DateTimeFormatter.ofPattern("HH:mm")

        // 요일 순서대로 정렬해서 표시
        val sorted = hours.sortedBy { dayOrder.indexOf(it.dayOfWeek.uppercase()) }

        // 오늘 요일 (강조 표시용)
        val todayEng = when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY    -> "MON"
            java.util.Calendar.TUESDAY   -> "TUE"
            java.util.Calendar.WEDNESDAY -> "WED"
            java.util.Calendar.THURSDAY  -> "THU"
            java.util.Calendar.FRIDAY    -> "FRI"
            java.util.Calendar.SATURDAY  -> "SAT"
            java.util.Calendar.SUNDAY    -> "SUN"
            else -> ""
        }

        sorted.forEach { hour ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 2, 0, 2)
            }

            val dayKor = dayKorMap[hour.dayOfWeek.uppercase()] ?: hour.dayOfWeek
            val isToday = hour.dayOfWeek.equals(todayEng, ignoreCase = true)

            val tvDay = android.widget.TextView(requireContext()).apply {
                text = "${dayKor}요일"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(
                    if (isToday) androidx.core.content.ContextCompat.getColor(requireContext(), R.color.orange)
                    else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
                )
                layoutParams = android.widget.LinearLayout.LayoutParams(120, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            // 정기휴무가 아닌데 시간 파싱 실패(null) 시 해당 행 생략
            val open = if (!hour.regularHoliday) parseLocalTime(hour.openTime) else null
            val close = if (!hour.regularHoliday) parseLocalTime(hour.closeTime) else null
            if (!hour.regularHoliday && (open == null || close == null)) return@forEach

            val tvTime = android.widget.TextView(requireContext()).apply {
                text = if (hour.regularHoliday) "정기휴무"
                      else "${open!!.format(fmt)} - ${close!!.format(fmt)}"
                textSize = 14f
                setTextColor(
                    if (isToday) androidx.core.content.ContextCompat.getColor(requireContext(), R.color.orange)
                    else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black)
                )
            }

            row.addView(tvDay)
            row.addView(tvTime)
            binding.llOperatingHours.addView(row)
        }
    }

    private fun parseLocalTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        val cleaned = timeStr.trim()
        val patterns = listOf("HH:mm:ss", "H:mm:ss", "HH:mm", "H:mm")
        for (pattern in patterns) {
            try {
                return LocalTime.parse(cleaned, DateTimeFormatter.ofPattern(pattern))
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun formatPhoneNumber(phone: String): String {
        var digits = phone.replace(Regex("\\D"), "")
        if (digits.isEmpty()) return phone

        if (!digits.startsWith("010")) {
            digits = "010$digits"
        }

        return when {
            digits.length == 10 -> {
                "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            digits.length >= 11 -> {
                val sub = digits.substring(0, 11)
                "${sub.substring(0, 3)}-${sub.substring(3, 7)}-${sub.substring(7)}"
            }
            else -> {
                if (digits.length > 3) {
                    "${digits.substring(0, 3)}-${digits.substring(3)}"
                } else {
                    digits
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
