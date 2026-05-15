package com.example.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.setFragmentResult
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PetBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.bottom_sheet_pet,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val recommendLayout = view.findViewById<FlexboxLayout>(R.id.recommendLayout)
        val recommendLayout2 = view.findViewById<FlexboxLayout>(R.id.recommendLayout2)
        val recommendLayout3 = view.findViewById<FlexboxLayout>(R.id.recommendLayout3)

        btnApply.setOnClickListener {
            val selectedPets = mutableListOf<String>()

            for (i in 0 until recommendLayout.childCount) {
                val child = recommendLayout.getChildAt(i)
                if (child is CheckBox && child.isChecked) {
                    selectedPets.add(child.text.toString())
                }
            }
            for (i in 0 until recommendLayout2.childCount) {
                val child = recommendLayout2.getChildAt(i)
                if (child is CheckBox && child.isChecked) {
                    selectedPets.add(child.text.toString())
                }
            }
            for (i in 0 until recommendLayout3.childCount) {
                val child = recommendLayout3.getChildAt(i)
                if (child is CheckBox && child.isChecked) {
                    selectedPets.add(child.text.toString())
                }
            }

            val result = Bundle()
            result.putStringArrayList("selected_pets", ArrayList(selectedPets))
            setFragmentResult("pet_selection", result)

            dismiss()
        }
    }
}
