package com.example.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.petplace.databinding.ActivityStoreTimeBottomSheetBinding

class StoreTimeBottomSheet(private val onTimeSelected: (String) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: ActivityStoreTimeBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityStoreTimeBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. '시' 버튼들 그룹화 및 단일 선택 로직
        val hourButtons = listOf(
            binding.rbHour1, binding.rbHour2, binding.rbHour3, binding.rbHour4,
            binding.rbHour5, binding.rbHour6, binding.rbHour7, binding.rbHour8,
            binding.rbHour9, binding.rbHour10, binding.rbHour11, binding.rbHour12
        )

        hourButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                // 하나를 클릭하면 나머지 버튼들은 모두 체크 해제
                hourButtons.forEach { it.isChecked = false }
                (it as RadioButton).isChecked = true
                binding.rbHoliday.isChecked = false // 시간 선택 시 '휴무' 해제
            }
        }

        // 2. '분' 버튼들 그룹화 및 단일 선택 로직
        val minuteButtons = listOf(binding.rbMin00, binding.rbMin30)

        minuteButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                minuteButtons.forEach { it.isChecked = false }
                (it as RadioButton).isChecked = true
                binding.rbHoliday.isChecked = false // 시간 선택 시 '휴무' 해제
            }
        }

        // 3. '휴무' 버튼 클릭 시 로직
        binding.rbHoliday.setOnClickListener {
            // 휴무를 누르면 시간, 분 버튼들 모두 체크 해제
            hourButtons.forEach { it.isChecked = false }
            minuteButtons.forEach { it.isChecked = false }
        }

        // 4. 적용 버튼 클릭 시 최종 텍스트 조합 로직
        binding.btnApplyTime.setOnClickListener {
            // '휴무'가 선택된 경우
            if (binding.rbHoliday.isChecked) {
                onTimeSelected("휴무")
                dismiss()
                return@setOnClickListener
            }

            // 오전/오후 판별 (R.id 대신 binding 변수의 id 속성 사용)
            val isAm = binding.rgAmpm.checkedRadioButtonId == binding.rbAm.id
            val amPmText = if (isAm) "오전" else "오후"

            // 선택된 시, 분 텍스트 가져오기 (아무것도 선택 안 했으면 기본값 "1", "00" 세팅)
            val selectedHour = hourButtons.firstOrNull { it.isChecked }?.text?.toString() ?: "1"
            val selectedMinute = minuteButtons.firstOrNull { it.isChecked }?.text?.toString() ?: "00"

            // 최종 텍스트 만들기 (예: "오전 09:30")
            // 시(hour)가 한 자리수면 앞에 0을 붙여 깔끔하게 표시 (예: "1" -> "01")
            val formattedHour = selectedHour.padStart(2, '0')
            val finalSelectedTime = "$amPmText $formattedHour:$selectedMinute"

            // 메인 액티비티로 데이터 전달 후 창 닫기
            onTimeSelected(finalSelectedTime)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}