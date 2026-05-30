package com.gabojameong.petplace

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicInteger

/**
 * OkHttp 인터셉터 — 동시 요청 수를 카운팅해서 로딩 오버레이를 자동으로 표시/제거.
 * GlobalApplication.currentActivity 의 decorView에 오버레이를 부착하므로
 * 어떤 Activity도 수정할 필요 없음.
 */
object LoadingInterceptor : Interceptor {

    private val activeRequests = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        if (activeRequests.getAndIncrement() == 0) {
            mainHandler.post { showOverlay() }
        }
        return try {
            chain.proceed(chain.request())
        } finally {
            if (activeRequests.decrementAndGet() == 0) {
                mainHandler.post { hideOverlay() }
            }
        }
    }

    private fun showOverlay() {
        val activity = GlobalApplication.currentActivity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (overlayView != null) return  // 이미 표시 중

        val root = activity.window.decorView as? FrameLayout ?: return

        val overlay = LayoutInflater.from(activity)
            .inflate(R.layout.overlay_loading, root, false)
        root.addView(overlay)
        overlayView = overlay
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        val activity = GlobalApplication.currentActivity

        try {
            val root = (activity?.window?.decorView as? FrameLayout)
                ?: (view.parent as? FrameLayout)
            root?.removeView(view)
        } catch (e: Exception) { /* Activity 이미 종료된 경우 무시 */ }

        overlayView = null
    }
}
