package com.example.petplace

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petplace.databinding.ActivityTempTestBinding

class TempTestActivity : AppCompatActivity() {

    // ViewBinding 객체 선언
    private lateinit var binding: ActivityTempTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 인플레이트 및 뷰 바인딩 초기화
        binding = ActivityTempTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 리뷰 읽기/쓰기 화면 이동
        binding.btnTestReviewRead.setOnClickListener {
            val intent = Intent(this, ReviewReadActivity::class.java)
            startActivity(intent)
        }

        // 사장님 모드(리뷰 관리) 화면 이동
        binding.btnTestOwner.setOnClickListener {
            val intent = Intent(this, OwnerReviewManageActivity::class.java).apply {
                putExtra("STORE_ID", 1)
            }
            startActivity(intent)
        }

        // 고객 모드(내 리뷰) 화면 이동
        binding.btnTestCustomer.setOnClickListener {
            val intent = Intent(this, MyReviewActivity::class.java).apply {
                putExtra("USER_ID", 1)
            }
            startActivity(intent)
        }

        // 관리자 마이페이지 이동
        binding.btnTestAdmin.setOnClickListener {
            val intent = Intent(this, AdminMyPageActivity::class.java)
            startActivity(intent)
        }

        // 임시 사업자 가입 신청 데이터 생성
        binding.btnTestAddBiz.setOnClickListener {
            val dbHelper = DatabaseHelper(this)
            val randomNum = (100..999).random()
            dbHelper.insertBusiness("123-45-$randomNum", "시흥시 정왕동 ${randomNum}번길", "애견 카페")
            showCustomDialog("가짜 사업자 데이터가 추가되었습니다.")
        }

        // 임시 1:1 문의 데이터 생성
        binding.btnTestAddInquiry.setOnClickListener {
            val dbHelper = DatabaseHelper(this)
            val categories = listOf("일반 문의", "오류 문의", "신고 문의")
            dbHelper.insertInquiry(categories.random(), "문의 사항 테스트 내용입니다.", "user_test@naver.com")
            showCustomDialog("문의 데이터가 추가되었습니다.")
        }

        // 업장 등록/관리 화면 이동
        binding.btnTestStoreManage.setOnClickListener {
            val intent = Intent(this, StoreManageActivity::class.java)
            startActivity(intent)
        }
    }
}