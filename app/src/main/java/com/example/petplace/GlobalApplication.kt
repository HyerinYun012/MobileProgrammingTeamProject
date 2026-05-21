package com.example.petplace

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    companion object {
        lateinit var instance: GlobalApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        KakaoSdk.init(this, "78e39d9b14d9db8581b78d89f1205fc6")
    }
}
