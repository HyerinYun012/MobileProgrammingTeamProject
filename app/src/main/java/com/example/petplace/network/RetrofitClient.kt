package com.example.petplace.network

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.example.petplace.GlobalApplication
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.java
import com.example.petplace.TempTestActivity

object RetrofitClient {
    private const val BASE_URL = "http://3.21.169.86:8080/"
    private var authToken: String? = null
    private val gson = Gson()

    // 토큰 설정 함수
    fun setToken(token: String?) {
        Log.d("RetrofitClient", "Setting token: $token")
        authToken = token
    }

    // 모든 요청 헤더에 토큰을 자동으로 넣는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // 🔥 핵심 방어막: 이미 요청에 Authorization 헤더가 붙어있는지 확인!
        val hasAuthHeader = original.header("Authorization") != null

        // 🚨 기존에 직접 넣은 토큰 헤더가 '없을 때만' 로그인된 기본 토큰을 자동으로 붙여줌!
        if (!hasAuthHeader) {
            authToken?.let {
                val cleanToken = it.replace("\n", "").replace("\r", "").trim()
                builder.header("Authorization", "Bearer $cleanToken")
            }
        }

        builder.header("Accept", "application/json")
        builder.header("User-Agent", "PetPlace-Android")

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

        // 2. 메인 화면으로 이동
        val intent = Intent(context, TempTestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    /**
     * Retrofit 응답에서 에러 메시지를 추출하는 공통 함수
     */
    fun parseErrorMessage(response: Response<*>): String {
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // 타임아웃을 넉넉하게 늘림
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        // 특정 서버에서 HTTP/2 이슈가 있을 때 HTTP/1.1을 강제하기도 하지만, 
        // 오히려 역효과가 날 수 있으므로 기본 설정을 따르거나 안정적인 설정을 유지합니다.
        // 여기서는 명시적인 프로토콜 제한을 해제하여 OkHttp가 최선의 프로토콜을 선택하게 합니다.
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
