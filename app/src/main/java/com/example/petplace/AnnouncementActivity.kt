package com.example.petplace

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petplace.databinding.ActivityAnnouncementBinding

class AnnouncementActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAnnouncementBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.hide()

        // 1. Intent 데이터 수신 (AnnouncementData는 Serializable 구현됨)
        val announcement = intent.getSerializableExtra("announcement", AnnouncementData::class.java)

        if (announcement != null) {
            initUI(announcement)
        } else {
            // 데이터가 없을 경우 Mock 데이터로 표시 (7-5 요구사항)
            showMockData()
        }

        binding.btnReturn.setOnClickListener {
            finish()
        }
    }

    private fun initUI(data: AnnouncementData) {
        binding.tvTitle.text = data.title
        binding.tvPoster.text = data.posterName
        binding.tvPostTime.text = data.postTime
        binding.tvContents.text = data.contents

        // 작성자 프로필 이미지
        Glide.with(this)
            .load(data.posterUrl)
            .placeholder(R.drawable.icon_pfp1)
            .into(binding.ivPoster)

        // 공지 상세 이미지
        if (data.fullImageUrl != null) {
            binding.ivFullImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(data.fullImageUrl)
                .into(binding.ivFullImage)
        } else {
            binding.ivFullImage.visibility = View.GONE
        }
    }

    private fun showMockData() {
        binding.tvTitle.text = "[MOCK] 3월 방문 감사 이벤트 안내"
        binding.tvPoster.text = "사장"
        binding.tvPostTime.text = "방금 전"
        binding.tvContents.text = "안녕하세요~! 항상 찾아주시는 손님 여러분들께 감사의 의미로 이벤트를 준비했습니다. 아래 사진을 참고해주세요 감사합니다^^ (이 데이터는 서버 통신 실패 시 표시되는 샘플입니다.)"
        
        binding.ivFullImage.visibility = View.VISIBLE
        binding.ivFullImage.setImageResource(R.drawable.test_announcement_full)
    }
    
    /* API 구현 시 활성화 예정 (주석 처리)
    private fun fetchAnnouncementDetail(id: Long) {
        // apiService.getAnnouncementDetail(id).enqueue(...)
    }
    */
}
