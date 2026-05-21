package com.example.petplace

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://3.21.169.86:8080/"
    private var authToken: String? = null

    // 토큰 설정 함수
    fun setToken(token: String?) {
        authToken = token
    }

    // 모든 요청 헤더에 토큰을 자동으로 넣는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        
        authToken?.let {
            builder.header("Authorization", "Bearer $it")
        }
        
        chain.proceed(builder.build())
    }

    // 서버로부터 401 Unauthorized(토큰 만료 등) 응답을 받았을 때 실행되는 인증처리기
    private val tokenAuthenticator = Authenticator { _, _ ->
        synchronized(this) {
            if (authToken != null) {
                logout() // 인증 실패 시 공통 로그아웃 실행
            }
        }
        null
    }

    /**
     * 로그아웃 및 세션 초기화 공통 함수
     */
    fun logout() {
        authToken = null
        val context = GlobalApplication.instance

        // 1. 저장된 모든 로그인 정보 삭제
        context.getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE).edit {
            clear()
        }

        // 2. 로그인 화면으로 이동 (기존 화면 스택 제거)
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
