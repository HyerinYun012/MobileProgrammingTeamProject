package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
        restaurant?.let { initUI(it) }
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

        // 영업 상태 및 오늘 시간 업데이트
        updateWorkingStatus(res.operatingHours)

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
