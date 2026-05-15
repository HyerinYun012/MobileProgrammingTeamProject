package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.petplace.RetrofitClient.apiService
import com.example.petplace.databinding.FragmentSingupInfoBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupInfoFragment : Fragment() {

    private var _binding: FragmentSingupInfoBinding? = null
    private val binding get() = _binding!!
    private var role: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = arguments?.getString("role")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSingupInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 역할에 따른 UI 노출 설정
        if (role == "owner") {
            binding.llBusiness.visibility = View.VISIBLE
            binding.textViewBusiness.visibility = View.VISIBLE
        } else {
            binding.llBusiness.visibility = View.GONE
            binding.textViewBusiness.visibility = View.GONE
        }

        binding.btnSubmit.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val loginId = binding.editTextId.text.toString().trim()
            val password = binding.editTextPw.text.toString().trim()
            val passwordConfirm = binding.editTextPassword4.text.toString().trim()
            val nickname = binding.editTextNickname.text.toString().trim()
            val phone = binding.editTextPhone.text.toString().trim()
            val marketingAgree = binding.checkBoxAgree.isChecked

            // 공통 유효성 검사
            if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || nickname.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(requireContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.checkBoxAgree.isChecked && !binding.checkBoxDisagree.isChecked) {
                Toast.makeText(requireContext(), "마케팅 동의 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "owner") {
                // 사장님 회원가입 로직
                val businessNumber = binding.editTextBusinessNumber.text.toString().trim()
                val businessAddress = binding.editTextBusinessAdress.text.toString().trim()

                if (businessNumber.isEmpty() || businessAddress.isEmpty()) {
                    Toast.makeText(requireContext(), "사업자 정보(번호, 주소)를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val signupRequest = SignupOwnerRequest(
                    name = name,
                    loginId = loginId,
                    password = password,
                    passwordCheck = passwordConfirm,
                    nickname = nickname,
                    phone = phone,
                    businessNo = businessNumber,
                    businessAddress = businessAddress,
                    marketingAgree = marketingAgree
                )

                apiService.signupOwner(signupRequest).enqueue(object : Callback<SignUpOwnerResponse> {
                    override fun onResponse(call: Call<SignUpOwnerResponse>, response: Response<SignUpOwnerResponse>) {
                        handleSignupResponse(response.isSuccessful)
                    }

                    override fun onFailure(call: Call<SignUpOwnerResponse>, t: Throwable) {
                        handleNetworkError(t)
                    }
                })

            } else {
                // 일반 고객 회원가입 로직
                val signupRequest = SignupCustomerRequest(
                    name = name,
                    loginId = loginId,
                    password = password,
                    passwordCheck = passwordConfirm,
                    nickname = nickname,
                    phone = phone
                )

                apiService.signupCustomer(signupRequest).enqueue(object : Callback<SignUpCustomerResponse> {
                    override fun onResponse(call: Call<SignUpCustomerResponse>, response: Response<SignUpCustomerResponse>) {
                        handleSignupResponse(response.isSuccessful)
                    }

                    override fun onFailure(call: Call<SignUpCustomerResponse>, t: Throwable) {
                        handleNetworkError(t)
                    }
                })
            }
        }
    }

    private fun handleSignupResponse(isSuccessful: Boolean) {
        if (isSuccessful) {
            Toast.makeText(requireContext(), "회원가입에 성공하였습니다. 로그인해주세요.", Toast.LENGTH_SHORT).show()
            // 회원가입 성공 시 로그인 화면으로 이동
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "회원가입 실패. 아이디 중복 등을 확인해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNetworkError(t: Throwable) {
        Log.e("Signup", "Network error: ${t.message}")
        Toast.makeText(requireContext(), "서버와의 통신에 실패하였습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}