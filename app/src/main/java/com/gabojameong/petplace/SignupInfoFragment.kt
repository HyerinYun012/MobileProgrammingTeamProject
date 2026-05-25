package com.gabojameong.petplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gabojameong.petplace.databinding.FragmentSingupInfoBinding
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
    private var isSocial: Boolean = false
    private var checkedIdDuplication = false
    private var checkedNicknameDuplication = false
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            role = it.getString("role")
            isSocial = it.getBoolean("isSocial", false)
        }
        checkedIdDuplication = false
        checkedNicknameDuplication = false
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
        
        if (isSocial) {
            // 소셜 가입 시 불필요한 입력 필드 숨기기
            binding.llName.visibility = View.GONE
            binding.llId.visibility = View.GONE
            binding.llPw.visibility = View.GONE
            binding.llPwCheck.visibility = View.GONE
            binding.btnIdCheck.visibility = View.GONE
            binding.textViewUserInfo.text = "소셜 계정 추가 정보 입력"
            
            // 전달받은 닉네임 설정
            //binding.editTextNickname.setText(arguments?.getString("nickname"))
            
            // ID/PW가 없으므로 체크 통과 처리
            checkedIdDuplication = true
        }

        if (role == "owner") {
            binding.llBusiness.visibility = View.VISIBLE
            binding.textViewBusiness.visibility = View.VISIBLE
        } else {
            binding.llBusiness.visibility = View.GONE
            binding.textViewBusiness.visibility = View.GONE
        }

        // 체크박스 상호 배제 로직
        binding.checkBoxAgree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkBoxDisagree.isChecked = false
        }
        binding.checkBoxDisagree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkBoxAgree.isChecked = false
        }

        binding.btnIdCheck.setOnClickListener {
            checkIdDuplication()
        }

        binding.btnNicknameCheck.setOnClickListener {
            checkNicknameDuplication()
        }

        binding.btnSubmit.setOnClickListener {
            performSignup()
        }
    }

    private fun checkIdDuplication() {
        val loginId = binding.editTextId.text.toString().trim()
        if (loginId.isEmpty()) {
            Toast.makeText(requireContext().applicationContext, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.checkId(loginId).enqueue(object : Callback<ApiResponse<Boolean>> {
            override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                if (response.isSuccessful) {
                    val isAvailable = response.body()?.data ?: false
                    if (isAvailable) {
                        checkedIdDuplication = true
                        Toast.makeText(requireContext().applicationContext, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext().applicationContext, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext().applicationContext, "중복 확인 실패: ${RetrofitClient.parseErrorMessage(response)}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkNicknameDuplication() {
        val nickname = binding.editTextNickname.text.toString().trim()
        if (nickname.isEmpty()) {
            Toast.makeText(requireContext().applicationContext, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.checkNickname(nickname).enqueue(object : Callback<ApiResponse<Boolean>> {
            override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                if (response.isSuccessful) {
                    val isAvailable = response.body()?.data ?: false
                    if (isAvailable) {
                        checkedNicknameDuplication = true
                        Toast.makeText(requireContext().applicationContext, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext().applicationContext, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext().applicationContext, "중복 확인 실패: ${RetrofitClient.parseErrorMessage(response)}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSignup() {
        val nickname = binding.editTextNickname.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()

        if (nickname.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext().applicationContext, "필수 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!checkedNicknameDuplication) {
            Toast.makeText(requireContext().applicationContext, "닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSocial) {
            val request = SocialSignupRequest(
                provider = arguments?.getString("provider") ?: "",
                accessToken = arguments?.getString("accessToken") ?: "",
                providerId = arguments?.getString("providerId") ?: "",
                nickname = nickname,
                phone = phone,
                role = role?.uppercase() ?: "CUSTOMER",
                marketingAgree = binding.checkBoxAgree.isChecked
            )
            apiService.socialSignup(request).enqueue(signupCallback)
            return
        }

        // 일반 회원가입 로직
        val name = binding.editTextName.text.toString().trim()
        val loginId = binding.editTextId.text.toString().trim()
        val password = binding.editTextPw.text.toString().trim()
        val passwordConfirm = binding.editTextPassword4.text.toString().trim()

        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext().applicationContext, "필수 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(requireContext().applicationContext, "비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!checkedIdDuplication) {
            Toast.makeText(requireContext().applicationContext, "아이디 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (role == "owner") {
            val businessNumber = binding.editTextBusinessNumber.text.toString().trim()
            val businessAddress = binding.editTextBusinessAdress.text.toString().trim()
            val businessName = binding.editTextBusinessName.text.toString().trim()
            if (businessNumber.isEmpty() || businessAddress.isEmpty()) {
                Toast.makeText(requireContext().applicationContext, "사업자 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            val ownerRequest = OwnerSignupRequest(
                loginId = loginId,
                password = password,
                passwordConfirm = passwordConfirm,
                name = name,
                nickname = nickname,
                phone = phone,
                email = email
            )

            apiService.signupOwner(ownerRequest).enqueue(signupCallback)
        } else {
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
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(requireContext().applicationContext, "자동 로그인 실패: $msg", Toast.LENGTH_LONG).show()
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
            region = mapRegionToEnum(addr),
            latitude = 0.0,
            longitude = 0.0,
            menus = emptyList(),
            operatingHours = emptyList()
        )

        val json = gson.toJson(resRequest)
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        apiService.registerRestaurant(requestBody, null).enqueue(object : Callback<ApiResponse<Long>> {
            override fun onResponse(call: Call<ApiResponse<Long>>, response: Response<ApiResponse<Long>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext().applicationContext, "가입 및 가게 등록 완료!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(requireContext().applicationContext, "가게 등록 실패: $msg", Toast.LENGTH_LONG).show()
                    navigateToLogin()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                navigateToLogin()
            }
        })
    }

    private fun mapRegionToEnum(addr: String): String {
        return when {
            addr.contains("거북섬") -> "GEOBUKSEOM"
            addr.contains("과림") -> "GWARIM"
            addr.contains("군자") -> "GUNJA"
            addr.contains("능곡") -> "NEUNGGOK"
            addr.contains("대야") -> "DAEYA"
            addr.contains("매화") -> "MAEHWA"
            addr.contains("목감") -> "MOKGAM"
            addr.contains("배곧") -> "BAEGON"
            addr.contains("신천") -> "SINCHEON"
            addr.contains("신현") -> "SINHYEON"
            addr.contains("연성") -> "YEONSEONG"
            addr.contains("월곶") -> "WOLGOT"
            addr.contains("은행") -> "EUNHAENG"
            addr.contains("장곡") -> "JANGGOK"
            addr.contains("정왕") -> "JEONGWANG"
            else -> "BAEGON"
        }
    }

    private val signupCallback = object : Callback<ApiResponse<Any>> {
        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
            if (response.isSuccessful && response.body()?.success == true) {
                if (role == "owner" && !isSocial) {
                    val loginId = binding.editTextId.text.toString().trim()
                    val pw = binding.editTextPw.text.toString().trim()
                    val bName = binding.editTextBusinessName.text.toString().trim()
                    val bAddr = binding.editTextBusinessAdress.text.toString().trim()
                    val bPhone = binding.editTextPhone.text.toString().trim()
                    val bNo = binding.editTextBusinessNumber.text.toString().trim()
                    autoLoginAndRegisterRestaurant(loginId, pw, bName, bAddr, bPhone, bNo)
                } else {
                    Toast.makeText(requireContext().applicationContext, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            } else {
                val msg = RetrofitClient.parseErrorMessage(response)
                Log.e("SignupCallback", "onResponse: $msg")
                Toast.makeText(requireContext().applicationContext, "가입 실패: $msg", Toast.LENGTH_LONG).show()
            }
        }
        override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
            Log.e("SignupCallback", "onFailure: ${t.message}", t)
            Toast.makeText(requireContext().applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToLogin() {
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
