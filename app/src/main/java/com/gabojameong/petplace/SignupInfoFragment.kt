package com.gabojameong.petplace

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private var isAccountCreated = false // 추가: 회원가입 계정 생성 여부 플래그
    private val apiService = RetrofitClient.apiService
    private val gson = Gson()
    private var selectedDong: String = ""
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    private val addressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fullAddress = result.data?.getStringExtra("address") ?: ""
            val dong        = result.data?.getStringExtra("dong") ?: ""
            val lat         = result.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lng         = result.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            binding.editTextBusinessAdress.setText(fullAddress)
            selectedDong = dong
            if (lat != 0.0) selectedLat = lat
            if (lng != 0.0) selectedLng = lng
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            role = it.getString("role")
            isSocial = it.getBoolean("isSocial", false)
        }
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

            // ID/PW가 없으므로 체크 통과 처리
            checkedIdDuplication = true
        }

        if (role == "owner") {
            binding.llBusiness.visibility = View.VISIBLE
            binding.textViewBusiness.visibility = View.VISIBLE
            // 사업자 회원가입 시 기본 서비스 유형 선택
            binding.checkBoxRestaurant.isChecked = true
        } else {
            binding.llBusiness.visibility = View.GONE
            binding.textViewBusiness.visibility = View.GONE
        }

        // 마케팅 동의 체크박스 상호 배제 로직
        setupMutualExclusion(listOf(binding.checkBoxAgree, binding.checkBoxDisagree))

        // 서비스 유형 체크박스 상호 배제 로직 (식당/카페/기타)
        setupMutualExclusion(listOf(binding.checkBoxRestaurant, binding.checkBoxCafe, binding.checkBoxOthers), mandatory = (role == "owner"))

        binding.btnSearchAddress.setOnClickListener {
            val intent = Intent(requireContext(), AddressSearchActivity::class.java)
            addressLauncher.launch(intent)
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

        // 전화번호 자동 포맷팅 (010-xxxx-xxxx 고정)
        binding.editTextPhone.setText("010-")
        Selection.setSelection(binding.editTextPhone.text, 4)
        binding.editTextPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val prefix = "010-"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val input = s.toString()

                if (!input.startsWith(prefix)) {
                    s.replace(0, s.length, prefix)
                    binding.editTextPhone.setSelection(s.length)
                    isFormatting = false
                    return
                }

                val body = input.substring(prefix.length).replace("-", "")
                val digits = body.filter { it.isDigit() }.take(8)
                
                val formatted = StringBuilder(prefix)
                for (i in digits.indices) {
                    if (i == 4) formatted.append("-")
                    formatted.append(digits[i])
                }

                val finalStr = formatted.toString()
                if (input != finalStr) {
                    s.replace(0, s.length, finalStr)
                    binding.editTextPhone.setSelection(s.length)
                }

                isFormatting = false
            }
        })

        // 사업자 등록번호 자동 포맷팅 (xxx-xx-xxxxx)
        binding.editTextBusinessNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val digits = s.toString().replace("-", "").filter { it.isDigit() }.take(10)
                
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    formatted.append(digits[i])
                    if (i == 2 && digits.length > 3) {
                        formatted.append("-")
                    } else if (i == 4 && digits.length > 5) {
                        formatted.append("-")
                    }
                }

                val finalStr = formatted.toString()
                if (s.toString() != finalStr) {
                    s.replace(0, s.length, finalStr)
                    binding.editTextBusinessNumber.setSelection(s.length)
                }

                isFormatting = false
            }
        })
    }

    private fun setupMutualExclusion(checkboxes: List<CheckBox>, mandatory: Boolean = false) {
        checkboxes.forEach { current ->
            current.setOnClickListener {
                if (current.isChecked) {
                    checkboxes.filter { it != current }.forEach { it.isChecked = false }
                } else if (mandatory) {
                    current.isChecked = true
                }
            }
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
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(requireContext().applicationContext, "중복 확인 실패: $msg", Toast.LENGTH_SHORT).show()
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
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(requireContext().applicationContext, "중복 확인 실패: $msg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSignup() {
        val nickname = binding.editTextNickname.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().replace("-", "")
        val email = binding.editTextEmail.text.toString().trim()

        if (nickname.isEmpty() || phone.length < 11 || email.isEmpty()) {
            Toast.makeText(requireContext().applicationContext, "필수 정보를 올바르게 입력해주세요. (전화번호 11자리)", Toast.LENGTH_SHORT).show()
            return
        }

        if (!checkedNicknameDuplication) {
            Toast.makeText(requireContext().applicationContext, "닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (role == "owner") {
            val businessNumber = binding.editTextBusinessNumber.text.toString().trim()
            val businessAddress = binding.editTextBusinessAdress.text.toString().trim()
            val businessName = binding.editTextBusinessName.text.toString().trim()
            
            if (businessNumber.isEmpty() || businessAddress.isEmpty() || businessName.isEmpty()) {
                Toast.makeText(requireContext().applicationContext, "사업자 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!binding.checkBoxRestaurant.isChecked && !binding.checkBoxCafe.isChecked && !binding.checkBoxOthers.isChecked) {
                Toast.makeText(requireContext().applicationContext, "서비스 유형을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            // 추가: 계정이 이미 생성되었다면 바로 가게 등록 단계로 진행 (Opt 2)
            if (isAccountCreated) {
                proceedToOwnerStep()
                return
            }
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

        val request = if (role == "owner") {
            OwnerSignupRequest(
                loginId = loginId,
                password = password,
                passwordConfirm = passwordConfirm,
                name = name,
                nickname = nickname,
                phone = phone,
                email = email
            )
        } else {
            CustomerSignupRequest(
                name = name,
                loginId = loginId,
                password = password,
                passwordConfirm = passwordConfirm,
                nickname = nickname,
                phone = phone,
                email = email
            )
        }

        if (role == "owner") {
            apiService.signupOwner(request as OwnerSignupRequest).enqueue(signupCallback)
        } else {
            apiService.signupCustomer(request as CustomerSignupRequest).enqueue(signupCallback)
        }
    }

    // 추가: 사장님 가입 후 다음 단계(로그인 및 가게등록)를 통합 수행하는 함수
    private fun proceedToOwnerStep() {
        val bName = binding.editTextBusinessName.text.toString().trim()
        val bAddr = binding.editTextBusinessAdress.text.toString().trim()
        val bPhone = binding.editTextPhone.text.toString().replace("-", "")
        val bNo = binding.editTextBusinessNumber.text.toString().trim()

        if (isSocial) {
            val provider = arguments?.getString("provider") ?: ""
            val accessToken = arguments?.getString("accessToken") ?: ""
            val providerId = arguments?.getString("providerId") ?: ""
            autoSocialLoginAndRegisterRestaurant(provider, accessToken, providerId, bName, bAddr, bPhone, bNo)
        } else {
            val loginId = binding.editTextId.text.toString().trim()
            val pw = binding.editTextPw.text.toString().trim()
            autoLoginAndRegisterRestaurant(loginId, pw, bName, bAddr, bPhone, bNo)
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
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "로그인 네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun autoSocialLoginAndRegisterRestaurant(provider: String, token: String, id: String, name: String, addr: String, phone: String, bNo: String) {
        val req = SocialLoginRequest(provider, token, id)
        apiService.socialLogin(req).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                val jwt = response.body()?.data
                if (response.isSuccessful && jwt != null) {
                    RetrofitClient.setToken(jwt)
                    registerRestaurantAfterSignup(name, addr, phone, bNo)
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
                    Toast.makeText(requireContext().applicationContext, "소셜 자동 로그인 실패: $msg", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "소셜 로그인 네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerRestaurantAfterSignup(name: String, addr: String, phone: String, bNo: String) {
        val category = when {
            binding.checkBoxRestaurant.isChecked -> "RESTAURANT"
            binding.checkBoxCafe.isChecked -> "CAFE"
            else -> "OTHERS"
        }

        val resRequest = RestaurantRequest(
            name = name,
            address = addr,
            phone = phone,
            businessNo = bNo,
            category = category,
            region = mapRegionToEnum(selectedDong),
            latitude = selectedLat,
            longitude = selectedLng,
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
                    Toast.makeText(requireContext().applicationContext, "가게 등록 실패: $msg\n정보를 확인하고 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Long>>, t: Throwable) {
                Toast.makeText(requireContext().applicationContext, "가게 등록 네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mapRegionToEnum(dong: String): String {
        return when {
            dong.contains("거북섬") -> "GEOBUKSEOM"
            dong.contains("과림") -> "GWARIM"
            dong.contains("군자") -> "GUNJA"
            dong.contains("능곡") -> "NEUNGGOK"
            dong.contains("대야") -> "DAEYA"
            dong.contains("매화") -> "MAEHWA"
            dong.contains("목감") -> "MOKGAM"
            dong.contains("배곧") -> "BAEGON"
            dong.contains("신천") -> "SINCHEON"
            dong.contains("신현") -> "SINHYEON"
            dong.contains("연성") -> "YEONSEONG"
            dong.contains("월곶") -> "WOLGOT"
            dong.contains("은행") -> "EUNHAENG"
            dong.contains("장곡") -> "JANGGOK"
            dong.contains("정왕") -> "JEONGWANG"
            else -> "ETC"
        }
    }

    private val signupCallback = object : Callback<ApiResponse<Any>> {
        override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
            if (response.isSuccessful && response.body()?.success == true) {
                isAccountCreated = true // 계정 생성 성공 표시
                if (role == "owner") {
                    proceedToOwnerStep()
                } else {
                    Toast.makeText(requireContext().applicationContext, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            } else {
                val msg = RetrofitClient.parseErrorMessage(response)
                // 이미 존재하는 계정인 경우, 사장님이라면 가게 등록 단계로 진행
                if (role == "owner" && (msg.contains("이미") || msg.contains("중복") || msg.contains("exists"))) {
                    isAccountCreated = true
                    Toast.makeText(requireContext().applicationContext, "이미 가입된 계정입니다. 가게 등록을 진행합니다.", Toast.LENGTH_SHORT).show()
                    proceedToOwnerStep()
                } else {
                    Toast.makeText(requireContext().applicationContext, "가입 실패: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
        override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
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
