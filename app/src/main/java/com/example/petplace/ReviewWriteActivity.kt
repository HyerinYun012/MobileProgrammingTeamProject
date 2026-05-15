package com.example.petplace

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ReviewWriteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_write)

        // 상단 뒤로가기 버튼
        val btnBack = findViewById<ImageView>(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }

        // 리뷰 등록하기 버튼 찾아오기
        val btnSubmitReview = findViewById<ImageView>(R.id.btn_submit_review)

        // 버튼을 누르면 커스텀 팝업창 띄우기
        btnSubmitReview.setOnClickListener {
            showSuccessDialog()
        }
    }

    // 팝업창을 만들고 동작시키는 함수
    private fun showSuccessDialog() {
        // 다이얼로그(팝업창) 생성
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // 기본 타이틀 제거
        dialog.setContentView(R.layout.dialog_review_success) // 아까 만든 디자인 연결

        // 팝업창 뒤의 기본 배경을 투명하게 설정
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 팝업창 안의 확인 버튼 찾아오기
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_confirm)

        // 확인 버튼을 눌렀을 때의 동작
        btnConfirm.setOnClickListener {
            dialog.dismiss() // 팝업창 닫기
            finish()         // 현재 화면 종료하고 이전 화면으로 돌아가기
        }

        // 팝업창 화면에 띄우기
        dialog.show()
    }
}