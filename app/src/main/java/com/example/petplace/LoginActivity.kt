package com.example.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 자동 로그인 체크
        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("jwt_token", null)
        if (savedToken != null) {
            RetrofitClient.setToken(savedToken)
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        supportActionBar?.hide()
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { performLogin(binding) }
        binding.btnSignIn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
        binding.btnKakao.setOnClickListener { startKakaoLogin() }
        binding.btnReturn.setOnClickListener { finish() }
    }

    private fun performLogin(binding: ActivityLoginBinding) {
        val loginId = binding.editTextPhone.text.toString().trim()
        val password = binding.editTextPw.text.toString().trim()

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.login(LoginRequest(loginId, password)).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    val token = apiResponse.data
                    if (token != null) {
                        // 토큰 획득 성공 시 프로필 조회로 최종 검증
                        fetchUserInfoAndNavigate(token)
                    }
                } else {
                    val msg = apiResponse?.message ?: "로그인 실패 (ID/PW를 확인하세요)"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error == null && token != null) {
                UserApiClient.instance.me { user, _ ->
                    if (user != null) {
                        val req = SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = token.accessToken,
                            providerId = user.id.toString(),
                            nickname = user.kakaoAccount?.profile?.nickname ?: "사용자",
                            phone = user.kakaoAccount?.phoneNumber?.replace("+82 ", "0")?.replace("-", "") ?: "",
                            role = "CUSTOMER",
                            marketingAgree = false
                        )
                        
                        apiService.socialLogin(req).enqueue(object : Callback<ApiResponse<String>> {
                            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                                val res = response.body()
                                if (response.isSuccessful && res?.success == true && res.data != null) {
                                    fetchUserInfoAndNavigate(res.data)
                                } else {
                                    Toast.makeText(this@LoginActivity, "소셜 로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                                Toast.makeText(this@LoginActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    // 7-7: 명세의 getProfile()을 사용하여 토큰 유효성 최종 확인 및 정보 저장
    private fun fetchUserInfoAndNavigate(token: String) {
        // 인터셉터에서 사용할 토큰 설정
        RetrofitClient.setToken(token)
        
        apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileResponse>>, response: Response<ApiResponse<UserProfileResponse>>) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true && apiResponse.data != null) {
                    val profile = apiResponse.data
                    
                    // 안전하게 정보 저장
                    val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("jwt_token", token)
                        putLong("userId", profile.id)
                        putString("nickname", profile.nickname)
                        apply()
                    }
                    
                    Toast.makeText(this@LoginActivity, "${profile.nickname}님, 환영합니다!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // 토큰은 받았으나 프로필 조회 실패 시 (403 방지)
                    RetrofitClient.setToken(null)
                    Toast.makeText(this@LoginActivity, "사용자 인증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                RetrofitClient.setToken(null)
                Toast.makeText(this@LoginActivity, "사용자 정보 확인 중 네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToMain() {
        val intent = Intent(this, SearchActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
