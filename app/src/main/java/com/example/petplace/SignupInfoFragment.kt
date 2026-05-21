package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.petplace.databinding.FragmentSingupInfoBinding
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupInfoFragment : Fragment() {

    private var _binding: FragmentSingupInfoBinding? = null
    private val binding get() = _binding!!
    private var role: String? = null
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

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

        if (role == "owner") {
            binding.llBusiness.visibility = View.VISIBLE
            binding.textViewBusiness.visibility = View.VISIBLE
        } else {
            binding.llBusiness.visibility = View.GONE
            binding.textViewBusiness.visibility = View.GONE
        }

        binding.editTextName.visibility = View.VISIBLE

        binding.btnSubmit.setOnClickListener {
            performSignup()
        }
    }

    private fun performSignup() {
        val name = binding.editTextName.text.toString().trim()
        val loginId = binding.editTextId.text.toString().trim()
        val password = binding.editTextPw.text.toString().trim()
        val passwordConfirm = binding.editTextPassword4.text.toString().trim()
        val nickname = binding.editTextNickname.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()

        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || 
            nickname.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "필수 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(requireContext(), "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (role == "owner") {
            val businessNumber = binding.editTextBusinessNumber.text.toString().trim()
            val businessAddress = binding.editTextBusinessAdress.text.toString().trim()
            
            if (businessNumber.isEmpty() || businessAddress.isEmpty()) {
                Toast.makeText(requireContext(), "사업자 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            // 7-7: 명세 동기화 - marketingAgree 제거
            val ownerRequest = OwnerSignupRequest(
                loginId = loginId,
                password = password,
                passwordConfirm = passwordConfirm,
                name = name,
                nickname = nickname,
                phone = phone,
                email = email
            )

            apiService.signupOwner(ownerRequest).enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        autoLoginAndRegisterRestaurant(loginId, password, name, businessAddress, phone, businessNumber)
                    } else {
                        val msg = parseErrorMessage(response)
                        Toast.makeText(requireContext(), "가입 실패: $msg", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    Toast.makeText(requireContext(), "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // 7-7: 명세 동기화 - marketingAgree 제거
            val request = CustomerSignupRequest(
                name = name,
                loginId = loginId,
                password = password,
                passwordConfirm = passwordConfirm,
                nickname = nickname,
                phone = phone,
                email = email
            )
            apiService.signupCustomer(request).enqueue(signupCallback)
        }
    }

    private fun autoLoginAndRegisterRestaurant(loginId: String, pw: String, name: String, addr: String, phone: String, bNo: String) {
        apiService.login(LoginRequest(loginId, pw)).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                val token = response.body()?.data
                if (response.isSuccessful && token != null) {
                    RetrofitClient.setToken(token)
                    registerRestaurantAfterSignup(name, addr, phone, bNo)
                } else {
                    val msg = parseErrorMessage(response)
                    Toast.makeText(requireContext(), "자동 로그인 실패: $msg", Toast.LENGTH_LONG).show()
                    navigateToLogin()
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                navigateToLogin()
            }
        })
    }

    private fun registerRestaurantAfterSignup(name: String, addr: String, phone: String, bNo: String) {
        val category = when {
            binding.checkBoxRestaurant.isChecked -> "RESTAURANT"
            binding.checkBoxCafe.isChecked -> "CAFE"
            else -> "RESTAURANT"
        }

        val resRequest = RestaurantRequest(
            name = name,
            address = addr,
            phone = phone,
            businessNo = bNo,
            category = category,
            region = if (addr.contains("시흥")) "JEONGWANG" else "BAEGON",
            latitude = 37.34,
            longitude = 126.73,
            menus = emptyList(),
            operatingHours = emptyList()
        )

        val json = gson.toJson(resRequest)
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        apiService.registerRestaurant(requestBody, null).enqueue(object : Callback<ApiResponse<Long>> {
            override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "가입 및 가게 등록 완료!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    val msg = parseErrorMessage(response)
                    Toast.makeText(requireContext(), "가게 등록 실패: $msg", Toast.LENGTH_LONG).show()
                    navigateToLogin()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                navigateToLogin()
            }
        })
    }

    private val signupCallback = object : Callback<ApiResponse<Any>> {
        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
            if (response.isSuccessful && response.body()?.success == true) {
                Toast.makeText(requireContext(), "회원가입 성공!", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            } else {
                val msg = parseErrorMessage(response)
                Log.e("SignupCallback", "onResponse: $msg")
                Toast.makeText(requireContext(), "가입 실패: $msg", Toast.LENGTH_LONG).show()
            }
        }
        override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
            Log.e("SignupCallback", "onFailure: ${t.message}", t)
            Toast.makeText(requireContext(), "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val errorBodyString = response.errorBody()?.string()
        return response.body()?.let {
            if (it is ApiResponse<*>) it.message else null
        } ?: errorBodyString?.let {
            try {
                val errorRes = gson.fromJson(it, ApiResponse::class.java)
                errorRes.message ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "알 수 없는 오류 (상태 코드: ${response.code()})"
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
