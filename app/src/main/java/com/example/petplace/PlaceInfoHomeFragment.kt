package com.example.petplace

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.petplace.databinding.FragmentPlaceInfoHomeBinding

class PlaceInfoHomeFragment : Fragment(R.layout.fragment_place_info_home) {
    private var _binding: FragmentPlaceInfoHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceInfoHomeBinding.bind(view)

        val restaurant = arguments?.getSerializable("restaurant", RestaurantResponse::class.java)
        restaurant?.let { initUI(it) }
    }

    private fun initUI(res: RestaurantResponse) {
        binding.textViewAddress.text = res.address
        binding.textViewPhone.text = res.phone

        binding.textViewTime.text = formatOperatingHours(res.operatingHours)

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

    private fun formatOperatingHours(hours: List<OperatingHour>?): String {
        if (hours.isNullOrEmpty()) return "정보 없음"
        
        return hours.joinToString("\n") { hour ->
            val day = when (hour.dayOfWeek) {
                "MON" -> "월"
                "TUE" -> "화"
                "WED" -> "수"
                "THU" -> "목"
                "FRI" -> "금"
                "SAT" -> "토"
                "SUN" -> "일"
                else -> hour.dayOfWeek
            }
            if (hour.regularHoliday) "$day: 정기휴무"
            else "$day: ${hour.openTime} - ${hour.closeTime}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
