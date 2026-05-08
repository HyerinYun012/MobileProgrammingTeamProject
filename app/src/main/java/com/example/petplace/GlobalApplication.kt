package com.example.petplace

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, "78e39d9b14d9db8581b78d89f1205fc6")
    }
}