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
    private lateinit var binding: ActivityLoginBinding

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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. 저장된 아이디 불러오기
        val savedId = sharedPref.getString("saved_id", null)
        if (savedId != null) {
            binding.editTextPhone.setText(savedId)
            binding.checkBoxSaveId.isChecked = true
        }

        binding.btnRetrieveId.setOnClickListener {
            val intent = Intent(this, FindIdActivity::class.java)
            startActivity(intent)
        }
        binding.btnRetrievePw.setOnClickListener {
            val intent = Intent(this, FindPwActivity::class.java)
            startActivity(intent)
        }
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
            Toast.makeText(applicationContext, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.login(LoginRequest(loginId, password)).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse?.success == true) {
                    val token = apiResponse.data
                    if (token != null) {
                        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                        if (binding.checkBoxSaveId.isChecked) {
                            sharedPref.edit().putString("saved_id", loginId).apply()
                        } else {
                            sharedPref.edit().remove("saved_id").apply()
                        }
                        
                        fetchUserInfoAndNavigate(token)
                    }
                } else {
                    val msg = apiResponse?.message ?: "로그인 실패 (ID/PW를 확인하세요)"
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(applicationContext, "서버와 통신할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startKakaoLogin() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error == null && token != null) {
                UserApiClient.instance.me { user, _ ->
                    if (user != null) {
                        //Log.d("KakaoLogin", "user : $user")
                        val req = SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = token.accessToken,
                            providerId = user.id.toString(),
                            //nickname = "",//user.kakaoAccount?.profile?.nickname ?: "사용자",
                            //phone = "",//user.kakaoAccount?.phoneNumber?.replace("+82 ", "0")?.replace("-", "") ?: "",
                            //role = "",//"CUSTOMER",
                            //marketingAgree = false
                        )//1.로그인 시도
                        //2. 로그인 실패시, 에러종류 확인. 회원가입이 안된거라면 회원가입으로 이동

                        apiService.socialLogin(req).enqueue(object : Callback<ApiResponse<String>> {
                            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                                val res = response.body()
                                if (response.isSuccessful && res?.success == true && res.data != null) {
                                    fetchUserInfoAndNavigate(res.data)
                                } else {
                                    val errorMsg = RetrofitClient.parseErrorMessage(response)
                                    Log.e("KakaoLogin", "response : $response, res : $res, accesstoken : ${token.accessToken}, error : $error, errormsg : ${errorMsg}",)
                                    Toast.makeText(applicationContext, "카카오 로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    if (response.code() == 401)
                                        Log.e("KakaoLogin","401 에러")
                                }
                            }
                            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                                Toast.makeText(applicationContext, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            } else if (error != null) {
                Toast.makeText(applicationContext, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun fetchUserInfoAndNavigate(token: String) {
        RetrofitClient.setToken(token)
        
        apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileResponse>>, response: Response<ApiResponse<UserProfileResponse>>) {
                val profile = response.body()?.data
                if (response.isSuccessful && profile != null) {
                    val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putString("jwt_token", token)
                        putLong("userId", profile.id)
                        putString("nickname", profile.nickname)
                        apply()
                    }
                    
                    Toast.makeText(applicationContext, "${profile.nickname}님, 환영합니다!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    RetrofitClient.setToken(null)
                    Toast.makeText(applicationContext, "사용자 인증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<UserProfileResponse>>, t: Throwable) {
                RetrofitClient.setToken(null)
                Toast.makeText(applicationContext, "사용자 정보 확인 중 네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
