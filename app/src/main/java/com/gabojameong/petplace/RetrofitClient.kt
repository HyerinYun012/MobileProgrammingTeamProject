package com.gabojameong.petplace

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
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

object RetrofitClient {
    private const val BASE_URL = "http://3.21.169.86:8080/"
    private var authToken: String? = null
    private val gson = Gson()

    fun setToken(token: String?) {
        Log.d("RetrofitClient", "Setting token: $token")
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        
        authToken?.let {
            // 토큰 내부나 양 끝의 줄바꿈/공백 제거 (헤더 손상 방지)
            val cleanToken = it.replace("\n", "").replace("\r", "").trim()
            builder.header("Authorization", "Bearer $cleanToken")
        }
        
        builder.header("Accept", "application/json")
        builder.header("User-Agent", "PetPlace-Android")
        
        chain.proceed(builder.build())
    }

    // 서버로부터 401 Unauthorized(토큰 만료 등) 응답을 받았을 때 실행되는 인증처리기
    private val tokenAuthenticator = Authenticator { _, _ ->
        synchronized(this) {
            if (authToken != null) {
                logout()
            }
        }
        null
    }

    fun logout() {
        authToken = null
        val context = GlobalApplication.instance

        //저장된 모든 로그인 정보 삭제
        context.getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE).edit {
            clear()
        }

        //메인 화면으로 이동
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(LoadingInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
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
