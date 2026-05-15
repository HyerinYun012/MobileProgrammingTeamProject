package com.example.petplace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petplace.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {
    private val keyHash by lazy {
        Utility.getKeyHash(this)
    }

    private val apiService = RetrofitClient.apiService

    private val kakaoCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                Log.d("KakaoLogin", "사용자의 로그인 취소")
                Toast.makeText(this, "로그인을 취소하였습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("KakaoLogin", "카카오 로그인 실패. 키해시: $keyHash", error)
                Toast.makeText(this, "카카오 로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        } else if (token != null) {
            Log.i("KakaoLogin", "카카오 로그인 성공 ${token.accessToken}")
            Toast.makeText(this, "카카오 로그인 성공", Toast.LENGTH_SHORT).show()
            UserApiClient.instance.me { user, error ->
                if (user != null) {
                    val loginRequest = KakaoLoginRequest(
                        additionalProp1 = user.id.toString(),
                        additionalProp2 = user.kakaoAccount?.email?:"",
                        additionalProp3 = user.kakaoAccount?.profile?.nickname?:""
                    )

                    apiService.kakaoLogin(loginRequest).enqueue(object : Callback<KakaoLoginResponse> {
                        override fun onResponse(call: Call<KakaoLoginResponse>, response: Response<KakaoLoginResponse>) {
                            if (response.isSuccessful) {
                                Log.d("Kakao login", "서버 통신 성공: ${response.body()}")
                                Log.d("Kakao login",
                                    "${user.kakaoAccount?.gender} ${user.kakaoAccount?.email} ${user.kakaoAccount?.profile?.nickname}")
                            } else {
                                Log.e("Kakao login", "서버 응답 에러: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<KakaoLoginResponse>, t: Throwable) {
                            Log.e("Kakao login", "네트워크 에러: ${t.message}")
                        }
                    })
                }
            }
        }
    }

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(binding.root)
        Log.d("KeyHash", keyHash)

        binding.btnReturn.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        binding.btnLogin.setOnClickListener {
            val loginId = binding.editTextPhone.text.toString().trim()
            val password = binding.editTextPw.text.toString().trim()

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(loginId = loginId, password = password)
            apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        Log.d("Login", "로그인 성공: ${response.body()}")
                        Toast.makeText(this@LoginActivity, "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show()
                        
                        // 로그인 성공 후 메인 화면(SearchActivity 등)으로 이동
                        val intent = Intent(this@LoginActivity, SearchActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Log.e("Login", "로그인 실패: ${response.code()}")
                        Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("Login", "네트워크 에러: ${t.message}")
                    Toast.makeText(this@LoginActivity, "서버와의 통신에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnSignIn.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
        binding.btnKakao.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this, callback = kakaoCallback)
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this, callback = kakaoCallback)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
