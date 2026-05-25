package com.gabojameong.petplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gabojameong.petplace.databinding.FragmentSignupRoleBinding

class SignupRoleFragment : Fragment() {

    private var _binding: FragmentSignupRoleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOwner.setOnClickListener {
            val fragment = SignupInfoFragment()
            // LoginActivity에서 넘어온 소셜 정보가 있다면 유지하면서 role 추가
            val bundle = if (arguments != null) Bundle(arguments) else Bundle()
            bundle.putString("role", "owner")
            fragment.arguments = bundle
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnCustomer.setOnClickListener {
            val fragment = SignupInfoFragment()
            val bundle = if (arguments != null) Bundle(arguments) else Bundle()
            bundle.putString("role", "customer")
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}