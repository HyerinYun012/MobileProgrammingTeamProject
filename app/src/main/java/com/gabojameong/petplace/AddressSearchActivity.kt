package com.gabojameong.petplace // 🚨 본인 패키지명 확인!

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class AddressSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 방금 수정한 XML 화면을 먼저 띄운다!
        setContentView(R.layout.activity_address_search)

        // 2. XML 안에 숨어있는 webView를 아이디로 찾아온다!
        val webView = findViewById<WebView>(R.id.webView)

        // 웹뷰 세팅
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.addJavascriptInterface(BridgeInterface(), "Android")

        webView.loadDataWithBaseURL("http://localhost/", getHtmlData(), "text/html", "UTF-8", null)
    }

    private fun getHtmlData(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0px;padding:0px;">
                <div id="layer" style="display:block;position:absolute;overflow:hidden;z-index:1;-webkit-overflow-scrolling:touch;width:100%;height:100%;"></div>
                <script src="https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
                <script>
                    // 🔥 [해결 3] 안드로이드가 재촉하지 않아도, 카카오 파일이 다 다운받아지면(onload) 스스로 창을 띄우게 변경!
                    window.onload = function() {
                        new daum.Postcode({
                            oncomplete: function(data) {
                                // data.x = 경도(longitude), data.y = 위도(latitude)
                                window.Android.processDATA(data.address, data.bname, data.x, data.y);
                            },
                            width: '100%',
                            height: '100%'
                        }).embed(document.getElementById('layer'));
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    inner class BridgeInterface {
        @JavascriptInterface
        fun processDATA(fullAddress: String, dongName: String, x: String, y: String) {
            val intent = Intent()
            intent.putExtra("address", fullAddress)
            intent.putExtra("dong", dongName)
            intent.putExtra("longitude", x.toDoubleOrNull() ?: 0.0)  // data.x = 경도
            intent.putExtra("latitude",  y.toDoubleOrNull() ?: 0.0)  // data.y = 위도
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}