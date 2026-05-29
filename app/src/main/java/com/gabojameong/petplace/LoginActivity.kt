package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.gabojameong.petplace.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import com.navercorp.nid.profile.domain.vo.NidProfile
import com.navercorp.nid.profile.util.NidProfileCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private val apiService = RetrofitClient.apiService
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("jwt_token", null)
        if (savedToken != null) {
            RetrofitClient.setToken(savedToken)
            finish()
            return
        }

        enableEdgeToEdge()
        supportActionBar?.hide()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.btnLogin.setOnClickListener { performLogin() }
        binding.btnSignIn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
        binding.btnKakao.setOnClickListener { startKakaoLogin() }
        binding.btnNaver.setOnClickListener { startNaverLogin() }
        binding.btnReturn.setOnClickListener { finish() }
    }

    private fun performLogin() {
        val loginId = binding.editTextPhone.text.toString().trim()
        val password = binding.editTextPw.text.toString().trim()

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(applicationContext, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.login(LoginRequest(loginId, password)).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()?.data
                    if (token != null) {
                        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        if (binding.checkBoxSaveId.isChecked) {
                            editor.putString("saved_id", loginId)
                        } else {
                            editor.remove("saved_id")
                        }
                        editor.apply()
                        
                        fetchUserInfoAndNavigate(token)
                    }
                } else {
                    val msg = RetrofitClient.parseErrorMessage(response)
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
                UserApiClient.instance.me { user, userError ->
                    if (user != null) {
                        val req = SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = token.accessToken,
                            providerId = user.id.toString(),
                        )
                        performSocialLogin(req, user.kakaoAccount?.profile?.nickname)
                    } else if (userError != null) {
                        Toast.makeText(applicationContext, "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
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

    private fun startNaverLogin() {
        val oauthLoginCallback = object : NidOAuthCallback {
            override fun onSuccess() {
                NidOAuth.getUserProfile(object : NidProfileCallback<NidProfile> {
                    override fun onSuccess(result: NidProfile) {
                        val profile = result.profile
                        val req = SocialLoginRequest(
                            provider = "NAVER",
                            accessToken = NidOAuth.getAccessToken() ?: "",
                            providerId = profile.id
                        )
                        performSocialLogin(req, profile.nickname)
                    }

                    override fun onFailure(errorCode: String, errorDesc: String) {
                        Toast.makeText(applicationContext, "네이버 프로필 조회 실패: $errorDesc", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                Toast.makeText(applicationContext, "네이버 로그인 실패: $errorDesc", Toast.LENGTH_SHORT).show()
            }
        }
        NidOAuth.requestLogin(this, oauthLoginCallback)
    }

    private fun performSocialLogin(req: SocialLoginRequest, nickname: String?) {
        apiService.socialLogin(req).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val jwtToken = response.body()?.data
                    if (jwtToken != null) {
                        fetchUserInfoAndNavigate(jwtToken)
                    }
                } else {
                    val errorMsg = RetrofitClient.parseErrorMessage(response)
                    if (errorMsg.contains("신규 소셜 회원")) {
                        val intent = Intent(this@LoginActivity, SignupActivity::class.java).apply {
                            putExtra("isSocial", true)
                            putExtra("provider", req.provider)
                            putExtra("accessToken", req.accessToken)
                            putExtra("providerId", req.providerId)
                            putExtra("nickname", nickname ?: "")
                        }
                        startActivity(intent)
                    } else {
                        Log.e("LoginActivity", "Social login failed: $errorMsg")
                        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(applicationContext, "서버 연결 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserInfoAndNavigate(token: String) {
        RetrofitClient.setToken(token)
        
        apiService.getProfile().enqueue(object : Callback<ApiResponse<UserProfileResponse>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileResponse>>, response: Response<ApiResponse<UserProfileResponse>>) {
                val profile = response.body()?.data
                if (response.isSuccessful && profile != null) {
                    val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("jwt_token", token)
                    editor.putLong("userId", profile.id)
                    editor.putString("nickname", profile.nickname)
                    editor.putString("userRole", profile.role) // role 저장
                    editor.apply()
                    
                    Toast.makeText(applicationContext, "${profile.nickname}님, 환영합니다!", Toast.LENGTH_SHORT).show()
                    finish()
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
}
