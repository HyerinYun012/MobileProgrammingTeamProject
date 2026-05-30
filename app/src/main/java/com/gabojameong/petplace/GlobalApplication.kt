package com.gabojameong.petplace

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMapSdk
import com.navercorp.nid.NidOAuth

class GlobalApplication : Application() {

    companion object {
        lateinit var instance: GlobalApplication
            private set

        //로딩 오버레이 부착용
        var currentActivity: Activity? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_KEY)
        NidOAuth.initialize(this, BuildConfig.NAVER_CLIENT_ID, BuildConfig.NAVER_CLIENT_SECRET, "펫플레이스")
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_MAP_CLIENT_ID)

        val sharedPref = getSharedPreferences("PetPlacePref", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("jwt_token", null)
        RetrofitClient.setToken(savedToken)

        //로딩 오버레이 자동 부착에 사용
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity)  { currentActivity = activity }
            override fun onActivityPaused(activity: Activity)   { currentActivity = null }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity)  {}
            override fun onActivityStopped(activity: Activity)  {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
