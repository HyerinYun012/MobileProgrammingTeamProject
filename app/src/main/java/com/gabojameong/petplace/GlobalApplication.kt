package com.gabojameong.petplace

import android.app.Application
import android.content.Context
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NidOAuth

class GlobalApplication : Application() {
    companion object {
        lateinit var instance: GlobalApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 카카오 SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_KEY)
        // 네이버 SDK 초기화
        NidOAuth.initialize(this, BuildConfig.NAVER_CLIENT_ID, BuildConfig.NAVER_CLIENT_SECRET, "펫플레이스")

        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("jwt_token", null)
        RetrofitClient.setToken(savedToken)
    }
}
