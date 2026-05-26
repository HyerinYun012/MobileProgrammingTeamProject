package com.example.petplace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityInquiryDetailBinding

class InquiryDetailActivity : AppCompatActivity() {

    // ViewBinding 객체 선언
    private lateinit var binding: ActivityInquiryDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityInquiryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼 이벤트 처리
        binding.btnBack.setOnClickListener { finish() }

        // Intent로부터 데이터 수신 (Null 방지 기본값 설정)
        val category = intent.getStringExtra("CATEGORY") ?: "분류 없음"
        val email = intent.getStringExtra("EMAIL") ?: "이메일 없음"
        val content = intent.getStringExtra("CONTENT") ?: "내용 없음"

        // 수신된 데이터를 화면(TextView)에 매핑
        binding.tvInquiryCategory.text = category
        binding.tvInquiryEmail.text = email
        binding.tvInquiryContent.text = content
    }
}