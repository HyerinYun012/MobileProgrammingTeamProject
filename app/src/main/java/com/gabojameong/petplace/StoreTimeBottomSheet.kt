package com.gabojameong.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gabojameong.petplace.databinding.ActivityStoreTimeBottomSheetBinding

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

        val hourButtons = listOf(
            binding.rbHour1, binding.rbHour2, binding.rbHour3, binding.rbHour4,
            binding.rbHour5, binding.rbHour6, binding.rbHour7, binding.rbHour8,
            binding.rbHour9, binding.rbHour10, binding.rbHour11, binding.rbHour12
        )

        hourButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                hourButtons.forEach { it.isChecked = false }
                (it as RadioButton).isChecked = true
                binding.rbHoliday.isChecked = false
            }
        }

        val minuteButtons = listOf(binding.rbMin00, binding.rbMin30)

        minuteButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                minuteButtons.forEach { it.isChecked = false }
                (it as RadioButton).isChecked = true
                binding.rbHoliday.isChecked = false
            }
        }

        binding.rbHoliday.setOnClickListener {
            hourButtons.forEach { it.isChecked = false }
            minuteButtons.forEach { it.isChecked = false }
        }

        binding.btnApplyTime.setOnClickListener {
            if (binding.rbHoliday.isChecked) {
                onTimeSelected("휴무")
                dismiss()
                return@setOnClickListener
            }

            val isAm = binding.rgAmpm.checkedRadioButtonId == binding.rbAm.id
            val amPmText = if (isAm) "오전" else "오후"

            val selectedHour = hourButtons.firstOrNull { it.isChecked }?.text?.toString() ?: "1"
            val selectedMinute = minuteButtons.firstOrNull { it.isChecked }?.text?.toString() ?: "00"

            val formattedHour = selectedHour.padStart(2, '0')
            val finalSelectedTime = "$amPmText $formattedHour:$selectedMinute"

            onTimeSelected(finalSelectedTime)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
