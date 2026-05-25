package com.gabojameong.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.setFragmentResult
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RegionBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.bottom_sheet_region,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val recommendLayout = view.findViewById<FlexboxLayout>(R.id.recommendLayout)
        
        btnApply.setOnClickListener {
            val selectedRegions = mutableListOf<String>()
            
            // FlexboxLayout 내의 모든 CheckBox를 확인하여 체크된 항목 수집
            for (i in 0 until recommendLayout.childCount) {
                val child = recommendLayout.getChildAt(i)
                if (child is CheckBox && child.isChecked) {
                    selectedRegions.add(child.text.toString())
                }
            }

            val result = Bundle()
            result.putStringArrayList("selected_regions", ArrayList(selectedRegions))
            setFragmentResult("region_selection", result)

            dismiss()
        }
    }
}
