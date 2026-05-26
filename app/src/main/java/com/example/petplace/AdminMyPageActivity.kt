package com.example.petplace

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityAdminMyPageBinding

class AdminMyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMyPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 뒤로가기 버튼 클릭 시 현재 마이페이지 닫기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 2. 신고 접수 관리 메뉴 클릭 시 ReportManageActivity로 이동
        binding.layoutMenuReport.setOnClickListener {
            val intent = Intent(this, ReportManageActivity::class.java)
            startActivity(intent)
        }

        // 3. 사업자 관리 메뉴 클릭 시 BusinessManageActivity로 이동
        binding.layoutMenuBusiness.setOnClickListener {
            val intent = Intent(this, BusinessManageActivity::class.java)
            startActivity(intent)
        }

        // 4. 1:1 문의 관리 메뉴 클릭 시 InquiryManageActivity로 이동
        binding.layoutMenuInquiry.setOnClickListener {
            val intent = Intent(this, InquiryManageActivity::class.java)
            startActivity(intent)
        }
    }
}