package com.example.petplace

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
import com.kakao.sdk.user.UserApiClient


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKakao.setOnClickListener{
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        Log.d("KakaoLogin", "사용자가 로그인을 취소함")
                        Toast.makeText(this,"로그인을 취소하였습니다.",Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("KakaoLogin", "로그인 실패", error)
                        Toast.makeText(this,"로그인에 실패하였습니다.",Toast.LENGTH_SHORT).show()
                    }
                } else if (token != null) {
                    Log.i("KakaoLogin", "로그인 성공 ${token.accessToken}")
                    Toast.makeText(this,"로그인 성공",Toast.LENGTH_SHORT).show()
                    // 사용자 정보 요청
                    UserApiClient.instance.me { user, error ->
                        if (user != null) {
                            Log.i("KakaoLogin", "사용자 정보 요청 성공" +
                                    "\n회원번호: ${user.id}" +
                                    "\n이메일: ${user.kakaoAccount?.email}" +
                                    "\n닉네임: ${user.kakaoAccount?.profile?.nickname}")
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


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}